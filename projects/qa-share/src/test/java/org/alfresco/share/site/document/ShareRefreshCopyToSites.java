package org.alfresco.share.site.document;

import java.io.File;
import java.util.List;

import org.alfresco.po.share.site.document.CopyOrMoveContentPage;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.share.util.AbstractUtils;
import org.alfresco.share.util.ShareUser;
import org.alfresco.share.util.ShareUser.TypeOfPage;
import org.alfresco.share.util.ShareUserSitePage;
import org.alfresco.share.util.SiteUtil;
import org.alfresco.share.util.api.CreateUserAPI;
import org.alfresco.test.FailedTestListener;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(FailedTestListener.class)
public class ShareRefreshCopyToSites extends AbstractUtils
{

    private static final Logger logger = Logger.getLogger(ShareRefreshCopyToSites.class);

    private String testUser;

    @Override
    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception
    {
        super.setup();
        // create a single user
        testName = this.getClass().getSimpleName();
        testUser = testName + "@" + DOMAIN_FREE;
        String[] testUserInfo = new String[] { testUser };
        try
        {
            CreateUserAPI.createActivateUserAsTenantAdmin(drone, ADMIN_USERNAME, testUserInfo);
            logger.info("ShareRefreshCopyToSites users created.");
        }
        catch (Throwable e)
        {
            reportError(drone, testName, e);
        }

    }

    @BeforeMethod(groups = { "AlfrescoOne" })
    public void prepare() throws Exception
    {
        // login as created user
        try
        {
            ShareUser.login(drone, testUser, DEFAULT_PASSWORD);
            logger.info("ShareRefreshCopyToSites user logged in - drone.");
        }
        catch (Throwable e)
        {
            reportError(drone, testName, e);
        }

    }

    @AfterMethod(groups = { "AlfrescoOne" })
    public void quit() throws Exception
    {
        // login as created user
        try
        {
            ShareUser.logout(drone);
            logger.info("ShareRefreshCopyToSites user logged out - drone.");
        }
        catch (Throwable e)
        {
            reportError(drone, testName, e);
        }

    }

