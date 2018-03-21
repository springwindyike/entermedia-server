package org.openedit.entermedia.scanner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;
import org.openedit.entermedia.creator.MediaCreator;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.page.Page;
import com.openedit.util.Exec;
import com.openedit.util.ExecResult;
import com.openedit.util.PathUtilities;

public class ExiftoolMetadataExtractor extends MetadataExtractor
{
	private static final String EMPTY_STRING = "";
	private static final Log log = LogFactory.getLog(ExiftoolMetadataExtractor.class);
	protected MediaCreator fieldExifToolThumbCreator;
	protected Exec fieldExec;
	protected Set fieldTextFields;

	public Set getTextFields()
	{
		if (fieldTextFields == null)
		{
			fieldTextFields = new HashSet();
			fieldTextFields.add("LensID");
			fieldTextFields.add("ShutterSpeed");
			fieldTextFields.add("FocalLength");

		}

		return fieldTextFields;
	}

	public void setTextFields(Set inTextFields)
	{
		fieldTextFields = inTextFields;
	}

	public MediaCreator getExifToolThumbCreator()
	{
		return fieldExifToolThumbCreator;
	}

	public void setExifToolThumbCreator(MediaCreator inExifToolThumbCreator)
	{
		fieldExifToolThumbCreator = inExifToolThumbCreator;
	}

	/**
	 * synchronized because ExifTool is not thread safe
	 */
	public synchronized boolean extractData(MediaArchive inArchive, ContentItem inputFile, Asset inAsset)
	{
		String[] supportedTypes = new String[] { "audio", "video", "image", "document" };
		String type = PathUtilities.extractPageType(inputFile.getName());

		if (type != null)
		{
			String mediatype = inArchive.getMediaRenderType(type);
			if (!Arrays.asList(supportedTypes).contains(mediatype))
			{
				return false;
			}
		}
		try
		{
			PropertyDetails details = inArchive.getAssetPropertyDetails();
			ArrayList<String> base = new ArrayList<String>();

			ContentItem etConfig = inArchive.getPageManager().getRepository().getStub(inArchive.getCatalogHome() + "/configuration/exiftool.conf");
			if (etConfig.exists())
			{
				base.add("-config");
				base.add(etConfig.getAbsolutePath());
			}

			base.add("-S");
			base.add("-d");
			base.add("\"%Y-%m-%d %H:%M:%S\""); //yyyy-MM-dd HH:mm:ss

			base.add(inputFile.getAbsolutePath());
			ArrayList<String> comm = new ArrayList(base);
			comm.add("-n");
			ExecResult result = getExec().runExec("exiftool", comm, true);
			if (!result.isRunOk())
			{
				String error = result.getStandardError();
				log.info("error " + error);
				return false;
			}
			String numberinfo = result.getStandardOut();
			if (numberinfo == null)
			{
				log.info("Exiftool found " + inAsset.getSourcePath() + " returned null");
			}
			else
			{
				log.debug("Exiftool found " + inAsset.getSourcePath() + " returned " + numberinfo.length());
			}

			boolean foundtext = parseNumericValues(inArchive, inAsset, details, numberinfo);
			if (foundtext)
			{
				//Run it again
				ExecResult resulttext = getExec().runExec("exiftool", base, true);
				if (!resulttext.isRunOk())
				{
					String error = resulttext.getStandardError();
					log.info("error " + error);
					return false;
				}
				String textinfo = resulttext.getStandardOut();
				parseTextValues(inAsset, details, textinfo);
			}
		}
		catch (Exception e1)
		{
			log.error("Could not read metada from asset: " + inAsset.getSourcePath() + e1, e1);
		}

		extractThumb(inArchive, inputFile, inAsset);

		return true;
	}

