/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ranktracker.action;

import com.opensymphony.xwork2.ActionSupport;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.struts2.ServletActionContext;
import ranktracker.entity.Campaigns;
import ranktracker.entity.Serpkeywords;
import ranktracker.entity.Videokeywords;
import ranktracker.form.RankComparision;
import ranktracker.service.CampaignsService;

/**
 *
 * @Laxmi Kiran Nallam <laxmikiran@globussoft.com>
 */
public class VideoTrackAction extends ActionSupport {

    /**
     * lstCampaigns The list containing Campaigns objects
     */
    private List<Campaigns> lstCampaigns;
    /**
     * lstCampaignsSearch The list containing Campaigns objects
     */
    private List<Campaigns> lstCampaignsSearch;
    /**
     * lstKeywords The list containing Videokeywords objects
     */
    private List<Videokeywords> lstKeywords;
    /**
     * objCampaignsService The service layer object variable for
     * CampaignsService
     */
    private CampaignsService objCampaignsService;
    /**
     * objRequest The HttpServletRequest object
     */
    private HttpServletRequest objRequest;
    /**
     * objSession The HttpSession object
     */
    private HttpSession objSession;
    /**
     * jString The Json string received from ajax call
     */
    private String jString;
    /**
     * message The ajax string variable to store action message
     */
    private String message;
    /**
     * lstKeywordData The list containing Integer Keyword Id's
     */
    private List<Integer> lstKeywordData;
    /**
     * activeKeywordsCount The current keyword usage of a given customer
     */
    private Integer activeKeywordsCount;
    /**
     * allowedKeywordsCount The allowed keyword usage for a given customer
     */
    private Integer allowedKeywordsCount;
    /**
     * seacrhCampaign The search campaign value
     */
    private String seacrhCampaign;
    /**
     * quotaCompleted The boolean variable indicating quota status for a given
     * customer
     */
    private int quotaCompleted;
    /**
     * object for rank compare form
     */
    private RankComparision rankComparision;

    /**
     * The method retrieves campaigns based on customer id
     *
     * @return struts return value
     * @throws Exception
     */
    @Override
    public String execute() throws Exception {

        //initializing http session object
        objRequest = ServletActionContext.getRequest();

        //initializing http session object
        objSession = objRequest.getSession();

        //checking for 'customerID' attribute in session
        if (objSession.getAttribute("customerID") != null) {

            if (objSession.getAttribute("activationPeriod").toString().equals("0")) {                
                return "renewal";
            }
            //reading the 'customerID' from session and type casting it to integer
            String sCustomerID = objSession.getAttribute("customerID").toString();
            Integer customerID = Integer.parseInt(sCustomerID);

            //invoking the getData() method of CampaignsServiceImpl
            Object[] dataObject = objCampaignsService.getVideoData(customerID);

            //retrieving theb list of keywords data from dataObject
            lstKeywordData = (List<Integer>) dataObject[0];

            //now retrieving the active keyword count and allowed keyword count
            activeKeywordsCount = lstKeywordData.get(0);
            allowedKeywordsCount = lstKeywordData.get(1);

            //retrieving the list of campaigns
            lstCampaigns = (List<Campaigns>) dataObject[1];
            lstCampaignsSearch = (List<Campaigns>) dataObject[1];
            lstKeywords = objCampaignsService.getRankDataVideo(customerID);
            int count5 = 0;
            int count10 = 0;
            int count20 = 0;
            int count30 = 0;
            int count100 = 0;
            int keywordcount = lstKeywords.size();
            int keyrank;
            rankComparision = new RankComparision();
            if (!lstKeywords.isEmpty()) {
                Iterator itr = lstKeywords.iterator();
                while (itr.hasNext()) {
                    Videokeywords keys = (Videokeywords) itr.next();
                    keyrank = keys.getRankYoutube();
                    if (keyrank <= 5 & keyrank != 0) {
                        count5++;
                    }
                    if (keyrank <= 10 & keyrank != 0) {
                        count10++;
                    }
                    if (keyrank <= 20 & keyrank != 0) {
                        count20++;
                    }
                    if (keyrank <= 30 & keyrank != 0) {
                        count30++;
                    }
                    if (keyrank <= 100 & keyrank != 0) {
                        count100++;
                    }
                }
                rankComparision.setKeywordsRankBelow5(count5);
                rankComparision.setKeywordsRankBelow10(count10);
                rankComparision.setKeywordsRankBelow20(count20);
                rankComparision.setKeywordsRankBelow30(count30);
                rankComparision.setKeywordsRankBelow100(count100);
                rankComparision.setTotalkeywords(keywordcount);
            }
            objRequest.setAttribute("highlight", "CAMPAIGN");
            if (objSession.getAttribute("message") != null) {
                addActionMessage(objSession.getAttribute("message").toString());
                objSession.removeAttribute("message");
            }
            return SUCCESS;

        } else {

            //if session attribute 'customerID' is null then return result parameter as 'LOGIN'
            //this result parameter is mapped in 'struts.xml'
            return LOGIN;
        }
    }

