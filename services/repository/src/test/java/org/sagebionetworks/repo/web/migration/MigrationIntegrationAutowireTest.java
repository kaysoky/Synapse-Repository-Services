package org.sagebionetworks.repo.web.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.EntityServletTestHelper;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is an integration test to migration of all tables from start to finish.
 * 
 * The test does the following:
 * 1. the before() method creates at least one object for every type object that must migrate.
 * 2. Create a backup copy of all data.
 * 3. Delete all data in the system.
 * 4. Restore all data from the backup.
 * 
 * NOTE: Whenever a new migration type is added this test must be extended to test that objects migration.
 * 
 * 
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationIntegrationAutowireTest {
	
	public static final long MAX_WAIT_MS = 10*1000; // 10 sec.
	
	@Autowired
	EntityServletTestHelper entityServletHelper;
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	private String userName;
	private String adminId;
	
	// To delete
	List<String> entityToDelete;
	List<WikiPageKey> wikiToDelete;
	List<String> fileHandlesToDelete;
	
	// Entites
	Project project;
	FileEntity fileEntity;
	// Wiki pages
	WikiPage rootWiki;
	WikiPage subWiki;
	WikiPageKey rootWikiKey;
	WikiPageKey subWikiKey;

	// File Handles
	S3FileHandle handleOne;
	PreviewFileHandle preview;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		userName = TestUserDAO.ADMIN_USER_NAME;
		adminId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		createFileHandles();
		createEntities();
		creatWikiPages();
	}


	public void creatWikiPages() throws ServletException, IOException,
			JSONObjectAdapterException {
		wikiToDelete = new LinkedList<WikiPageKey>();
		// Create a wiki page
		rootWiki = new WikiPage();
		rootWiki.setAttachmentFileHandleIds(new LinkedList<String>());
		rootWiki.getAttachmentFileHandleIds().add(handleOne.getId());
		rootWiki.setTitle("Root title");
		rootWiki.setMarkdown("Root markdown");
		rootWiki = entityServletHelper.createWikiPage(userName, fileEntity.getId(), ObjectType.ENTITY, rootWiki);
		rootWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, rootWiki.getId());
		wikiToDelete.add(rootWikiKey);
		
		subWiki = new WikiPage();
		subWiki.setParentWikiId(rootWiki.getId());
		subWiki.setTitle("Sub-wiki-title");
		subWiki.setMarkdown("sub-wiki markdown");
		subWiki = entityServletHelper.createWikiPage(userName, fileEntity.getId(), ObjectType.ENTITY, subWiki);
		subWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, subWiki.getId());
	}


	/**
	 * Create the entities used by this test.
	 * @throws JSONObjectAdapterException
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void createEntities() throws JSONObjectAdapterException,
			ServletException, IOException, NotFoundException {
		entityToDelete = new LinkedList<String>();
		// Create a project
		project = new Project();
		project.setName("MigrationIntegrationAutowireTest.Project");
		project.setEntityType(Project.class.getName());
		project = (Project) entityServletHelper.createEntity(project, userName, null);
		entityToDelete.add(project.getId());
		
		// Create a file entity
		fileEntity = new FileEntity();
		fileEntity.setName("MigrationIntegrationAutowireTest.FileEntity");
		fileEntity.setEntityType(FileEntity.class.getName());
		fileEntity.setParentId(project.getId());
		fileEntity.setDataFileHandleId(handleOne.getId());
		fileEntity = (FileEntity) entityServletHelper.createEntity(fileEntity, userName, null);
	}


	/**
	 * Create the file handles used by this test.
	 * @throws NotFoundException
	 */
	public void createFileHandles() throws NotFoundException {
		fileHandlesToDelete = new LinkedList<String>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create a preview
		preview = new PreviewFileHandle();
		preview.setCreatedBy(adminId);
		preview.setCreatedOn(new Date());
		preview.setBucketName("bucket");
		preview.setKey("previewFileKey");
		preview.setEtag("etag");
		preview.setFileName("bar.txt");
		preview = fileMetadataDao.createFile(preview);
		fileHandlesToDelete.add(preview.getId());
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), preview.getId());
		fileHandlesToDelete.add(handleOne.getId());
	}
	
	
	@After
	public void after() throws Exception{
		if(wikiToDelete != null){
			for(WikiPageKey key: wikiToDelete){
				entityServletHelper.deleteWikiPage(key, userName);
			}
		}
		// Delete the project
		if(entityToDelete != null){
			for(String id: entityToDelete){
				entityServletHelper.deleteEntity(id, userName);
			}
		}
		if(fileHandlesToDelete != null){
			for(String id: fileHandlesToDelete){
				fileMetadataDao.delete(id);
			}
		}
	}
	
	
	@Test
	public void testRoundTrip() throws Exception{
		// Get the list of primary types
		MigrationTypeList primaryTypesList = entityServletHelper.getPrimaryMigrationTypes(userName);
		assertNotNull(primaryTypesList);
		assertNotNull(primaryTypesList.getList());
		assertTrue(primaryTypesList.getList().size() > 0);
		// Get the counts before we start
		MigrationTypeCounts startCounts = entityServletHelper.getMigrationTypeCounts(userName);
		validateStartingCount(startCounts);
		
		// This test will backup all data, delete it, then restore it.
		Map<MigrationType, String> map = new HashMap<MigrationType, String>();
		for(MigrationType type: primaryTypesList.getList()){
			// Backup each type
			BackupRestoreStatus status = backupAllOfType(type);
			if(status != null){
				assertNotNull(status.getBackupUrl());
				String fileName = getFileNameFromUrl(status.getBackupUrl());
				map.put(type, fileName);
			}
		}
		// We will delete the data when all object are ready
		
		// Now delete all data in reverse order
		for(int i=primaryTypesList.getList().size()-1; i >= 1; i--){
			MigrationType type = primaryTypesList.getList().get(i);
			deleteAllOfType(type);
		}
		// after deleting, the counts should be null
		MigrationTypeCounts afterDeleteCounts = entityServletHelper.getMigrationTypeCounts(userName);
		assertNotNull(afterDeleteCounts);
		assertNotNull(afterDeleteCounts.getList());
		for(int i=1; i<afterDeleteCounts.getList().size(); i++){
			assertEquals(new Long(0), afterDeleteCounts.getList().get(i).getCount());
		}
		
		// Now restore all of the data
		for(MigrationType type: primaryTypesList.getList()){
			String fileName = map.get(type);
			assertNotNull("Did not find a backup file name for type: "+type, fileName);
			restoreFromBackup(type, fileName);
		}
		// The counts should all be back
		MigrationTypeCounts finalCounts = entityServletHelper.getMigrationTypeCounts(userName);
		assertEquals(startCounts, finalCounts);
	}
	
	/**
	 * There must be at least one object for every type of migratable object.
	 * @param startCounts
	 */
	private void validateStartingCount(MigrationTypeCounts startCounts) {
		assertNotNull(startCounts);
		assertNotNull(startCounts.getList());
		assertEquals("This test requires at least one object to exist for each MigrationType.  Please create a new object of the new MigrationType in the before() method of this test.",MigrationType.values().length, startCounts.getList().size());
		for(MigrationTypeCount count: startCounts.getList()){
			assertTrue("This test requires at least one object to exist for each MigrationType.  Please create a new object of type: "+count.getType()+" in the before() method of this test.", count.getCount() > 0);
		}
	}


	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}
	
	/**
	 * Backup all data
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private BackupRestoreStatus backupAllOfType(MigrationType type) throws Exception {
		IdList idList = getIdListOfAllOfType(type);
		if(idList == null) return null;
		// Start the backup job
		BackupRestoreStatus status = entityServletHelper.startBackup(userName, type, idList);
		// wait for it..
		waitForDaemon(status);
		return entityServletHelper.getBackupRestoreStatus(userName, status.getId());
	}
	
	private void restoreFromBackup(MigrationType type, String fileName) throws ServletException, IOException, JSONObjectAdapterException, InterruptedException{
		RestoreSubmission sub = new RestoreSubmission();
		sub.setFileName(fileName);
		BackupRestoreStatus status = entityServletHelper.startRestore(userName, type, sub);
		// wait for it
		waitForDaemon(status);
	}
	
	/**
	 * Delete all data for a type.
	 * @param type
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private void deleteAllOfType(MigrationType type) throws ServletException, IOException, JSONObjectAdapterException{
		IdList idList = getIdListOfAllOfType(type);
		if(idList == null) return;
		MigrationTypeCount result = entityServletHelper.deleteMigrationType(userName, type, idList);
		System.out.print("Deleted: "+result);
	}
	
	/**
	 * List all of the IDs for a type.
	 * @param type
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private IdList getIdListOfAllOfType(MigrationType type) throws ServletException, IOException, JSONObjectAdapterException{
		RowMetadataResult list = entityServletHelper.getRowMetadata(userName, type, Long.MAX_VALUE, 0);
		if(list.getTotalCount() < 1) return null;
		// Create the backup list
		List<String> toBackup = new LinkedList<String>();
		for(RowMetadata row: list.getList()){
			toBackup.add(row.getId());
		}
		IdList idList = new IdList();
		idList.setList(toBackup);
		return idList;
	}
	
	/**
	 * Wait for a deamon to process a a job.
	 * @param status
	 * @throws InterruptedException 
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void waitForDaemon(BackupRestoreStatus status) throws InterruptedException, ServletException, IOException, JSONObjectAdapterException{
		long start = System.currentTimeMillis();
		while(DaemonStatus.COMPLETED != status.getStatus()){
			assertFalse("Daemon failed", DaemonStatus.FAILED == status.getStatus());
			System.out.println("Waiting for backup/restore daemon.  Message: "+status.getProgresssMessage());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for a backup/restore daemon",elapse < MAX_WAIT_MS);
			status = entityServletHelper.getBackupRestoreStatus(userName, status.getId());
		}
	}

}
