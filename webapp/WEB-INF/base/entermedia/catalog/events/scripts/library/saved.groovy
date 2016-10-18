package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data

public void init() {
	String id = context.getRequestParameter("id");

	Data library = context.getPageValue("data");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	if(library == null){
		if( id == null) {
			id = context.getRequestParameter("id.value");
		}
		if( id == null) {
			return;
		}
		library = mediaArchive.getSearcher("library").searchById(id);
	}

	if( library != null ) 
	{
		Category parentcategory = null;
		if( library.get("categoryid") == null)
		{
			String path = library.get("folder");
			if( path == null)
			{
				path = "Libraries/" + library.getName();
			}
			parentcategory = mediaArchive.createCategoryPath(path);
			library.setValue("categoryid", parentcategory.getId() );
			String username = context.getUserName();
			parentcategory.addValue("viewusers",username);
			mediaArchive.getCategorySearcher().saveData(parentcategory);
		
			String owner = library.get("owner");
			if(owner == null){
				library.setProperty("owner", username);
				//library.setProperty("ownerprofile",context.getUserProfile().getId()); 
				mediaArchive.getSearcher("library").saveData(library, null);
			}
			log.info("saving library $path");
		}	
	}
}

init();

