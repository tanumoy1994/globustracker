/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ranktracker.crawler.bing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.ApplicationContext;
import ranktracker.dao.KeywordsDao;
import ranktracker.utility.Bing_search;
import ranktracker.utility.FetchPagewithClientAthentication;

/**
 *
 * @author User
 */
public class BingPagenLinks implements Runnable {

    ApplicationContext appContext;
    String bnUrl;
    String bnKeyword;
    String bnLinkBing;
    int bnCrawlCount;
    int bnKeywordID;
    String bestMatchLinkBing = null;
    KeywordsDao objKeywordDao;
    FetchPagewithClientAthentication fetchclientpage;
    Bing_search bingSearch;

    public BingPagenLinks(String bnUrl, String bnKeyword, int bnKeywordID, ApplicationContext applicationContext) {
        this.bnUrl = bnUrl;
        this.bnKeyword = bnKeyword;
        this.bnKeywordID = bnKeywordID;
        this.appContext = applicationContext;
        this.objKeywordDao = appContext.getBean("objKeywordDao", KeywordsDao.class);
        this.fetchclientpage = appContext.getBean("fetchSourcewithAuthentication", FetchPagewithClientAthentication.class);
        this.bingSearch = appContext.getBean("bingSearch", Bing_search.class);
    }

    public List getOtherBingPageLinksCrawl(Queue pagenLinks, String bnLinkBing, String bnUrl, int portNo) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Queue<String>>> future = new ArrayList<>();
        List<String> pageslist = new ArrayList<>();
        Queue pagelist = pagenLinks;
        String lstpage = null;
        Iterator itr = pagelist.iterator();
        for (int i = 0; itr.hasNext(); i++) {
            String pl = (String) itr.next();
            pageslist.addAll(getExtraPageLinksCrawl(pl, future, executor, i, bnLinkBing, portNo));
            Boolean foundmatch = findLinksofDomaininPagination(pageslist, bnUrl);
            if (foundmatch) {
                return pageslist;
            }
            lstpage = pl;
            System.out.println("**********************************");
        }