    /**
     * The method adds a new campaign to database
     *
     * @return struts return value
     */
    public String addVideoCampaign() {

        //initializing http request object
        objRequest = ServletActionContext.getRequest();

        //initializing http session object
        objSession = objRequest.getSession();

        //checking for 'customerID' attribute in session
        if (objSession.getAttribute("customerID") != null) {
            String campaignName = "";
            campaignName = jString;

            //checking for validation, campaign name must be alphanumeric with space
            boolean b = checkValidation(campaignName);
            if (!b) {

                //if validation failed then add action message
                message = "Campaign Name should be alphanumeric";
                objSession.setAttribute("message", message);
                addActionError(message);
                return SUCCESS;
            }
            if (!campaignName.equals("")) {

                //reading the 'userID' from session and type casting it to integer
                Integer userID = Integer.parseInt(objSession.getAttribute("userID").toString());
                System.out.println("campaignName:::" + campaignName);
                //int Quota=objCampaignsService.checkCampaignQuota(customerID);
                //int campaignId = 1;

                //now invoking the addCampaign() method of CampaignsServiceImpl
                int campaignId = objCampaignsService.addVideoCampaign(campaignName, userID);
                if (campaignId == -1) {
                    message = "Sorry This Campaign value already exist in Database";
                } else if (campaignId == -10) {
                    message = "Your Assigned Quota for Number of Campaigns is Full.";
                    quotaCompleted = 1;
                    setQuotaCompleted(quotaCompleted);
                } else {
                    message = "Campaign has been created. Please Click on Campaign Name to Add Keywords";
                }
            } else {
                message = "Campaign Value required";
            }
        } else {

            //if session attribute 'customerID' is null then return result parameter as 'LOGIN'
            //this result parameter is mapped in 'struts.xml'
            return LOGIN;
        }
        objSession.setAttribute("message", message);
        return SUCCESS;
    }

    /**
     * The method validates a campaign name for special characters
     *
     * @param campaign
     * @return boolean true if campaign name contains special characters
     */
    public boolean checkValidation(String campaign) {
        String pattern = "([a-zA-Z0-9]+([ ][a-zA-Z0-9])*)+";
        if (campaign.matches(pattern)) {
            return true;
        }
        return false;
    }