    /**
     * Copy to dialog
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14035() throws Exception
    {
        // create content
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        File sampleFile = SiteUtil.prepareFile();
        String fileName = sampleFile.getName();
        String[] fileInfo = { fileName, DOCLIB };
        DocumentLibraryPage documentLibraryPage = ShareUser.uploadFileInFolder(drone, fileInfo);
        CopyOrMoveContentPage copyToPage = documentLibraryPage.getFileDirectoryInfo(fileName).selectCopyTo().render();
        List<String> destinations = copyToPage.getDestinations();
        List<String> sites = copyToPage.getSites();

        if (isAlfrescoVersionCloud(drone))
        {
            // cloud
            Assert.assertTrue(destinations.contains("Recent Sites"));
            Assert.assertTrue(destinations.contains("Favorite Sites"));
            Assert.assertTrue(destinations.contains("All Sites"));
        }
        else
        {
            // enterprise
            Assert.assertTrue(destinations.contains("Recent Sites"));
            Assert.assertTrue(destinations.contains("Favorite Sites"));
            Assert.assertTrue(destinations.contains("All Sites"));
            Assert.assertTrue(destinations.contains("Repository"));
            Assert.assertTrue(destinations.contains("Shared Files"));
            Assert.assertTrue(destinations.contains("My Files"));
            Assert.assertTrue(sites.contains(siteName));

        }

    }

    /**
     * Copy to Recent Sites
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14036() throws Exception
    {

        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit recent site
        ShareUser.openUserDashboard(drone);
        String recentSiteName = testName + "RecentSite" + System.currentTimeMillis();
        SiteUtil.createSite(drone, recentSiteName, "recent site description", SITE_VISIBILITY_PUBLIC);

        // move file to recent site
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        
        ShareUser.copyOrMoveArtifact(drone, "Recent Sites", recentSiteName, sampleFile.getName(), PerformOperation.OK, TypeOfPage.CopyTo);

        // check the file is copied to recent site
        docPage = ShareUser.openSitesDocumentLibrary(drone, recentSiteName);
        Assert.assertTrue(docPage.isFileVisible(sampleFile.getName()));

    }

    /**
     * Copy to Recent Sites - Cancel
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14037() throws Exception
    {

        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit recent site
        ShareUser.openUserDashboard(drone);
        String recentSiteName = testName + "RecentSite" + System.currentTimeMillis();
        ShareUser.createSite(drone, recentSiteName, SITE_VISIBILITY_PUBLIC);

        // move file to recent site - cancel
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);

        ShareUser.copyOrMoveArtifact(drone, "Recent Sites", recentSiteName, sampleFile.getName(), PerformOperation.CANCEL, TypeOfPage.CopyTo );

        // check the file is not copied to the recent site
        docPage = ShareUser.openSitesDocumentLibrary(drone, recentSiteName);
        Assert.assertFalse(docPage.isFileVisible(sampleFile.getName()));

    }

    /**
     * Copy to Favourite Sites
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14038() throws Exception
    {

        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit favourite site
        ShareUser.openUserDashboard(drone);
        String favouriteSiteName = testName + "FavouriteSite" + System.currentTimeMillis();
        ShareUser.createSite(drone, favouriteSiteName, SITE_VISIBILITY_PUBLIC);

        // move file to recent site
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);

        ShareUser.copyOrMoveArtifact(drone, "Favorite Sites", favouriteSiteName, sampleFile.getName(), PerformOperation.OK, TypeOfPage.CopyTo);
        
        // check the file is copied to favourite site
        docPage = ShareUser.openSitesDocumentLibrary(drone, favouriteSiteName);
        Assert.assertTrue(docPage.isFileVisible(sampleFile.getName()));
    }

    /**
     * Copy to Favourite Sites - Cancel
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14039() throws Exception
    {

        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit recent site
        ShareUser.openUserDashboard(drone);
        String favouriteSiteName = testName + "FavouriteSite" + System.currentTimeMillis();
        ShareUser.createSite(drone, favouriteSiteName, SITE_VISIBILITY_PUBLIC);

        // move file to recent site - cancel
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);

        ShareUser.copyOrMoveArtifact(drone, "Favorite Sites", favouriteSiteName, sampleFile.getName(), PerformOperation.CANCEL, TypeOfPage.CopyTo);

        // check the file is not present in the favourite site
        docPage = ShareUser.openSitesDocumentLibrary(drone, favouriteSiteName);
        Assert.assertFalse(docPage.isFileVisible(sampleFile.getName()));

    }

    /**
     * Copy to Sites
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14040() throws Exception
    {
        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit recent site
        ShareUser.openUserDashboard(drone);
        String anySiteName = testName + "AnySite" + System.currentTimeMillis();
        ShareUser.createSite(drone, anySiteName, SITE_VISIBILITY_PUBLIC);

        // move file to recent site
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);

        ShareUser.copyOrMoveArtifact(drone, "All Sites", anySiteName, sampleFile.getName(), PerformOperation.OK, TypeOfPage.CopyTo);

        // check the file is present in public site
        docPage = ShareUser.openSitesDocumentLibrary(drone, anySiteName);
        Assert.assertTrue(docPage.isFileVisible(sampleFile.getName()));
    }

    /**
     * Copy to Sites - Cancel
     * enterprise
     * cloud
     * 
     * @throws Exception
     */

    @Test(groups = { "AlfrescoOne" })
    public void AONE_14041() throws Exception
    {
        // create site
        String testName = getTestName();
        String siteName = testName + System.currentTimeMillis();
        ShareUser.createSite(drone, siteName, SITE_VISIBILITY_PUBLIC);

        // upload file
        DocumentLibraryPage docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
        File sampleFile = SiteUtil.prepareFile();
        ShareUserSitePage.uploadFile(drone, sampleFile);

        // create and visit recent site
        ShareUser.openUserDashboard(drone);
        String anySiteName = testName + "AnySite" + System.currentTimeMillis();
        ShareUser.createSite(drone, anySiteName, SITE_VISIBILITY_PUBLIC);

        // move file to recent site - cancel
        docPage = ShareUser.openSitesDocumentLibrary(drone, siteName);
    
        ShareUser.copyOrMoveArtifact(drone, "All Sites", anySiteName, sampleFile.getName(), PerformOperation.CANCEL, TypeOfPage.CopyTo);

        // check the file is not present in public site
        docPage = ShareUser.openSitesDocumentLibrary(drone, anySiteName);
        Assert.assertFalse(docPage.isFileVisible(sampleFile.getName()));

    }

}
