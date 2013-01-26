package org.openedit.entermedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.CompositeData;

import com.openedit.hittracker.HitTracker;

public class CompositeAsset extends Asset implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected MediaArchive fieldArchive;
	protected HitTracker fieldSelectedHits;
	protected String fieldId;
	protected List fieldRemovedCategories;
	protected List fieldRemovedKeywords;
	protected Map fieldPropertiesSet;
	
	public Map getPropertiesSet()
	{
		if (fieldPropertiesSet == null)
		{
			fieldPropertiesSet = new HashMap();
		}
		return fieldPropertiesSet;
	}

	public void setPropertiesSet(Map inPropertiesSet)
	{
		fieldPropertiesSet = inPropertiesSet;
	}

	public List getRemovedCategories()
	{
		if (fieldRemovedCategories == null)
		{
			fieldRemovedCategories = new ArrayList();
		}

		return fieldRemovedCategories;
	}

	public void setRemovedCategories(List inRemovedCategories)
	{
		fieldRemovedCategories = inRemovedCategories;
	}

	public List getRemovedKeywords()
	{
		if (fieldRemovedKeywords == null)
		{
			fieldRemovedKeywords = new ArrayList();
		}

		return fieldRemovedKeywords;
	}

	public void setRemovedKeywords(List inRemovedKeywords)
	{
		fieldRemovedKeywords = inRemovedKeywords;
	}

	
	public CompositeAsset(MediaArchive inMediaArchive, HitTracker inHits)
	{
		setArchive(inMediaArchive);
		setSelectedHits(inHits.getSelectedHitracker());
	}
	
	public int size()
	{
		return getSelectedHits().size();
	}
	public List getKeywords()
	{
		if( fieldKeywords == null )
		{
			Data first = (Data)getSelectedHits().first();
			String fwords = first.get("keywords");
			if( fwords != null && fwords.length() > 0)
			{
				String[] common = fwords.split("\\|");
				for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					String words = data.get("keywords");
					for (int i = 0; i < common.length; i++)
					{
						String k = common[i];
						if( k != null && !words.contains(k) )
						{
							common[i] = null;
						}
					}
				}
				fieldKeywords = new ArrayList();
				for (int i = 0; i < common.length; i++)
				{
					if( common[i] != null )
					{
						fieldKeywords.add(common[i].trim());
					}
				}
			}
			else
			{
				fieldKeywords = new ArrayList();
			}
		}
		return fieldKeywords;
	}


	protected void checkSave(List<Asset> inTosave)
	{
		if( inTosave.size() > 99 )
		{
			getArchive().saveAssets(inTosave);
			inTosave.clear();
		}
	}

	public void removeKeyword(String inKey)
	{
		super.removeKeyword(inKey);
		getRemovedKeywords().add(inKey);
	}

	public List getCategories()
	{
		if( fieldCategories == null )
		{
			Data first = (Data)getSelectedHits().first();
			if( first == null )
			{
				return Collections.EMPTY_LIST;
			}
			String fcats = first.get("category");
			if( fcats != null )
			{
				String[] catlist = fcats.split("\\|");
				for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					String cats = data.get("category");
					if( cats != null )
					{
						for (int i = 0; i < catlist.length; i++)
						{
							String  catid = catlist[i];
							if(catid != null && !cats.contains(catid) )
							{
								catlist[i] = null;
							}
						}
					}
				}
				ArrayList categories = new ArrayList();
				for (int i = 0; i < catlist.length; i++)
				{
					String  catid = catlist[i];
					if( catid != null )
					{
						Category cat = getArchive().getCategory(catid.trim());
						if( cat != null )
						{
							categories.add( cat );
						}
					}
				}
				Collections.sort(categories);
				fieldCategories  = categories;
			}
			else
			{
				fieldCategories = new ArrayList();
			}
		}
		return fieldCategories;
	}

	public void removeCategory(Category inCategory)
	{
		super.removeCategory(inCategory);
		getRemovedCategories().add(inCategory);
		
	}
	
	public String getProperty(String inKey) 
	{
		return get(inKey);
	}
	public String get(String inId)
	{	
		if (size() > 0)
		{
			String val = super.get(inId);
			if( val != null ) //already set to a value
			{
				if( val.length() == 0 )
				{
					return null; //set to empty
				}
				return val;
			}
//			if( val == null ) 
//			{
//				return null;
//			}
			//return something only if all the values match the first record
			val = ((Data)getSelectedHits().first()).get(inId);
			for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String newval = data.get(inId);
				if( val == null )
				{
					if( val != newval )
					{
						val = "";
						break;
					}
				}
				else if (!val.equals(newval))
				{
					val = "";
					break;
				}
			}
			if(	val == null )
			{
				val = "";
			}

			super.setProperty(inId, val);
			return val;
		}
		
		return null;
	}
	public void setProperty(String inKey, String inValue)
	{
		if( inValue == null )
		{
			inValue = "";
		}
		getProperties().put(inKey, inValue);
		getPropertiesSet().put(inKey,inValue);
	}

	public String getId()
	{
		return fieldId;
	}

	public String getName()
	{
		return "Multiple Data";
	}
	
	public void setName(String inName)
	{
		//Nothing to do here
	}

	public void setId(String inNewid)
	{
		fieldId = inNewid;
	}


	public String getSourcePath()
	{
		if( size() > 0)
		{
			Data first = (Data)getSelectedHits().first();
			return first.getSourcePath() + "multi" + size();
		}
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		
	}
	
	public Iterator iterator()
	{
		return new AssetIterator(getSelectedHits().iterator());
	}
	
	public HitTracker getSelectedHits()
	{
		return fieldSelectedHits;
	}

	public void setSelectedHits(HitTracker inSelectedHits)
	{
		fieldSelectedHits = inSelectedHits;
	}

	public MediaArchive getArchive()
	{
		return fieldArchive;
	}

	public void setArchive(MediaArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	public void saveChanges() 
	{
		//compare keywords, categories and data. 
		List tosave = new ArrayList(100);
		for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset fieldCurrentAsset = null;
			
			for (Iterator iterator2 = getCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				String cats = data.get("category");
				if( !cats.contains(cat.getId() ) )
				{
					Asset asset = loadAsset( fieldCurrentAsset, data, tosave);
					if( asset != null )
					{
						asset.addCategory(cat);
						tosave.add(asset);
						checkSave(tosave);
					}
				}
			}
			for (Iterator iterator2 = getRemovedCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				String cats = data.get("category");
				if( cats.contains(cat.getId() ) )
				{
					Asset asset = loadAsset( fieldCurrentAsset, data, tosave);
					if( asset != null )
					{
						asset.removeCategory(cat);
						tosave.add(asset);
						checkSave(tosave);
					}
				}
			}
			
			for (Iterator iterator2 = getKeywords().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				String existing = data.get("keywords");
				if( existing == null || !existing.contains(inKey) )
				{
					Asset asset = loadAsset( fieldCurrentAsset, data, tosave);
					if( asset != null )
					{
						asset.addKeyword(inKey);
					}
				}
			}
			
			for (Iterator iterator2 = getRemovedKeywords().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				inKey = inKey.trim();
				String existing = data.get("keywords");
				if( existing != null && existing.contains(inKey) )
				{
					Asset asset = loadAsset( fieldCurrentAsset, data, tosave);
					if( asset != null )
					{
						asset.removeKeyword(inKey);
					}
				}
			}
			for (Iterator iterator2 = getPropertiesSet().keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				String value = (String)getPropertiesSet().get(key);
				String existingvalue = data.get(key);
				
				if( existingvalue == value )
				{
					continue;
				}
				if( existingvalue != null && existingvalue.equals(value) )
				{
					continue;
				}
				Asset asset = loadAsset( fieldCurrentAsset, data, tosave);
				if( asset != null )
				{
					asset.setProperty(key, value);
				}
			}
			//TODO: Deal with multi value fields
			
		}
		getArchive().saveAssets(tosave);
	}
//
	class AssetIterator implements Iterator
	{
		Iterator fieldDataIterator;
		
		public AssetIterator(Iterator inHitsIterator)
		{
			fieldDataIterator = inHitsIterator;
		}
		
		public boolean hasNext()
		{
			return fieldDataIterator.hasNext();
		}

		@Override
		public Object next()
		{
			Data next = (Data)fieldDataIterator.next();
			
			return getArchive().getAssetBySourcePath(next.getSourcePath());
		}

		public void remove()
		{
		}
		
		
	}
	
	protected Asset loadAsset(Asset inFieldCurrentAsset, Data inData, List toSave)
	{
		if( inFieldCurrentAsset == null )
		{
			inFieldCurrentAsset =  getArchive().getAssetBySourcePath(inData.getSourcePath());
		}
		else
		{
			checkSave(toSave);
		}
		toSave.add(inFieldCurrentAsset);
		return inFieldCurrentAsset;
	}
	
	public String toString()
	{
		return getId();
	}
	
	public void refresh()
	{
		getPropertiesSet().clear();
		fieldCategories = null;
		fieldKeywords = null;
		
	}
	
}