    /**
     * The method edits/updates a given campaign name
     *
     * @return struts return value
     */
    public String editVideoCampaign() {

        //initializing http request object
        objRequest = ServletActionContext.getRequest();

        //initializing http session object
        objSession = objRequest.getSession();

        //checking for 'customerID' attribute in session
        if (objSession.getAttribute("customerID") != null) {
            String[] data = jString.split(":");
            Integer campaignId = Integer.parseInt(data[0]);
            // System.out.println("campaignId ------------------= " + campaignId);
            String campaignName = data[1];
            // System.out.println("campaignName --------------= " + campaignName);
            Integer customerID = Integer.parseInt(objSession.getAttribute("customerID").toString());
            //  System.out.println("customerID = " + customerID);
            //reading the 'userID' from session and type casting it to integer
            Integer userID = Integer.parseInt(objSession.getAttribute("userID").toString());
            System.out.println("userID = " + userID);
            //System.out.println("Site id is:::" + campaignId);

            //now invoking the editCampaign() method of CampaignsServiceImpl
            int updated = objCampaignsService.editCampaign(campaignId, campaignName, customerID);
            if (updated == 1) {
                message = "Campaign has been Edited";
                System.out.println("message = " + message);
            } else {
                message = "Sorry Campaign Name " + campaignName + " already Exist";
                System.out.println("message = " + message);
                addActionError(message);
            }
            objSession.setAttribute("message", message);
            return SUCCESS;
        } else {

            //if session attribute 'customerID' is null then return result parameter as 'LOGIN'
            //this result parameter is mapped in 'struts.xml'
            return LOGIN;
        }
    }

    /**
     * The method deletes a given campaign based on campaign id
     *
     * @return struts return value
     */
    public String deleteVideoCampaign() {

        //initializing http request object
        objRequest = ServletActionContext.getRequest();

        //initializing http session object
        objSession = objRequest.getSession();

        //checking for 'customerID' attribute in session
        if (objSession.getAttribute("customerID") != null) {
            Integer campaignId = 0;
            campaignId = Integer.parseInt(jString);
            System.out.println("Site id is:::" + campaignId);

            //reading the 'customerID' from session and type casting it to integer
            Integer customerID = (Integer) objSession.getAttribute("customerID");

            //now invoking the deleteCampaign() method of CampaignsServiceImpl
            String campaignName = objCampaignsService.deleteVideoCampaign(campaignId, customerID);
            message = "'" + campaignName + "' - Campaign has been Deleted";
            objSession.setAttribute("message", message);
            return SUCCESS;
        }

        //if session attribute 'customerID' is null then return result parameter as 'LOGIN'
        //this result parameter is mapped in 'struts.xml'
        return LOGIN;
    }

    /**
     * The method retrieves campaigns based on given search key
     *
     * @return struts return value
     */
    public String searchVideoCampaign() {

        //initializing http request object
        objRequest = ServletActionContext.getRequest();

        //initializing http session object
        objSession = objRequest.getSession();

        //checking for 'customerID' attribute in session
        if (objSession.getAttribute("customerID") != null) {

            //reading the 'customerID' from session and type casting it to integer
            Integer customerID = (Integer) objSession.getAttribute("customerID");
            String campaign = objRequest.getParameter("searchVideoCampaign");
            if (campaign.equals("Search Campaign")) {
                message = "Campaign name is Required";
                addActionError(message);
                return INPUT;
            }

            //now invoking the searchCampaign() method of CampaignsServiceImpl
            lstCampaigns = objCampaignsService.searchCampaign(campaign, customerID);
            Object[] dataObject = objCampaignsService.getVideoData(customerID);
            lstCampaignsSearch = (List<Campaigns>) dataObject[1];

            //retrieving the list of keywords
            lstKeywordData = (List<Integer>) dataObject[0];

            //retrieving active keyword count and allowed keyword count
            activeKeywordsCount = lstKeywordData.get(0);
            allowedKeywordsCount = lstKeywordData.get(1);
            lstKeywords = objCampaignsService.getRankDataVideo(customerID);
            int count5 = 0;
            int count10 = 0;
            int count20 = 0;
            int count30 = 0;
            int count100 = 0;
            int keywordcount = lstKeywords.size();
            int keyrank;
            rankComparision = new RankComparision();
            if (!lstKeywords.isEmpty()) {
                Iterator itr = lstKeywords.iterator();
                while (itr.hasNext()) {
                    Videokeywords keys = (Videokeywords) itr.next();
                    keyrank = keys.getRankYoutube();
                    if (keyrank <= 5 & keyrank !=0) {
                        count5++;
                    }
                    if (keyrank <= 10 & keyrank !=0) {
                        count10++;
                    }
                    if (keyrank <= 20 & keyrank !=0) {
                        count20++;
                    }
                    if (keyrank <= 30 & keyrank !=0) {
                        count30++;
                    }
                    if (keyrank <= 100 & keyrank !=0) {
                        count100++;
                    }
                }
                rankComparision.setKeywordsRankBelow5(count5);
                rankComparision.setKeywordsRankBelow10(count10);
                rankComparision.setKeywordsRankBelow20(count20);
                rankComparision.setKeywordsRankBelow30(count30);
                rankComparision.setKeywordsRankBelow100(count100);
                rankComparision.setTotalkeywords(keywordcount);
            }
            if (lstCampaigns.isEmpty()) {
                message = "Sorry! Your Search Campaign not found!";
                addActionMessage(message);
//                setMessage(message);
//                seacrhCampaign = campaign;
//                setSeacrhCampaign(seacrhCampaign);
//                objRequest.setAttribute("notfound", campaign);
            }
            return SUCCESS;
        } else {

            //if session attribute 'customerID' is null then return result parameter as 'LOGIN'
            //this result parameter is mapped in 'struts.xml'
            return LOGIN;
        }
    }

