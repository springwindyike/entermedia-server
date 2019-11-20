package org.entermediadb.websocket.chat;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;

public class ChatManager implements CatalogEnabled
{
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public void updateChatTopicLastModified(String collectionid, String channelid)
	{
		MultiValued status = (MultiValued) getMediaArchive().query("chattopiclastmodified").exact("channelid", channelid).searchOne();
		if (status == null)
		{
			status = (MultiValued) getMediaArchive().getSearcher("chattopiclastmodified").createNewData();
			status.setValue("chattopicid", channelid);
			status.setValue("collectionid", collectionid);
		}
		status.setValue("datemodified", new Date());
		getMediaArchive().saveData("chattopiclastmodified", status);

	}

	//TODO: Do this while messages are coming in
	public void updateChatTopicLastChecked(String collectionid, String channelid, String inUserId)
	{
		MultiValued status = (MultiValued) getMediaArchive().query("chattopiclastchecked").exact("chattopicid", channelid).exact("userid", inUserId).searchOne();
		if (status == null)
		{
			status = (MultiValued) getMediaArchive().getSearcher("chattopiclastchecked").createNewData();
			status.setValue("chattopicid", channelid);
			status.setValue("userid", inUserId);
			status.setValue("collectionid", collectionid);
			
		}
		status.setValue("datechecked", new Date());
		getMediaArchive().saveData("chattopiclastchecked", status);

	}

	public Set loadChatTopicLastChecked(String inCollectionId, String inUserId)
	{
		//First get all the topics
		Collection alltopicsmodified = getMediaArchive().query("chattopiclastmodified").exact("collectionid", inCollectionId).search();

		HashMap<String, Data> modifiedtopics = new HashMap<String, Data>();
		for (Iterator iterator = alltopicsmodified.iterator(); iterator.hasNext();)
		{
			Data expiredcheck = (Data) iterator.next();
			modifiedtopics.put(expiredcheck.get("chattopicid"), expiredcheck);
		}

		Collection lastchecks = getMediaArchive().query("chattopiclastchecked").exact("userid", inUserId).exact("collectionid", inCollectionId).search();

		for (Iterator iterator = lastchecks.iterator(); iterator.hasNext();)
		{
			Data lastcheck = (Data) iterator.next();
			String lastchecktopicid = lastcheck.get("chattopicid");
			Data existingtopicmodified = modifiedtopics.get(lastchecktopicid);
			if (existingtopicmodified != null)
			{
				Date modifiedtopic = (Date) existingtopicmodified.getValue("datemodified");
				Date checked = (Date) lastcheck.getValue("datechecked");
				if (!modifiedtopic.after(checked))
				{
					String topicid = lastcheck.get("chattopicid");
					modifiedtopics.remove(topicid);
				}
			}
		}
		//What is left has not been viewed... TODO: Deal with empty topics? put welcome message.. Please enter chat
		return new HashSet(modifiedtopics.keySet());

	}

}