	protected boolean parseNumericValues(MediaArchive inArchive, Asset inAsset, PropertyDetails details, String numberinfo)
	{
		Pattern p = Pattern.compile("(\\w+):\\s+(.+)"); //clean whitespace TODO: handle lower/mixed case
		boolean foundtextvalues = false;
		if (numberinfo != null)
		{
			String[] numbers = numberinfo.split("\n");
			for (int i = 0; i < numbers.length; i++)
			{
				Matcher m = p.matcher(numbers[i]);
				if (!m.find())
				{
					continue;
				}
				String key = m.group(1);
				String value = m.group(2);

				if (key == null || value == null)
				{
					continue;
				}

				if ("ImageSize".equals(key))
				{
					try
					{
						String[] dims = value.split("x");
						String width = dims[0];
						String height = dims[1];
						//width & heights can have decimals if converted from vectors, e.g., SVGs
						if (width.contains("."))
						{//round off to the nearest integer
							Float fwidth = Float.parseFloat(width);
							width = String.valueOf(fwidth.intValue());
						}
						if (height.contains("."))
						{
							Float fheight = Float.parseFloat(height);
							height = String.valueOf(fheight.intValue());
						}
						inAsset.setProperty("width", width);
						inAsset.setProperty("height", height);
					}
					catch (Exception e)
					{
						log.warn("Could not parse ImageSize string: " + value);
					}
				}
				else if ("MaxPageSizeW".equals(key))
				{
					if (inAsset.get("width") == null)
					{
						float wide = Float.parseFloat(value);
						wide = wide * 72f;
						inAsset.setProperty("width", String.valueOf(Math.round(wide)));
					}
				}
				else if ("MaxPageSizeH".equals(key))
				{
					if (inAsset.get("height") == null)
					{
						float height = Float.parseFloat(value);
						height = height * 72f;
						inAsset.setProperty("height", String.valueOf(Math.round(height)));
					}
				}
				else if ("Duration".equals(key) || "SendDuration".equals(key))
				{
					try
					{
						inAsset.setProperty("duration", value);
						value = processDuration(value);
						inAsset.setProperty("length", value);
					}
					catch (Exception e)
					{
						log.warn("Could not parse file length: " + value);
					}
				}
				//				else if("Subject".equals(key))
				//				{
				//					String[] kwords = value.split(",");
				//					for( String kword : kwords )
				//					{
				//						inAsset.addKeyword(kword.trim());
				//					}
				//				}
				else if ("FileType".equals(key))
				{
					if (inAsset.getProperty("fileformat") == null)
					{
						inAsset.setProperty("fileformat", value.toLowerCase());
					}
				}
				else if ("Subject".equals(key) || "Keyword".equals(key) || "Keywords".equals(key))
				{
					String[] kwords = value.split(",");
					for (String kword : kwords)
					{
						inAsset.addKeyword(kword.trim());
					}
				}
				else if ("VideoFrameRate".equals(key))
				{
					inAsset.setProperty("framerate", roundFrameRate(value));
				}
				else if ("ColorSpace".equals(key))
				{
					if ("65535".equals(value))
					{
						//not valid
						continue;
					}
					inAsset.setProperty("colorspace", value);
				}
				else if ("GPSLatitude".equals(key))
				{
					inAsset.setProperty("position_lat", value);
				}
				else if ("GPSLongitude".equals(key))
				{
					inAsset.setProperty("position_lng", value);
				}

				else if (getTextFields().contains(key))
				{
					foundtextvalues = true;
				}
				else
				{
					PropertyDetail property = details.getDetailByExternalId(key);

					if (property == null)
					{
						continue;
					}
					else if (property.isDate())
					{
						//Date dateValue = externalFormat.parse(value);
						//value = value + " -0000"; //added offset of 0 since that seems to be the default
						//TODO: Should we clean up dates on their way in? Right now it uses a close format but not the perfect format
						value = DateStorageUtil.getStorageUtil().checkFormat(value);
						inAsset.setProperty(property.getId(), value);
					}
					else if (property.isList() )  //|| property.isDataType("number")
					{
						m = p.matcher(numbers[i]);
						if (m.find())
						{
							Searcher searcher = inArchive.getSearcherManager().getSearcher(property.getListCatalogId(), property.getListId());
							Data lookup = (Data) searcher.searchByField("name", value);
							if (lookup != null)
							{
								inAsset.setProperty(property.getId(), lookup.getId());
								continue;
							}
							else if(Boolean.parseBoolean(property.get("autocreatefromexif"))){
								lookup = searcher.createNewData();
								lookup.setName(value);
								//lookup.setId(searcher.nextId());
								searcher.saveData(lookup, null);
								inAsset.setProperty(property.getId(), lookup.getId());
							}
							else
							{
								inAsset.setProperty(property.getId(), value);
							}
						}
					}
					else
					{
						inAsset.setProperty(property.getId(), value);
					}
				}
			}
		}
		return foundtextvalues;
	}