    /**
     *
     * @return lstCampaigns
     */
    public List<Campaigns> getLstCampaigns() {
        return lstCampaigns;
    }

    /**
     *
     * @param lstCampaigns
     */
    public void setLstCampaigns(List<Campaigns> lstCampaigns) {
        this.lstCampaigns = lstCampaigns;
    }

    /**
     *
     * @return objCampaignsService
     */
    public CampaignsService getObjCampaignsService() {
        return objCampaignsService;
    }

    /**
     *
     * @param objCampaignsService
     */
    public void setObjCampaignsService(CampaignsService objCampaignsService) {
        this.objCampaignsService = objCampaignsService;
    }

    /**
     *
     * @return jString
     */
    public String getJString() {
        return jString;
    }

    /**
     *
     * @param jString
     */
    public void setJString(String jString) {
        this.jString = jString;
    }

    /**
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     *
     * @return activeKeywordsCount
     */
    public Integer getActiveKeywordsCount() {
        return activeKeywordsCount;
    }

    /**
     *
     * @param activeKeywordsCount
     */
    public void setActiveKeywordsCount(Integer activeKeywordsCount) {
        this.activeKeywordsCount = activeKeywordsCount;
    }

    /**
     *
     * @return allowedKeywordsCount
     */
    public Integer getAllowedKeywordsCount() {
        return allowedKeywordsCount;
    }

    /**
     *
     * @param allowedKeywordsCount
     */
    public void setAllowedKeywordsCount(Integer allowedKeywordsCount) {
        this.allowedKeywordsCount = allowedKeywordsCount;
    }

    /**
     *
     * @return seacrhCampaign
     */
    public String getSeacrhCampaign() {
        return seacrhCampaign;
    }

    /**
     *
     * @param seacrhCampaign
     */
    public void setSeacrhCampaign(String seacrhCampaign) {
        this.seacrhCampaign = seacrhCampaign;
    }

    /**
     *
     * @return quotaCompleted
     */
    public int getQuotaCompleted() {
        return quotaCompleted;
    }

    /**
     *
     * @param quotaCompleted
     */
    public void setQuotaCompleted(int quotaCompleted) {
        this.quotaCompleted = quotaCompleted;
    }

    public List<Videokeywords> getLstKeywords() {
        return lstKeywords;
    }

    public void setLstKeywords(List<Videokeywords> lstKeywords) {
        this.lstKeywords = lstKeywords;
    }

    public RankComparision getRankComparision() {
        return rankComparision;
    }

    public void setRankComparision(RankComparision rankComparision) {
        this.rankComparision = rankComparision;
    }

    public List<Campaigns> getLstCampaignsSearch() {
        return lstCampaignsSearch;
    }

    public void setLstCampaignsSearch(List<Campaigns> lstCampaignsSearch) {
        this.lstCampaignsSearch = lstCampaignsSearch;
    }

}
