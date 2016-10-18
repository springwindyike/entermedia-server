package assets

import java.util.Map;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.TimeParser
import org.junit.Before;
import org.openedit.Data
import org.openedit.page.Page
import org.openedit.page.manage.PageManager
import org.openedit.users.User;


MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
PageManager pageManager = mediaarchive.getPageManager();

public void archiveAssets(Data retentionpolicy, Collection assets)
{
	assets.each
	{
		if( !it.archivesourcepath  )
		{
			Asset asset = mediaarchive.getAssetSearcher().loadData(it);
			Page fullpath = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + asset.getSourcePath() );
			if(fullpath.exists()){
				String mask = retentionpolicy.get("archivepath");
				String newsourcepath = mediaarchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask( mediaarchive, null, asset.getName(), mask, asset.getProperties());
				
				Page newpage = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + newsourcepath);
				pageManager.movePage(fullpath,newpage);
				log.info("Archived asset to ${newpage.getContentItem().getAbsolutePath()}");
				asset.setFolder(fullpath.isFolder());
				asset.setValue("archivesourcepath",newsourcepath);
				mediaarchive.getAssetSearcher().saveData(asset);
			}
			else
			{
				log.info("Original did not exist to archive: ${asset.getSourcePath()}");
			}
		}
		
	}

	
}
public void checkRules()
{
	Collection policies = mediaarchive.getList("retentionrules");
	log.info("Checking assets rules " + policies.size() );
	policies.each 
	{
		Data retentionpolicy = it;
		TimeParser parser = new TimeParser();
		long daystokeep = parser.parse(it.expirationperiod);
		Date target = new Date(System.currentTimeMillis() -  daystokeep);
		 
		Collection assets = mediaarchive.getAssetSearcher().query().exact("retentionpolicy",it.id).before("assetaddeddate", target).search();
		log.info("Found ${assets.size()} for retention policy ${it} ${assets.query}");
		archiveAssets(retentionpolicy, assets);
	}
}	

String assetids = context.getRequestParameter("assetids");
if( assetids == null)
{
	checkRules();
}
else
{
	Collection hits = mediaarchive.getAssetSearcher().query().orgroup("id",assetids).search();
	List assets = new ArrayList();
	hits.each
	{
		Asset asset = mediaarchive.getAssetSearcher().loadData(it);
		String policy = asset.get("retentionpolicy");
		if( policy != null)
		{
			Data retentionpolicy = mediaarchive.getData("retentionrules",policy);
			TimeParser parser = new TimeParser();
			long daystokeep = parser.parse(retentionpolicy.expirationperiod);
			Date cutoff = new Date(System.currentTimeMillis() -  daystokeep );
			Date uploaddate = asset.getDate("assetaddeddate");
			if( daystokeep == 0 || uploaddate.before(cutoff) )
			{
				assets.add(asset);	
				archiveAssets(retentionpolicy, assets);
				assets.clear();
			}
		}
	}
}

