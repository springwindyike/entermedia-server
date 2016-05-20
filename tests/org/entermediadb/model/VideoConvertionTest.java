package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.events.PathEventManager;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;

public class VideoConvertionTest extends BaseEnterMediaTest
{
	public void testMpegToFlv()
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();
		
		ConvertInstructions instructions = new ConvertInstructions(archive);
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/101");
		instructions.setOutputExtension("flv");

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	public void testAviToFlv()
	{

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();

		ConvertInstructions instructions = new ConvertInstructions(archive);
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/103");
		instructions.setOutputExtension("flv");

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	public void testWmvToFlv()
	{

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();
		
		ConvertInstructions instructions = new ConvertInstructions(archive);
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/102");
		instructions.setOutputExtension("flv");

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	
	public void testMpegToJpeg()
	{
		

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();

		ConvertInstructions instructions1 = new ConvertInstructions(archive);
		instructions1.setForce(true);
		instructions1.setAssetSourcePath("users/admin/101");
		instructions1.setOutputExtension("mp4");
		
		Page converted = manager.createOutput(instructions1);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("mp4", PathUtilities.extractPageType(converted.getPath()));

		
		ConvertInstructions instructions = new ConvertInstructions(archive);
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/101");
		instructions.setOutputExtension("jpg");


		Page converted2 = manager.createOutput(instructions);
		assertNotNull(converted2);
		assertTrue(converted2.exists());
		assertTrue(converted2.length() > 0);
		assertEquals("jpg", PathUtilities.extractPageType(converted2.getPath()));
	}

	/* We dont do this any more
	public void testCreateAllFlash() throws Exception
	{
		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		//TODO: Check for flash video before and after running this event
		Page flash = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/video.flv");
		getFixture().getPageManager().removePage(flash);
		assertTrue(!flash.exists());
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/makeflashpreviews.html", req);
		req.getPageValue("mediaArchive");
		flash = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/video.mp4");
		assertTrue(flash.exists());
	}
	*/

	public void xtestCreateAllWatermarks() throws Exception
	{
		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		Page watermark = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/image150x150wm.jpg");
		getFixture().getPageManager().removePage(watermark);
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/makewatermarks.html", req);
		req.getPageValue("mediaArchive");
		watermark = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/image150x150wm.jpg");
		assertTrue(watermark.exists());

	}

	/** We dont do this any more
	public void testPublishOneLocation() throws Exception
	{

		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		Page watermark = getPage("/entermedia/catalogs/testcatalog/publishing/akamia/" + 101 + ".flv");
		getFixture().getPageManager().removePage(watermark);
		req.setRequestParameter("sourcepath", "users/admin/101/");
		req.setRequestParameter("locationid", "akamia");
		req.setRequestParameter("forced", "true");
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/publishasset.html", req);
		watermark = getPage("/entermedia/catalogs/testcatalog/publishing/akamia/" + 101 + ".flv");
		assertTrue(watermark.exists());
	}
	*/
}
