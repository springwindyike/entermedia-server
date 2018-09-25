package org.entermediadb.asset.pull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.node.NodeManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.OutputFiller;
import org.openedit.util.URLUtilities;

public class PullManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(PullManager.class);
	protected SearcherManager fieldSearcherManager;
	protected NodeManager fieldNodeManager;
	protected String fieldCatalogId;
	OutputFiller filler = new OutputFiller();
	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


	public NodeManager getNodeManager()
	{
		return fieldNodeManager;
	}


	public void setNodeManager(NodeManager inNodeManager)
	{
		fieldNodeManager = inNodeManager;
	}


	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}


	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	public HitTracker listRecentChanges(String inType, String inLastpulldate)
	{
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
			QueryBuilder builder = searcher.query().exact("importstatus","complete").exact("mastereditclusterid", getNodeManager().getLocalClusterId());
			if( inLastpulldate != null)
			{
				Date startingfrom = DateStorageUtil.getStorageUtil().parseFromStorage(inLastpulldate);
				builder.after("recordmodificationdate", startingfrom);
			}
			HitTracker hits = builder.search();
			if( !hits.isEmpty() )
			{
				hits.enableBulkOperations();
				log.info("Found changes " + hits.size());
			}
			return hits;

	}


	public long processPullQueue(MediaArchive inArchive)
	{
		//Connect to all the nodes
		//Run a search based on las time I pulled it down
		long totalcount = 0;
		try
		{
			Collection nodes = getNodeManager().getRemoteEditClusters(inArchive.getCatalogId());
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				Data node = (Data) iterator.next();
				String url = node.get("baseurl");
				if( url != null)
				{
					long time = System.currentTimeMillis();
					time = time  - 10000L; //Buffer
					Date now = new Date(time);
					HttpRequestBuilder connection = new HttpRequestBuilder();
					Map params = new HashMap();
					if( node.get("entermediakey") != null)
					{
						params.put("entermedia.key", node.get("entermediakey"));
					}
					else
					{
						log.error("entermediakey is required");
						continue;
					}
					if( node.get("lastpulldate") != null)
					{
						Object dateob = node.getValue("lastpulldate");
						Date pulldate = null;
						if(dateob instanceof String){
							pulldate = DateStorageUtil.getStorageUtil().parseFromStorage((String) dateob);
						} else{
							 pulldate = (Date)node.getValue("lastpulldate");	
						}
						
						if( pulldate.after(now))
						{
							log.info("We just ran a pull within last 10 seconds");
							continue;
						}
						params.put("lastpulldate", node.get("lastpulldate")); //Tostring
					}
					params.put("searchtype", "asset"); //Loop over all of the types
					if( inArchive.getAssetSearcher().getAllHits().isEmpty() )
					{
						log.info("Doing a full download");
						params.put("fulldownload", "true");
					}
					
					long ok = downloadPages(inArchive, connection, node, params);
					if( ok != -1 )
					{
						node.setValue("lastpulldate", now);
						getSearcherManager().getSearcher(inArchive.getCatalogId(),"editingcluster").saveData(node);
					}	
					else
					{
						//error downloading
					}
					totalcount = totalcount + ok;
				}
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}
		return totalcount;
	}

	protected long downloadPages(MediaArchive inArchive, HttpRequestBuilder connection, Data node, Map params) throws Exception
	{
		String baseurl = node.get("baseurl");
		//add origiginal support
		String url = baseurl + "/mediadb/services/cluster/listchanges.json";
		StringBuffer link = new StringBuffer();
		link.append("?");
		link.append("entermedia.key=");
		link.append(params.get("entermedia.key"));
		link.append("&lastpulldate=");
		if( params.get("lastpulldate") != null)
		{
			link.append(params.get("lastpulldate"));
		}
		link.append("&searchtype=");
		link.append(params.get("searchtype"));
		log.info("Checking: " + url + link);
		HttpResponse response2 = connection.sharedPost(url , params);
		StatusLine sl = response2.getStatusLine();           
		if (sl.getStatusCode() != 200)
		{
			node.setProperty("lasterrormessage", "Could not download " + sl.getStatusCode() + " " + sl.getReasonPhrase());
			node.setValue("lasterrordate",new Date()); 
			getSearcherManager().getSearcher(inArchive.getCatalogId(),"editingcluster").saveData(node);
			log.error("Initial data server error " + sl);
			return -1;
		}
		String returned = EntityUtils.toString(response2.getEntity());
		//log.info("returned:" + returned);
		Map parsed = (Map)new JSONParser().parse(returned);
		boolean skipgenerated = (boolean) Boolean.parseBoolean(node.get("skipgenerated"));
		boolean skiporiginal = (boolean) Boolean.parseBoolean(node.get("skiporiginal"));

		long assetcount = 0;
		int page = 1;
		Map response = (Map)parsed.get("response");
		String ok = (String)response.get("status");
		if( ok != null && ok.equals("ok") )
		{
			Collection saved = importChanges(inArchive, returned, parsed);
			assetcount = assetcount + saved.size();
			if(!skipgenerated) {

			downloadGeneratedFiles(inArchive,connection,node,params,parsed);
			}
			if(!skiporiginal) {
				//TODO:   Need the asset
			}
			
			int pages = Integer.parseInt( response.get("pages").toString() );
			//loop over pages
			String hitssessionid = (String)response.get("hitssessionid");
			params.put("hitssessionid", hitssessionid);
			for (int count = 2; count <= pages; count++)
			{
				url = baseurl + "/mediadb/services/cluster/nextpage.json";
		
				params.put("page",String.valueOf( count));
				
				log.info("next page: " + url + link + "&page=" + count + "&hitssessionid=" + hitssessionid);
				response2 = connection.sharedPost(url , params);
				sl = response2.getStatusLine();           
				if (sl.getStatusCode() != 200)
				{
					node.setProperty("lasterrormessage", sl.getStatusCode() + " " + sl.getReasonPhrase());
					node.setValue("lasterrordate",new Date()); 

					getSearcherManager().getSearcher(inArchive.getCatalogId(),"editingcluster").saveData(node);
					log.error("Page server error " + sl);
					return -1;
				}
				returned = EntityUtils.toString(response2.getEntity());
				//log.info("Got page of json: " + returned);
				parsed = (Map)new JSONParser().parse(returned);
				response = (Map)parsed.get("response");
				ok = (String)response.get("status");
				if( ok != null && !ok.equals("ok") )
				{
					log.error("Page could not be loaded " + returned);
					return -1;
				}
				log.info("Downloading page " + count + " of " + pages + " pages. assets count:" + assetcount);
				saved = importChanges(inArchive, returned, parsed);
				assetcount = assetcount + saved.size();
				if(!skipgenerated) {
					downloadGeneratedFiles(inArchive,connection,node,params,parsed);
				}
				if(!skiporiginal) {
					downloadGeneratedFiles(inArchive,connection,node,params,parsed);
				}

			}
			return assetcount;
		}
		else if ( ok != null && ok.equals("empty") )
		{
			//No changes found
			return 0;
		}
		else 
		{
			log.error("Initial data could not be loaded " + returned);
			return -1;
		}
	}


	protected void downloadGeneratedFiles(MediaArchive inArchive, HttpRequestBuilder inConnection, Data node, Map inParams, Map parsed)
	{
		String url = node.get("baseurl");
		try
		{
			//How do I get the sourcepath list?
			Collection changes = (Collection)parsed.get("generated");
			if(changes == null || changes.isEmpty())
			{
				return;
			}
			for (Iterator iterator2 = changes.iterator(); iterator2.hasNext();)
			{
				Map changed = (Map) iterator2.next();
				String sourcepath = (String)changed.get("sourcepath");
				//List generated media and compare it
				
				Collection files = (Collection)changed.get("files");
				if( files != null)
				{
					if( files.isEmpty() )
					{
						log.debug("No thumbs :" + sourcepath + " on " + parsed.toString());
						return;
					}
					for (Iterator iterator3 = files.iterator(); iterator3.hasNext();)
					{
						Map filelisting = (Map) iterator3.next();
						//Compare timestamps
						String filename = (String)filelisting.get("filename");
						String lastmodified = (String)filelisting.get("lastmodified");
						long datetime = Long.parseLong(lastmodified);
						//TODO:remove any milliseconds? Does this match rsync?
						
						ContentItem found = inArchive.getContent( "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + sourcepath + "/" + filename);
						if( !found.exists() || found.getLastModified() != datetime)
						{
							//http://em9dev.entermediadb.org/openinstitute/mediadb/services/module/asset/downloads/preset/Collections/Cincinnati%20-%20Flying%20Pigs/Flying%20Pig%20Marathon/Business%20Pig.jpg/image1024x768.jpg?cache=false
							//String fullURL = url + "/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename;
							String path = url + URLUtilities.encode("/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename );
							HttpResponse genfile = inConnection.sharedPost(path, inParams);
							StatusLine filestatus = genfile.getStatusLine();           
							if (filestatus.getStatusCode() != 200)
							{
								log.error("Could not download generated " + filestatus + " " + path);
								continue;
							}
							
								//Save to local file
								log.info("Saving :" + sourcepath + "/" + filename + " URL:" + path);
								InputStream stream = genfile.getEntity().getContent();
//								InputStreamItem item  = new InputStreamItem();
//								item.setAbsolutePath(found.getAbsolutePath());
//								item.setInputStream(genfile.getEntity().getContent());
//								inArchive.getPageManager().getRepository().put(item);
								//Change the timestamp to match
								File tosave = new File(found.getAbsolutePath());
								tosave.getParentFile().mkdirs();
								FileOutputStream fos = new FileOutputStream(tosave);
								filler.fill(stream, fos);
								filler.close(stream);
								filler.close(fos);
								tosave.setLastModified(datetime);
						}	
					}
				}
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not download files " + url , ex);
		}
	}

	protected Collection importChanges(MediaArchive inArchive, String returned, Map parsed)
	{
		//I dont want to edit the json in any way, so using original
		Collection array;
		try
		{
			//array = new JsonUtil().parseArray("results", returned);
			JSONParser parser = new JSONParser();
			JSONObject everything = (JSONObject) parser.parse(returned);
			
			JSONArray jsonarray = (JSONArray) everything.get("results");
			
			inArchive.getAssetSearcher().saveJson(jsonarray);
			log.info("saved " + jsonarray.size() + " changed asset ");
			return jsonarray;
		}
		catch (Exception e)
		{
			log.info("Error parsing following content: " + returned);
			throw new OpenEditException(e);
		}
		
		
	}

	/**
	 * Should this be in realtime? Maybe we should have as database journal to track local edits and push them out slowly...yes!
	 * @param inType
	 * @param inAssetIds
	 */
	public void pushLocalChangesToMaster(String inType, Collection<String> inAssetIds)
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
		Collection nodes = getNodeManager().getRemoteEditClusters(getCatalogId());
		HttpRequestBuilder builder = new HttpRequestBuilder();
		try
		{
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				Data node = (Data) iterator.next();
				HitTracker hits = searcher.query().ids(inAssetIds).exact("mastereditclusterid", node.getId()).search();
				if( !hits.isEmpty())
				{
					String url = node.get("baseurl");
					if( url != null)
					{
						Map params = new HashMap();
						if( node.get("entermediadkey") != null)
						{
							params.put("entermediadkey", node.get("entermediadkey"));
						}
						if( node.get("lastpulldate") != null)
						{
							params.put("lastpulldate", node.get("lastpulldate"));
						}
						//TODO: Add the json data
						StringBuffer jsonbody = new StringBuffer();
						jsonbody.append("[");
						for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
						{
							SearchHitData data = (SearchHitData) iterator2.next();
							jsonbody.append(data.toJsonString());
							if( iterator2.hasNext() )
							{
								jsonbody.append(",");
							}
						}
						jsonbody.append("]");
						params.put("changes", jsonbody.toString());
						
						HttpResponse response2 = builder.post(url + "/mediadb/services/cluster/savechanges.json", params);
						StatusLine sl = response2.getStatusLine();           
						if (sl.getStatusCode() != 200)
						{
							node.setProperty("lasterrormessage", "Could not push changes " + sl.getStatusCode() + " " + sl.getReasonPhrase());
							getSearcherManager().getSearcher(getCatalogId(),"editingcluster").saveData(node);
							log.error("Could not save changes to remote server " + url + " " + sl.getStatusCode() + " " + sl.getReasonPhrase());
							continue;
						}
					}
				}
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}
	}


	public ContentItem downloadOriginal(MediaArchive inArchive, Asset inAsset, File inFile, boolean ifneeded)
	{
		
		Data node = (Data) inArchive.getSearcher("editingcluster").searchByField("clustername", inAsset.get("mastereditclusterid"));
		String url = node.get("baseurl");
		
		
		FileItem item = new FileItem(inFile);

		String path = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?

		String primaryname = inAsset.getPrimaryFile();
		if (primaryname != null && inAsset.isFolder())
		{
			path = path + "/" + primaryname;
		}
		item.setPath(path);
		if (ifneeded)
		{
			//Check it exists and it matches
			long size = inAsset.getLong("filesize");
			if (item.getLength() != size)
			{
				String finalurl = url + "/mediadb/services/module/asset/downloads/originals/" +  URLUtilities.encode(inArchive.asLinkToOriginal(inAsset));
				HttpRequestBuilder connection = new HttpRequestBuilder();
				Map params = new HashMap();
				if( node.get("entermediakey") != null)
				{
					params.put("entermedia.key", node.get("entermediakey"));
				}
				
				HttpResponse genfile = connection.sharedPost(finalurl, params);
				StatusLine filestatus = genfile.getStatusLine();           
				if (filestatus.getStatusCode() != 200)
				{
					log.error("Could not download generated " + filestatus + " " + path + "Full URL: " + finalurl);
					return null;
				}
				
					//Save to local file
					try
					{
						log.info("Saving :" + inAsset.getSourcePath() + "/" + inAsset.getName() + " URL:" + path);
						InputStream stream = genfile.getEntity().getContent();

						inFile.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(inFile);
						filler.fill(stream, fos);
						filler.close(stream);
						filler.close(fos);
						//inFile.setLastModified(datetime);
					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
					throw new OpenEditException(e);
					}
				
				
				
				
				
			}
		}
		
		return item;
		

		
	}


	public InputStream getOriginalDocumentStream(MediaArchive inArchive, Asset inAsset)
	{
		try
		{
			Data node = (Data) inArchive.getSearcher("editingcluser").searchByField("clustername", inAsset.get("mastednodeid"));
			String url = node.get("baseurl");
			String finalurl = url + URLUtilities.encode(inArchive.asLinkToOriginal(inAsset));
			HttpRequestBuilder connection = new HttpRequestBuilder();
			Map params = new HashMap();
			if( node.get("entermediakey") != null)
			{
				params.put("entermedia.key", node.get("entermediakey"));
			}
			
			HttpResponse genfile = connection.sharedPost(finalurl, params);
			return genfile.getEntity().getContent();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
		throw new OpenEditException(e);
		}

		
	}

	protected File getFile(Asset inAsset)
	{
		String path = "/WEB-INF/data" + inAsset.getMediaArchive().getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?

		String primaryname = inAsset.getPrimaryFile();
		if (primaryname != null && inAsset.isFolder())
		{
			path = path + "/" + primaryname;
		}
		return new File(inAsset.getMediaArchive().getPageManager().getPage(path).getContentItem().getAbsolutePath());
		
		
	}
	


}