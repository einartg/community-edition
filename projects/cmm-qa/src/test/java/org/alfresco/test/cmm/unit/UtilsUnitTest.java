package org.alfresco.test.cmm.unit;

import org.alfresco.test.cmm.AbstractCMMQATest;
import org.alfresco.test.enums.CMISBinding;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UtilsUnitTest extends AbstractCMMQATest
{
    private static final Logger logger = Logger.getLogger(UtilsUnitTest.class);

    protected CMISBinding bindingType = CMISBinding.ATOMPUB10;
    
    @BeforeClass(alwaysRun = true)
    public void setupTest() throws Exception
    {
        super.setupCmis();
        testName = this.getClass().getSimpleName();
        logger.info("Starting Tests: " + testName);      
    }

    /**
     * User logs out after test is executed
     * 
     * @throws Exception
     */
    @AfterClass
    public void quit() throws Exception
    {
        driver.quit();
    }
    
       
    @Test
    public void testTestContext()
    {
       // Alfresco Instance 1
        Assert.assertNotNull(testProperties);
        Assert.assertNotNull(api);
        Assert.assertNotNull(httpClientProvider);
        Assert.assertNotNull(oAuthProps);
        Assert.assertNotNull(alfUsers);
        
        logger.info("Target URL: " + shareUrl);
        logger.info("Alfresco Version: " + alfrescoVersion);
    }
    
    
    @Test
    public void testApiAndCmisClient()
    {
        Assert.assertNotNull(apiClient, "Api Client is null");
        Assert.assertNotNull(apiClient.getApiProps().getApiUrl(), "Api URL is null");
        
        Assert.assertNotNull(cmisApiClient, "Cmis Client is null");
        Assert.assertNotNull(cmisApiClient.getCMISRepositories(new String[]{username, password}, "").get(0).getId());
    }
    
    

    @Test
    public void testLogin() throws Exception
    { 
        //Login as RepoAdmin
        loginAs(driver, new String[] {username});
                
        logout(driver);              
    }    

    @Test
    public void testGetUtils() throws Exception
    {
        String testName = getUniqueTestName();
        Assert.assertNotNull(testName);
        Assert.assertNotNull(getSiteName(testName));  
        Assert.assertNotNull(getFileName(testName));
        Assert.assertNotNull(getFolderName(testName));
        Assert.assertNotNull(getSiteShortname(testName));
        
        testName = null;
        String siteName = getSiteName(testName);
        Assert.assertNotNull(siteName);  
        Assert.assertNotNull(getFileName(testName));
        Assert.assertNotNull(getFolderName(testName));
        Assert.assertNotNull(getSiteShortname(siteName));
    }

}
