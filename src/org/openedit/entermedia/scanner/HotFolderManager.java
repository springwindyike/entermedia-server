package org.openedit.entermedia.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.PathEventModule;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.repository.filesystem.XmlVersionRepository;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebServer;
import com.openedit.page.manage.PageManager;
import com.openedit.util.EmStringUtils;

public class HotFolderManager
{
	private static final Log log = LogFactory.getLog(HotFolderManager.class);

	protected PageManager fieldPageManager;
	protected SearcherManager fieldSearcherManager;
	protected WebServer fieldWebServer;
	
	public WebServer getWebServer()
	{
		return fieldWebServer;
	}


	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}


	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}


	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}


	public PageManager getPageManager()
	{
		return fieldPageManager;
	}


	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public void saveMounts(String inCatalogId)
	{
		//remove any old hot folders for this catalog
		List configs = new ArrayList(getPageManager().getRepositoryManager().getRepositories());
		String path = "/WEB-INF/data/" + inCatalogId + "/originals";
		
		//remove all the mounts
		for (Iterator iterator = configs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if (config.getPath().startsWith(path)) 
			{
				getPageManager().getRepositoryManager().removeRepository(config.getPath());
			}
		}
		
		List<Repository> mounts = new ArrayList<Repository>();
		Collection folders = loadFolders(inCatalogId);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String folderpath = folder.get("subfolder");
			String fullpath = path + "/" + folderpath;
			Repository repo = null;
			String versioncontrol = folder.get("versioncontrol");
			if( Boolean.valueOf(versioncontrol) )
			{
				repo = new XmlVersionRepository();
				repo.setRepositoryType("versionRepository");
			}
			else
			{
				repo = new FileRepository();
			}
			//save data to repo
			repo.setPath(fullpath);
			repo.setExternalPath(folder.get("externalpath"));
			mounts.add(repo);
			//repo.setFilterIn(folder.get("includes"));
			//repo.setFilterOut(folder.get("excludes"));
		}
		
		if( mounts.size() > 0)
		{
			configs = getPageManager().getRepositoryManager().getRepositories();
			configs.addAll(mounts);
			getWebServer().saveMounts(configs);
		}
		//getPageManager().getRepositoryManager().setRepositories(configs);
		//save the file
	}


	public Collection loadFolders(String inCatalogId)
	{
		Searcher hfsearcher = getFolderSearcher(inCatalogId);

		return hfsearcher.getAllHits();
	}


	public Searcher getFolderSearcher(String inCatalogId)
	{
		return getSearcherManager().getSearcher(inCatalogId, "hotfolder");
	}

	public Data getFolderByPathEnding(String inCatalogId, String inFolder)
	{		
		for (Iterator iterator = loadFolders(inCatalogId).iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String subfolder = folder.get("subfolder");
			if(inFolder.equals(subfolder) )
			{
				return folder;
			}
			
		}
		return null;
	}


	public void deleteFolder(String inCatalogId, Data inExisting)
	{
		getFolderSearcher(inCatalogId).delete(inExisting, null);
		saveMounts(inCatalogId);
	}


	public void saveFolder(String inCatalogId, Data inNewrow)
	{
		getFolderSearcher(inCatalogId).saveData(inNewrow, null);		
		saveMounts(inCatalogId);
	}	

	public List<String> importHotFolder(MediaArchive inArchive, Data inFolder)
	{
		String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
		String name = inFolder.get("subfolder");
		String path = base + "/" + name;
		
		AssetImporter importer = (AssetImporter)getWebServer().getModuleManager().getBean("assetImporter");
		
		String excludes = inFolder.get("excludes");
		if( excludes != null )
		{
			List<String> list = EmStringUtils.split(excludes);
			for (int i = 0; i < list.size(); i++)
			{
				String row = list.get(i).trim();
				if( row.startsWith("/") &&  !row.startsWith(path))
				{
					row = path + row;
				}
				list.set(i, row);
			}
			importer.setExcludeMatches(list);
		}
		
		importer.setIncludeExtensions(inFolder.get("includes"));
		String attachments = inFolder.get("attachmenttrigger");
		if( attachments != null )
		{
			Collection attachmentslist = EmStringUtils.split(attachments);
			importer.setAttachmentFilters(attachmentslist);
		}
		
		Date started = new Date();
		long sincedate = 0;
		String since = inFolder.get("lastscanstart");
		log.info(inFolder + " filtered by date " + since );
		if( since != null )
		{
			sincedate = DateStorageUtil.getStorageUtil().parseFromStorage(since).getTime();
		}
		List<String> paths = importer.processOn(base, path, inArchive, sincedate, null);
		inFolder.setProperty("lastscanstart", DateStorageUtil.getStorageUtil().formatForStorage(started));
		getFolderSearcher(inArchive.getCatalogId()).saveData(inFolder, null);
		log.info(inFolder + " Imported " + paths.size() );
		
		return paths;
	}
	
}
