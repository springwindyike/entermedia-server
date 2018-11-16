package org.entermediadb.posts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class PostModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(PostModule.class);
	
	public void loadPost(WebPageRequest inReq)
	{
		String sourcepath = inReq.getContentProperty("postsourcepath");
		if(sourcepath == null)
		{
			sourcepath = loadFromAppHome(inReq);
		}
		
		//getSearcherManager().getCacheManager()
		String catalogid = inReq.findValue("catalogid");
		//TODO: Cache this
		//TODO: Fallback? Use the actual page
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "postdata");
		PostData post = (PostData) searcher.query().exact("sourcepath", sourcepath).searchOne();
		if (post == null)
		{
			if (sourcepath.endsWith("index.html"))
			{
				sourcepath = sourcepath.substring(0, sourcepath.length() - 10);
			}
		}
		
		post = (PostData) searcher.query().exact("sourcepath", sourcepath).searchOne();

		if (post == null && inReq.getUser() != null)
		{
			//TODO: Check they are in post edit mode
			String mode = inReq.getUser().get("oe_edit_mode");
			if("postedit".equals(mode))
			{
				post = (PostData) searcher.createNewData();
				//post.setValue("siteid", siteid);
				post.setValue("sourcepath", sourcepath);
				post.setValue("maincontent", "");
				searcher.saveData(post);
			}
		}
		else
		{
			//TODO: send 404
		}
		//TODO: Set mod time
		inReq.putPageValue("postdata", post);
		//Load up a $sitehome and $postdata 
	}

	protected String loadFromAppHome(WebPageRequest inReq)
	{
		String path = inReq.getPath();

		String sitehome = (String) inReq.getPageValue("sitehome");
		String apphome = (String)inReq.getPageValue("apphome");
		if( apphome == null)
		{
			log.info("Loading post from base? no apphome set");
			return null;
		}
		String 	sourcepath = null;
		if( apphome.equals(sitehome))
		{
			sourcepath = path.substring(apphome.length() + 1);
		}
		else if( apphome.startsWith(sitehome))
		{
			String apphomeending = apphome.substring(sitehome.length() + 1,apphome.length());
	
			int loc = path.indexOf(apphomeending);
			if( loc == -1)
			{
				log.info("apphome and sitehome are not compatible: sitehome" + sitehome + " apphome:" + apphome);
				return null;			
			}
			sourcepath = path.substring(loc,path.length());
		}
		else
		{
			log.info("apphome and sitehome are not compatible: sitehome" + sitehome + " apphome:" + apphome);
			return null;			
		}
		return sourcepath;
	}
}