        int str1 = 0;
        int str2 = 0;
        String[] splitstring1 = lstpage.split("first=");
        String newsplitstring1 = splitstring1[1];
        str1 = Integer.parseInt(newsplitstring1.substring(0, 2));
        newsplitstring1 = newsplitstring1.substring(2, newsplitstring1.length());
        str2 = Integer.parseInt(newsplitstring1.substring(newsplitstring1.length() - 1, newsplitstring1.length()));
        newsplitstring1 = newsplitstring1.replace(Integer.toString(str2), "");
        for (int i = 0; i < 15; i++) {
            str1 = str1 + 10;
            if (str2 != 4) {
                str2 = str2 + 1;
            }
            lstpage = new StringBuilder().append(splitstring1[0]).append("first=").append(str1).append(newsplitstring1).append(str2).toString();
            System.out.println("URL = " + lstpage);
            pageslist.addAll(getExtraPageLinksCrawl(lstpage, future, executor, i, bnLinkBing, portNo));
            Boolean foundmatch = findLinksofDomaininPagination(pageslist, bnUrl);
            if (foundmatch) {
                return pageslist;
            }
        }
        System.out.println("Total size of pagelist is " + pageslist.size());
        executor.shutdown();
        return pageslist;
    }

    public List getExtraPageLinksCrawl(String newpageurl, List<Future<Queue<String>>> future,
            ExecutorService executor, int i, String bnLinkBing, int portNo) throws IOException, InterruptedException {
        List<String> newpagelinks = new LinkedList<>();
        HttpGet httpget = new HttpGet((String) "http://" + bnLinkBing + newpageurl);
        future = ((List<Future<Queue<String>>>) executor.invokeAll((Arrays.asList(new GetBingThread(httpget, i, bnKeyword, portNo)))));
        for (Future<Queue<String>> futur : future) {
            try {
                while (!futur.get().isEmpty()) {
                    String s = futur.get().poll();
                    newpagelinks.add(s);
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(BingPagenLinks.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return newpagelinks;
    }

    public Boolean findLinksofDomaininPagination(List linkslist, String domainurl) {
        Boolean foundmatch = false;
        Iterator itr = linkslist.iterator();
        while (itr.hasNext()) {
            String o = itr.next().toString();
            if (o.contains(domainurl)) {
                foundmatch = true;
                break;
            }
        }
        return foundmatch;
    }

    @Override
    public void run() {
        int tStop = 0;
        Queue<String> mainbinglinks = new LinkedList<>();
        Queue<String> pagenbinglinks = new LinkedList<>();

        String responseBody;

        //get random port number
        int portNo = generateRandomPortforBing();

        try {
            List otherlinks = new ArrayList();
            bnLinkBing = "www.bing.com";
            bnCrawlCount = 0;
            String bestmatchlink = null;
            Boolean foundmatch = false;
            System.out.println("Keyword " + bnKeyword);
            String scparam = "8-" + bnKeyword.length();

            //String newurl="http://www.bing.com/search?q="+bnKeyword+"&qs=n&form=QBLH&filt=all&pq="+bnKeyword+"&sc=8-11&sp=-1&sk=";
            //http://www.bing.com/search?q=songs+hindi&qs=n&form=QBLH&filt=all&pq=songs+hindi&sc=8-11&sp=-1&sk=
            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(bnLinkBing)
                    .setPath("/search")
                    .setParameter("q", bnKeyword)
                    .setParameter("qs", "n")
                    .setParameter("form", "QBRE")
                    .setParameter("filt", "all")
                    .setParameter("pq", bnKeyword)
                    .setParameter("sc", scparam)
                    .setParameter("sp", "-1")
                    .setParameter("sk", "")
                    .build();
            System.out.println("newurl -------------------------= " + uri);

            String mainpagename = "/bingmainpage" + Thread.currentThread() + ".txt";
            String mainpage = fetchclientpage.getShowPage() + mainpagename;
            try {
                responseBody = fetchclientpage.fetchPageSourcefromClientBing(uri, mainpagename, portNo);

                mainbinglinks = bingSearch.getBingMainLinks(responseBody, bnKeyword);

                foundmatch = findLinksofSite(mainbinglinks);

                if (foundmatch) {
                    tStop = 1;

//                    File f = new File(mainpage);
//                    f.delete();
                    //Storing the Urls which are fetched from first page
//                    String path = bnKeyword + ".txt";
//                    Queue firstPageLinks = new LinkedList<>(mainbinglinks);
//                    writeUrlFile(firstPageLinks, path);
                    System.out.println("pageslist.size() " + mainbinglinks.size());
                }

                if (tStop != 1) {
                    try {
                        // I have use  this try catch (Nitesh)
                        pagenbinglinks = bingSearch.getBingPageinationLinks(responseBody);
//                    File f = new File(mainpage);
//                    f.delete();
                        otherlinks = getOtherBingPageLinksCrawl(pagenbinglinks, bnLinkBing, bnUrl, portNo);
                    } catch (Exception e) {
                    }
                }

            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Bing_search.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }

            if (tStop != 1) {
                Queue<String> pageslist = new LinkedList<>(mainbinglinks);
                pageslist.addAll(otherlinks);

                //Storing all the Urls for a given keyword into a file
//                String path = bnKeyword + ".txt";
//                writeUrlFile(pageslist, path);
                System.out.println("pageslist.size() " + pageslist.size());

                Boolean flag = true;
                Boolean flag2 = true;
                Boolean fond2 = false;
                int count = 0;
                int reg = 0;
                int newWebRankBing = 0;
                int bestMatchRankBing = 0;
                Iterator itr = pageslist.iterator();
                while (itr.hasNext()) {
                    String s = (String) itr.next();
                    count++;
                    Boolean fond = false;
                    bestmatchlink = findBestMatchLink(s, bnUrl);
                    if (bestmatchlink != null) {
                        if (flag2) {
                            bestMatchLinkBing = bestmatchlink;
                            System.out.println("bestMatchLinkBing for " + bnKeyword + " = " + bestMatchLinkBing);
                            bestMatchRankBing = count;
                            System.out.println("bestMatchRankBing for " + bnKeyword + " = " + bestMatchRankBing);
                            fond2 = true;
                            flag2 = false;
                            reg = 1;
                        }
                    }
                    fond = findBingLinkRank(s, bnKeyword, bnUrl);
                    if (fond2) {
                        if (fond) {
                            if (flag) {
                                newWebRankBing = count;
                                System.out.println("newWebRankBing for " + bnKeyword + " = " + newWebRankBing);
                                System.out.println("Best Matched Url for " + bnKeyword + " = " + s);
                                objKeywordDao.saveResult(bnKeywordID, newWebRankBing, bestMatchRankBing, bestMatchLinkBing, "bing.com");
                                flag = false;
                                reg = 2;
                                break;
                            }
                        }
                    }
                }
                if (reg == 1) {
                    objKeywordDao.saveResult(bnKeywordID, 501, bestMatchRankBing, bestMatchLinkBing, "bing.com");
                } else if (reg == 0) {
                    objKeywordDao.saveResult(bnKeywordID, 501, 501, " No Link Found ", "bing.com");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Bing_search.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public Boolean findBingLinkRank(String newurl, String keyword, String domainurl) {
        Boolean fod = false;
        Boolean flag = false;
        String[] domainsparts = new String[5];
        if (domainurl.contains("/")) {
            domainsparts = domainurl.split("/");
            flag = true;
        } else {
            domainsparts[0] = domainurl;
        }
        if (flag) {
            for (int i = 1; i < domainsparts.length; i++) {
                if (newurl.contains(domainsparts[0]) && newurl.contains(domainsparts[i])) {
                    if (newurl.contains(domainurl)) {
                        fod = true;
                    }
                }
            }
        } else {
            if (newurl.contains(domainurl) && newurl.contains(domainsparts[0])) {
                fod = true;
            }
        }
        return fod;
    }

    public String findBestMatchLink(String newurl, String domainurl) {
        String bestmatch = null;
        String[] domainsparts = new String[5];
        if (domainurl.contains("/")) {
            domainsparts = domainurl.split("/");
        } else {
            domainsparts[0] = domainurl;
        }
        if (newurl.contains(domainsparts[0])) {
            if (newurl.contains("www.")) {
                String spliturl = newurl.replace("www.", "");
                bestmatch = spliturl;
            } else if (newurl.contains("http:")) {
                String spliturl = newurl.replace("http://", "");
                bestmatch = spliturl;
            } else if (newurl.contains("https:")) {
                String spliturl = newurl.replace("https://", "");
                bestmatch = spliturl;
            } else {
                bestmatch = newurl;
            }
        }
        return bestmatch;
    }

    public Boolean findLinksofSite(Queue mainlinks) {
        String bestmatchlink = null;
        Boolean foundmatch = false;
        Boolean flag = true;
        Boolean flag2 = true;
        Boolean fond2 = false;
        int count = 0;
        int bestMatchRankBing = 0;
        int newWebRankBing = 0;
        try {
            Iterator itrr = mainlinks.iterator();
            while (itrr.hasNext()) {
                String linksinpage = (String) itrr.next();
                count++;
                Boolean fond = false;
                bestmatchlink = findBestMatchLink(linksinpage, bnUrl);
                if (bestmatchlink != null) {
                    if (flag2) {
                        bestMatchLinkBing = bestmatchlink;
                        System.out.println("bestMatchLinkBing for " + bnKeyword + " = " + bestMatchLinkBing);
                        bestMatchRankBing = count;
                        System.out.println("bestMatchRankBing for " + bnKeyword + " = " + bestMatchRankBing);
                        fond2 = true;
                        flag2 = false;
                    }
                }
                fond = findBingLinkRank(linksinpage, bnKeyword, bnUrl);
                if (fond2) {
                    if (fond) {
                        if (flag) {
                            newWebRankBing = count;
                            System.out.println("newWebRankBing for " + bnKeyword + " = " + newWebRankBing);
                            System.out.println("Best Matched Url for " + bnKeyword + " = " + linksinpage);
                            objKeywordDao.saveResult(bnKeywordID, newWebRankBing, bestMatchRankBing, bestMatchLinkBing, "bing.com");
                            foundmatch = true;
                            flag = false;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return foundmatch;
    }

    public void writeUrlFile(Queue url, String filename) {
        String str = getShowPage();
        String domainUrl = null;
        File file;
        try {
            FileOutputStream fop = null;
            file = new File(str + "/Urltextfilesbing/" + filename);
            //  System.out.println(file.getAbsolutePath());
            if (!file.exists()) {
                file.createNewFile();
            }
            fop = new FileOutputStream(file);
            Iterator itrr = url.iterator();
            while (itrr.hasNext()) {
                domainUrl = (String) itrr.next();
                byte[] contentInBytes = domainUrl.getBytes();
                fop.write(contentInBytes);
                fop.write(System.getProperty("line.separator").getBytes());
            }
            fop.flush();
            fop.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getShowPage() {
        String str = null;
        try {
            str = this.getClass().getClassLoader().getResource("").getPath();
            if (str.contains("%20")) {
                str = str.replaceAll("%20", " ");
            }
        } catch (Exception e) {
            str = System.getProperty("user.dir");
        }
        return str;
    }

    /**
     * This method generates random port number from 2732 to 2829
     *
     */
    private int generateRandomPortforBing() {

        Random random = new Random();
        int[] portList = new int[98];
        int portBegin = 2732;
        int portNo;

        for (int i = 0; i < portList.length; i++) {
            portList[i] = portBegin;
            portBegin = portBegin + 1;
        }

        int num = random.nextInt(98);
        portNo = portList[num];
        return portNo;
    }
}
