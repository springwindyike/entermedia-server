package org.openedit.entermedia.scanner;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.page.manage.PageManager;

public class MetaDataReader
{
	private static final Log log = LogFactory.getLog(MetaDataReader.class);
	protected List fieldMetadataExtractors;

	public void populateAsset(MediaArchive inArchive, ContentItem inputFile, Asset inAsset)
	{
		try
		{
			//Make sure this is not a getStub so that S3 can cache it
			//make sure it is fully loaded
			if( inputFile.isStub() )
			{
				inputFile = getPageManager().getRepository().get(inputFile.getPath());
			}	

//			GregorianCalendar cal = new GregorianCalendar();
//			cal.setTimeInMillis(inputFile.lastModified());
//			cal.set(Calendar.MILLISECOND, 0);
			// Asset Modification Date">2005-03-04 08:28:57

			
			String now =  DateStorageUtil.getStorageUtil().formatForStorage(inputFile.lastModified());
			inAsset.setProperty("assetmodificationdate",now);
			// inAsset.setProperty("recordmodificationdate", format.format(
			// new Date() ) );
			inAsset.setProperty("filesize", String.valueOf(inputFile.getLength()));
			if (inAsset.getName() == null)
			{
				inAsset.setName(inputFile.getName());
			}
			long start = System.currentTimeMillis();
			boolean foundone = false;
			for (Iterator iterator = getMetadataExtractors().iterator(); iterator.hasNext();)
			{
				MetadataExtractor extrac = (MetadataExtractor) iterator.next();
				if( extrac.extractData(inArchive, inputFile, inAsset) )
				{
					foundone = true;
				}
			}
			if( foundone )
			{
				long end = System.currentTimeMillis();
				if( log.isDebugEnabled() )
				{
					log.debug("Got metadata in " + (end - start) + " mili seconds.");
				}
			}
		}
		catch (Exception e)
		{
			log.error("Could not read metadata", e);
		}
	}

	public List getMetadataExtractors()
	{
		if (fieldMetadataExtractors == null)
		{
			fieldMetadataExtractors = new ArrayList();
		}

		return fieldMetadataExtractors;
	}

	public void setMetadataExtractors(List inMetadataExtractors)
	{
		fieldMetadataExtractors = inMetadataExtractors;
	}
	
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	
}