	protected void parseTextValues(Asset inAsset, PropertyDetails details, String numberinfo)
	{
		Pattern p = Pattern.compile("(\\w+):\\s+(.+)"); //clean whitespace
		if (numberinfo != null)
		{
			String[] numbers = numberinfo.split("\n");
			for (int i = 0; i < numbers.length; i++)
			{
				Matcher m = p.matcher(numbers[i]);
				if (!m.find())
				{
					continue;
				}
				String key = m.group(1);
				String value = m.group(2);

				if (key == null || value == null || !getTextFields().contains(key))
				{
					continue;
				}
				PropertyDetail property = details.getDetailByExternalId(key);
				if (property == null)
				{
					continue;
				}
				inAsset.setProperty(property.getId(), value);
			}
		}
	}

	protected String processDuration(String value)
	{
		if (value.contains("s"))
		{
			value = value.split("\\.")[0];
		}
		else
		{
			String[] parts = value.split(":");
			double total = 0;
			for (int j = 0; j < parts.length; j++)
			{
				total += Math.pow(60, parts.length - 1 - j) * Double.parseDouble(parts[j]);
			}
			value = String.valueOf(Math.round(total));
		}
		return value;
	}

	protected void extractThumb(MediaArchive inArchive, ContentItem inInputFile, Asset inAsset)
	{
		if (getExifToolThumbCreator().canReadIn(inArchive, inAsset.getFileFormat()))
		{
			Page custom = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/customthumb.png");
	
			//if we have embdeded thumb 
			ConvertInstructions instructions = new ConvertInstructions();
			instructions.setForce(true);
			instructions.setInputPath(inInputFile.getPath());
			instructions.setOutputPath(custom.getPath());
			ConvertResult res = getExifToolThumbCreator().convert(inArchive, inAsset, custom, instructions);
			if (res.isOk())
			{
	//			if (!"generated".equals(inAsset.get("previewstatus")))
	//			{
	//				inAsset.setProperty("previewstatus", "exif");
	//			}
				return;
			}
		}
		String format = inAsset.getFileFormat();
		if( format == null)
		{
			return;
		}
		if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format) ||
				"tiff".equalsIgnoreCase(format) || "tif".equalsIgnoreCase(format) )// ||  "pdf".equalsIgnoreCase(format) )
		{	
			//OR if we have CMYK with no profile input
			String colorspace =  inAsset.get("colorspace");
			if( colorspace == null)
			{
				if( isCMYKColorSpace(inInputFile) )
				{
					colorspace = "4";
					inAsset.setProperty("colorspsace", colorspace);
				}
			}
			if( "4".equals( colorspace ) ||  "5".equals(colorspace ))
			{
				if( !isCMYKProfile(inInputFile) )
				{
					Page custom = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/customthumb.jpg");
	
			        MediaCreator c = inArchive.getCreatorManager().getMediaCreatorByOutputFormat("png");
					ConvertInstructions instructions = new ConvertInstructions();
					instructions.setForce(true);
					//instructions.setMaxScaledSize(1900, height);
					instructions.setProperty("fixcmyk", "true");
					instructions.setInputPath(inInputFile.getPath());
					instructions.setOutputPath(custom.getPath());
				 	c.convert(inArchive, inAsset, custom, instructions);
					
				}
			}
		}
	}
	protected boolean isCMYKColorSpace(ContentItem inOriginal)
	{
		List<String> command = new ArrayList<String>();
		command.add("-verbose");
		command.add(inOriginal.getAbsolutePath());
		ExecResult result = getExec().runExec("identify",command, true);
		String sout = result.getStandardOut();
		String[] tokens = sout.split("\n");
		if (tokens.length > 0){
			for(String token:tokens)
			{
				if (token != null && token.trim().startsWith("Colorspace:")){//Colorspace: CMYK
					boolean isCMYK = token.toLowerCase().contains("cmyk");
					return isCMYK;
				}
			}
		}
		return false;
	}
	protected boolean isCMYKProfile(ContentItem inOriginal)
	{
		List<String> command = new ArrayList<String>();
		
		command.add("-a");
		command.add("-S");
		command.add("-G0");
		command.add("-ICC_Profile:ColorSpaceData");
		command.add(inOriginal.getAbsolutePath());
		ExecResult result = getExec().runExec("exiftool",command, true);
		String sout = result.getStandardOut();
		if( sout.toLowerCase().contains("cmyk"))
		{
			return true;
		}
		return false;
	}
	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec exec)
	{
		fieldExec = exec;
	}

	protected String roundFrameRate(String val)
	{
		if (val == null || EMPTY_STRING.equals(val.trim()))
			return EMPTY_STRING;
		BigDecimal result = new BigDecimal(val);
		result = result.setScale(2, RoundingMode.HALF_UP);
		return result.toString();
	}

}