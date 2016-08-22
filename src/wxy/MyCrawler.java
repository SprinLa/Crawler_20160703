package wxy;

import wxy.Utils.CBloomFilter;
import wxy.Utils.PageInfo;
import wxy.Utils.UrlInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author delia
 * @create 2016-06-28 下午7:55
 */

// AddCrawlList 结果集
enum ENUM_ADD_RES{
SUCCESS,// 正常返回
TIMEOUT,// put url 超时
URLINFO_NULL,// urlInfo == null
PUT_INTERRUPT,// url put interrupted
SURL_NULL,// sUrl = null
EXSITES // mb过滤错误,已存在url
}
public class MyCrawler {
    public MyCrawler(String sUrl, int maxNUm, String filePath) {
        this.initUrl = sUrl;
        this.MAX_NUM = maxNUm;
        this.filePath = filePath;
        urlist = new LinkedBlockingQueue<UrlInfo>(MAX_NUM*500);/*---------------------大小待商榷------------------------*/
    }
    
    public static void main(String[] args) {
        String filePath = "/Users/delia/Documents/workspace/Crawler/out/urlResult.txt";//urlResult.txt
        int MAX_NUM = 100;
        MyCrawler mc = new MyCrawler("http://www.bupt.edu.cn",MAX_NUM,filePath);//
        //MyCrawler mc = new MyCrawler("http://net.china.com.cn/",MAX_NUM,filePath);//
        mc.startTime = System.currentTimeMillis();
        mc.crawl();
    }
    
    //爬取方法
    public void crawl() {
        if (initUrl == null || initUrl.equals("") || initUrl.length() == 0) {
            System.out.println("入口url不能为空,请重新输入!");
        }
        try {
            this.fileWriter = new FileWriter(filePath);
        } catch (IOException e) {
            System.out.println("打开文件错误,请检查路径后重启程序!");
            e.printStackTrace();
            System.exit(1);
        }
        this.bufferedWriter = new BufferedWriter(fileWriter);
        cachedPool = Executors.newCachedThreadPool();
        UrlInfo urlInfo = new UrlInfo(initUrl,initUrl);
        ENUM_ADD_RES addRes = AddCrawlList(initUrl, urlInfo);
        if (!ENUM_ADD_RES.SUCCESS.equals(addRes)){
            System.out.println(Thread.currentThread().getName()+" AddCrawlList 出错,errorCode="+addRes);
            return;
        }
        
        int num = 0;
        for (;num < MAX_NUM; num++) {
            try {
                UrlInfo u = urlist.poll(100,TimeUnit.SECONDS); // 此处导致线程无法终止,url个数不足,阻塞
                if (u == null){
                    System.out.println("获取url超过100s,终止程序!");
                    endProgram(num);
                    System.exit(1);
                }
                cachedPool.execute(new TaskCrawlPages(u));
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("main:获取url的阻塞被打断了!");
                e.printStackTrace();
                break;
            }
        }
        cachedPool.shutdown();
        try{
            cachedPool.awaitTermination(1,TimeUnit.DAYS);
        }catch(InterruptedException e){
            System.out.println("执行超时退出!");
            e.printStackTrace();
            System.exit(1);
        }
        endProgram(num);
    }
    
    // 关闭资源,打印结果
    public void endProgram(int num){
        try {
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName()+" bufferedWriter,fileWriter 关闭出错!");
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();
        System.out.println("(执行次数:"+num+") 已爬取 " + crawlNum.get() + " 个,程序终止,执行时间:" + (endTime - startTime) / 1000.0 + "s"+
                           " 最长connect时间= "+getInputStreamTime+" 最长读取网页时间= "+getPageTime+" 最长网页解析时间= "+parsePageTime+" 最长执行时间= "+EndTime+" 已爬取队列长度 = "+doneList.size()+" errorNum = "+errorList.size());
        System.out.println("[以下为出错url:]");
        Iterator it = errorList.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entry = (Map.Entry)it.next();
            System.out.println(entry.getKey()+"  "+entry.getValue());
        }
    }
    
    
    
    
    
    
    
    public void crawlPage(UrlInfo urlInfo) {
        
        //   ====** 开始线程 **============================================================================================
        
        long cStartTime = System.currentTimeMillis();//递增爬取数目crawlNum;
        String sUrl = urlInfo.getsUrl();
        String sRoot = urlInfo.getsCrawlUrl();
        doneList.add(sUrl);
        synchronized (crawlNum) {
            crawlNum.addAndGet(1);
            System.out.println(Thread.currentThread().getName()+" 开始:爬取第"+crawlNum.get()+"个,(urlRoot:) "+sRoot+" (url:) "+sUrl);
            try {
                bufferedWriter.write(crawlNum.get() + "    " + Thread.currentThread().getName() + "    " + sUrl + "\n");
            } catch (IOException e) {
                System.out.println(Thread.currentThread().getName()+" 写文件错误,return!");
                e.printStackTrace();
                return;
            }
        }
        
        //   ====** 获取服务器响应 **=======================================================================================
        
        long t_connect_Start = System.currentTimeMillis();
        URL url = null;// MalformedURLException
        HttpURLConnection connection = null;//IOException
        StringBuffer sHtml = new StringBuffer();// Read page into buffer.
        InputStream in = null;//发送http请求,返回输入流,读取服务器返回信息
        try {
            url = new URL(sUrl);
            if(null != url.getRef()){
                URL u = url;
                url = new URL(u.getProtocol(), u.getHost(), u.getPort(),u.getFile());
            }
            connection = (HttpURLConnection) url.openConnection();
            //connection.setRequestMethod("POST");// ProtocolException
            connection.setRequestMethod("GET");// ProtocolException
            //connection.setDoOutput(true);
            connection.setDoInput(true);// 设置e是否从httpUrlConnection读入，默认情况下是true;
            connection.setUseCaches(true);// Post 请求不能使用缓存
            connection.setRequestProperty("Accept-Encoding", "identity");//告诉服务器你的浏览器支持gzip解压 gzip,deflate
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Length","0");
            connection.setConnectTimeout(2*1000);
            connection.setReadTimeout(5*1000);
            
            String type = connection.getContentType();
            if(type == null ||(type.indexOf("text")==-1 &&
                               type.indexOf("txt")==-1&&
                               type.indexOf("HTM")==-1&&
                               type.indexOf("htm")==-1)){
                //System.err.println("bad content type "+type+" at site"+site);
                System.out.println(Thread.currentThread().getName()+" --------------------------------bad content type "+type+" at url"+sUrl);
                errorList.put(sUrl,"content-type:"+type);
                return;
            }
            
            int iState = 0;
            iState = connection.getResponseCode();
            if (200 != iState) {
                System.out.println(Thread.currentThread().getName()+" --------------------------------[-url] 响应码:"+iState+",return!");
                errorList.put(sUrl,"响应码:"+iState);
                return;//只存储200 OK成功响应,其他无视
            }
            
            long t_connect_End = System.currentTimeMillis();
            long t_connect = t_connect_End-t_connect_Start;
            getInputStreamTime = t_connect > getInputStreamTime?t_connect:getInputStreamTime;
            System.out.println(Thread.currentThread().getName()+"    获取响应 耗时: "+t_connect);
            //   ====** 获取服务器响应 end **
            //   ====** 读取网页 **============================================================================================
            /*{
             此处应有编码解码
             }*/
            long tReadStart = System.currentTimeMillis();
            BufferedReader reader;
            String line;
            in = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null ) {// SocketTimeoutException
                sHtml.append(line + "\n");
                //System.out.println(line);
            }
            //if (sUrl.equals("http://net.china.com.cn/txt/2015-12/07/content_8429444.htm")){
            //    bufferedWriter.write(sHtml.toString());
            //}
            
            
            long tReadEnd = System.currentTimeMillis();
            long t_Read = tReadEnd-tReadStart;
            getPageTime = (t_Read > getPageTime) ? t_Read : getPageTime;
            System.out.println(Thread.currentThread().getName()+"    读取网页 耗时 "+t_Read);
            
            reader.close();
            in.close();
            connection.disconnect();
            
        } catch (Exception e) {
            long t_exception = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName()+"-------------Exception,return!耗时: "+(t_exception-cStartTime)+ "(urlRoot:) "+sRoot+" (url:) "+sUrl);
            e.printStackTrace();
            errorList.put(sUrl,e.getMessage());
            return;
        }//  ====** 读取网页 end **
        //   ====** 解析网页 **=============================================================================================
        
        long t_parse_start = System.currentTimeMillis();
        int urlNum = 0;
        if (sHtml.length() != 0) {
            int len = connection.getContentLength();
            urlNum = parsePage(new PageInfo(sUrl, sHtml.toString(), len));//  解析网页
            if (-1 == urlNum){
                System.out.println(Thread.currentThread().getName()+" 解析网页");
            }
        }else{
            errorList.put(sUrl,"sHtml.length() = 0");
            System.out.println("-------------"+Thread.currentThread().getName()+" sHtml.length() = 0,url="+sUrl);
            return;
        }
        long t_parse_end = System.currentTimeMillis();
        long parseTime = t_parse_end - t_parse_start;
        parsePageTime = (parseTime > parsePageTime)?parseTime:parsePageTime;
        System.out.println(Thread.currentThread().getName()+"    解析网页,获取url "+urlNum+" 个,耗时: "+parseTime);
        //   ====** 解析网页 end **
        
        long cEndTime = System.currentTimeMillis();
        long singeLongTime = cEndTime - cStartTime;
        EndTime = (singeLongTime > EndTime)?singeLongTime:EndTime;
        System.out.println(Thread.currentThread().getName()+" 结束:time = "+(cEndTime-cStartTime)+" urlist.size="+urlist.size());
        
        //   ====** 结束 **================================================================================================
    }
    
    
    
    
    
    
    
    
    public ENUM_ADD_RES AddCrawlList(String absolutePath, UrlInfo urlInfo){
        if (urlInfo == null) return ENUM_ADD_RES.URLINFO_NULL;//urlInfo == null
        String sUrl = checkUrl(absolutePath, urlInfo.getsUrl());
        if (sUrl != null) {
            synchronized (mb) {
                if (mb.add(sUrl)) {//bloom过滤器去重复url
                    try {
                        boolean isPut = urlist.offer(urlInfo, 1, TimeUnit.SECONDS);
                        if (!isPut) {
                            return ENUM_ADD_RES.TIMEOUT;//put url 超时
                        }
                        return ENUM_ADD_RES.SUCCESS;// 正常返回
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return ENUM_ADD_RES.PUT_INTERRUPT;// url put interrupted
                    }
                    
                } else {
                    return ENUM_ADD_RES.EXSITES;// mb过滤错误,已存在url
                }
            }
        }else{
            return ENUM_ADD_RES.SURL_NULL;
        }
        
    }
    // 抓取页面线程
    class TaskCrawlPages implements Runnable {
        private UrlInfo urlInfo = null;
        
        public TaskCrawlPages(UrlInfo urlInfo) {
            this.urlInfo = urlInfo;
        }
        
        @Override
        public void run() {
            crawlPage(urlInfo);//抓取网页
        }
    }
    
    
    public int parsePage(PageInfo pageInfo){
        String sHtml = null;
        String sCrawlUrl = null;
        int urlNum = 0;
        sHtml = pageInfo.getsHtml();
        sCrawlUrl = pageInfo.getsUrl();
        String regex = "<a.*?/a>";//任意字符任意次,尽可能少的匹配 .*表示任意字符,?表示懒惰匹配
        Pattern pt = Pattern.compile(regex);
        Matcher mt = pt.matcher(sHtml);
    loop:
        while (mt.find()) {
            Matcher myurl = Pattern.compile("href=[ ]*\".*?\"").matcher(mt.group());
            while (myurl.find()) {
                String sUrl = myurl.group().replaceAll("href=|>", "").trim();
                int lenUrl = sUrl.length();
                sUrl = sUrl.substring(1, lenUrl - 1).replaceAll("\\s*", "");// \s表示空白
                String urlToCheck = null;
                if (sUrl != null && sUrl.length() != 0) {
                    urlToCheck = sUrl;
                    String urLast = checkUrl(sCrawlUrl, urlToCheck);
                    if (urLast == null) {
                        //Systxz.out.println(Thread.currentThread().getName()+"-----------url=null! sCrawlUrl: "+sCrawlUrl+" urlToCheck: "+urlToCheck+" myurl: "+myurl.group()+" ----[url = NULL!]");
                        continue ;
                    }else {
                        //判断size
                        UrlInfo urlInfo =  new UrlInfo(sCrawlUrl, urLast);
                        ENUM_ADD_RES addCode = AddCrawlList(sCrawlUrl,urlInfo);
                        if(!ENUM_ADD_RES.SUCCESS.equals(addCode)){
                            if (ENUM_ADD_RES.TIMEOUT.equals(addCode)){
                                System.out.println(Thread.currentThread().getName()+" put url等待1s超时! urlist.size="+urlist.size()+",停止解析本网页!");
                                break loop;
                                
                            }
                        }else{
                            urlNum ++;
                        }
                    }
                }
            }
        }
        return urlNum;
    }
    
    public String checkUrl(String absolutePath, String sUrl) {
        if (sUrl == null || sUrl.length() ==0 || sUrl.equals(""))
            return null;
        if (sUrl.contains("javascript") || sUrl.contains("javacript") || sUrl.contains("javascritp") || sUrl.contains("<%=") || sUrl.indexOf("'") == 0) {
            return null;
        }
        if (!sUrl.toLowerCase().startsWith("http://") && !sUrl.toLowerCase().startsWith("https://")) {
            //if (sUrl.indexOf("/") == 0 || sUrl.indexOf("../") == 0 || sUrl.indexOf("./") == 0 || absolutePath.lastIndexOf("/") == (absolutePath.length() -1)) {
            URL absoluteUrl, parseUrl;
            try {
                absoluteUrl = new URL(absolutePath);
                parseUrl = new URL(absoluteUrl, sUrl);
                return parseUrl.toString();
            } catch (MalformedURLException e) {
                //System.out.println(Thread.currentThread().getName()+" checkUrl Exception!抛弃此url!(rootUrl:)"+absolutePath+" (url:)"+sUrl);
                //e.printStackTrace();
                return null;
            }
        }else {
            URL verifiedUrl = null;
            try {
                verifiedUrl = new URL(sUrl);
            } catch (Exception e) {
                return null;
            }
            return verifiedUrl.toString();
        }
    }
    
    private String initUrl = null;
    private int MAX_NUM = 1;
    public static CBloomFilter mb = new CBloomFilter();
    public static BlockingQueue<UrlInfo> urlist;
    public static BlockingQueue<String> doneList = new LinkedBlockingQueue<String>();
    public static HashMap<String,String> errorList= new HashMap<>();
    private static AtomicInteger crawlNum = new AtomicInteger(0);
    private FileWriter fileWriter = null;
    private BufferedWriter bufferedWriter = null;
    private String filePath = null;
    public long startTime = 0;
    private long endTime = 0;
    private static ExecutorService cachedPool;
    private long getInputStreamTime = 0;
    private long getPageTime = 0;
    private long parsePageTime = 0;
    private long EndTime = 0;
}

