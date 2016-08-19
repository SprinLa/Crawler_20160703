package wxy;

import wxy.Utils.CBloomFilter;
import wxy.Utils.PageInfo;
import wxy.Utils.UrlInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author delia
 * @create 2016-06-28 下午7:55
 */

public class MyCrawler {
    private String initUrl = null;
    private int MAX_NUM = 1;
    public static CBloomFilter mb = new CBloomFilter();
    public static BlockingQueue<UrlInfo> urlist;
    private static AtomicInteger crawlNum = new AtomicInteger(0);
    private static AtomicInteger errorNum = new AtomicInteger(0);
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


    public MyCrawler(String sUrl, int maxNUm, String filePath) {
        this.initUrl = sUrl;
        this.MAX_NUM = maxNUm;
        this.filePath = filePath;
        urlist = new LinkedBlockingQueue<UrlInfo>(MAX_NUM*500);/*---------------------大小待商榷------------------------*/
    }

    public static void main(String[] args) {
        String filePath = "/Users/delia/Documents/workspace/Crawler/out/urlResult.txt";//urlResult.txt
        int MAX_NUM = 1;
        //MyCrawler mc = new MyCrawler("http://weibo.com/BaiduTVgame",MAX_NUM,filePath);//http://rj.baidu.com/
        MyCrawler mc = new MyCrawler("http://tieba.baidu.com/f?kw=&fr=wwwt",MAX_NUM,filePath);//http://rj.baidu.com/
        // MyCrawler mc = new MyCrawler("http://tieba.baidu.com/f?kw=&fr=wwwt",MAX_NUM,filePath);
        // http://music.baidu.com/song/242078437?infrom=dayhot&amp;uid=40C0332B-0B7D-3919-AC90-FF8A038822D7 连接超时
        // http://ivr.baidu.com/other/s57b426b34b33.html
        // http://music.baidu.com/song/242078465?infrom=dayhot&amp;uid=3D5AB83A-C703-F265-7600-E4FFF634DB93
        //读超时 <验证:UnknownHostException: v.hao123.com http,连接错误>http://v.hao123.com http://sy.hao123.com/qy/7993
        // 解析出0个url<验证:UnknownHostException: test.baidu.com http> http://test.baidu.com http://cbbs.baidu.com http://news.baidu.com/ns?cl=2&rn=20&tn=news&word=
        // 耗光内存 http://music.baidu.com/?uid=6DBDB269-FA83-0DAD-653E-B47F209FB1EB
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
        //cachedPool = Executors.newCachedThreadPool();
        cachedPool = Executors.newFixedThreadPool(20);
        UrlInfo urlInfo = new UrlInfo(initUrl,initUrl);
        int addRes = AddCrawlList(initUrl, urlInfo);
        if (1 != addRes){
            System.out.println(Thread.currentThread().getName()+" AddCrawlList 出错,errorCode="+addRes);
            return;
        }

        int num = 0;
        for (; num < MAX_NUM; num++) {
            try {
                //System.out.println(Thread.currentThread().getName() + " urlist.size="+urlist.size()+" 提取url...");
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

    public void endProgram(int num){
        try {
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName()+" bufferedWriter,fileWriter 关闭出错!");
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();
        System.out.println("num="+num);
        System.out.println("已爬取 " + crawlNum.get() + " 个,程序终止,执行时间:" + (endTime - startTime) / 1000.0 + "s"+
                " 最长connect时间= "+getInputStreamTime+" 最长读取网页时间= "+getPageTime+" 最长网页解析时间= "+parsePageTime+" 最长执行时间= "+EndTime+" errorNum = "+errorNum.get());
    }






    //crawlPage__________________________________________________________________________________________________________

    public void crawlPage(UrlInfo urlInfo) {
        //------------------------------------------------开始线程-------------------------------------

        long cStartTime = System.currentTimeMillis();
        String sUrl = urlInfo.getsUrl();
        synchronized (crawlNum) {
            crawlNum.addAndGet(1);
            System.out.println(Thread.currentThread().getName()+" 开始:爬取第"+crawlNum.get()+"个,获取URL对象..."+"url= "+sUrl);
            try {
                bufferedWriter.write(crawlNum.get() + "    " + Thread.currentThread().getName() + "    " + sUrl + "\n");
            } catch (IOException e) {
                System.out.println(Thread.currentThread().getName()+" 写文件错误,return!");
                e.printStackTrace();
                return;
            }

        }

        if (sUrl == null || sUrl.length() == 0 ||sUrl.equals("")){
            System.out.println(Thread.currentThread().getName()+" url == 0,return!");
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        URL url = null;// MalformedURLException
        try {
            url = new URL(sUrl);
        } catch (MalformedURLException e) {
            System.out.println(Thread.currentThread().getName()+" URL(sUrl) - MalformedURLException,return!");
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        //System.out.println(Thread.currentThread().getName()+" 开始:获取HttpURLConnection对象...");
        //建立连接
        HttpURLConnection connection = null;//IOException
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName()+" 161,url.openConnection() - IOException,return!");
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        //System.out.println(Thread.currentThread().getName()+" 成功:获取到HttpURLConnection对象...");
        //System.out.println(Thread.currentThread().getName()+" 开始:设置POST方式...");
        try {
            connection.setRequestMethod("POST");// ProtocolException
        } catch (ProtocolException e) {
            System.out.println(Thread.currentThread().getName()+" 160,setRequestMethod(\"POST\") -  ProtocolException,return!");
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        //System.out.println(Thread.currentThread().getName()+" 成功:设置POST方式成功...");

        connection.setDoOutput(true);
        connection.setDoInput(true);// 设置是否从httpUrlConnection读入，默认情况下是true;
        connection.setUseCaches(false);// Post 请求不能使用缓存
        connection.setRequestProperty("Accept-Encoding", "identity");//告诉服务器你的浏览器支持gzip解压 gzip,deflate
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Length","0");
        connection.setConnectTimeout(2*1000);
        connection.setReadTimeout(6*10*1000);

        long tcStart = System.currentTimeMillis();
        long tcEnd = 0;
        try {
            connection.connect();
            //int iState = connection.getResponseCode();
            //if (200 != iState) {
            //    synchronized (errorNum) {
            //        errorNum.addAndGet(1);
            //    }
            //    System.out.println(Thread.currentThread().getName()+" --------------------------------[-url] 响应码:"+iState+",return!");
            //    return;//只存储200 OK成功响应,其他无视
            //}
        } catch (IOException e) {
            tcEnd = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName()+"connectException,耗时: "+(tcEnd-tcStart));
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        tcEnd = System.currentTimeMillis();
        long tConn = tcEnd - tcStart;
        getInputStreamTime = tConn > getInputStreamTime?tConn:getInputStreamTime;
        System.out.println(Thread.currentThread().getName()+" 成功:完成connect 耗时: "+(tcEnd-tcStart));



//System.out.println(Thread.currentThread().getName()+" 开始:获取响应码...");

        long tSStart = System.currentTimeMillis();
        long tSEnd = 0;
        int iState = 0;
        try {
            iState = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (200 != iState) {
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            System.out.println(Thread.currentThread().getName()+" --------------------------------[-url] 响应码:"+iState+",return!");
            return;//只存储200 OK成功响应,其他无视
        }
        tSEnd = System.currentTimeMillis();
        long tState = tSEnd - tSStart;
        getInputStreamTime = tState > getInputStreamTime?tState:getInputStreamTime;
        System.out.println(Thread.currentThread().getName()+" 成功:完成getResponseCode 耗时: "+tState);





        //------------------------------------------------开始获取getInputStream()-------------------------------------

        long t1 = System.currentTimeMillis();
        long t2 = 0;
        InputStream in = null;//发送http请求,返回输入流,读取服务器返回信息
        //System.out.println(Thread.currentThread().getName()+" 开始:获取getInputStream()...");
        try {
            in = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }

            return;
        }
        t2=System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName()+" 成功:获取getInputStream(),耗时: "+(t2-t1));










        /*
        * 此处应有编码解码
        * */
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(in));
        String line;
        StringBuffer sHtml = new StringBuffer();// Read page into buffer.

        //------------------------------------------------开始读sHtml-------------------------------------------

        int lineNum = 0;
        System.out.println(Thread.currentThread().getName()+" 开始:读取网页...");
        long tReadStart = System.currentTimeMillis();
        try {
            while ((line = reader.readLine()) != null ) {// SocketTimeoutException
                sHtml.append(line + "\n");
            }
        } catch (IOException e) {
            //System.out.println(Thread.currentThread().getName()+" 217,readLine()() - IOException,return!");
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }
        long tReadEnd = System.currentTimeMillis();
        long t_Read = tReadEnd-tReadStart;
        //System.out.println(Thread.currentThread().getName()+" 成功:读取网页,耗时 "+t_Read);
        getPageTime = (t_Read > getPageTime) ? t_Read : getPageTime;

        try {
            reader.close();
            in.close();
        } catch (IOException e) {
            //System.out.println(Thread.currentThread().getName()+" 225,reader&in.close()() - IOException,return!");
            e.printStackTrace();
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            return;
        }

        //------------------------------------------------读完sHtml-------------------------------------------


        if (sHtml.length() != 0) {
            long cPageTime = System.currentTimeMillis();
            int len = connection.getContentLength();
            parsePage(new PageInfo(sUrl, sHtml.toString(), len));
            long cPageEndTime = System.currentTimeMillis();
            long parseTime = cPageEndTime-cPageTime;
            parsePageTime = (parseTime > parsePageTime)?parseTime:parsePageTime;
            //System.out.println(Thread.currentThread().getName()+" 解析网页耗时: "+parseTime);


        }else{
            synchronized (errorNum) {
                errorNum.addAndGet(1);
            }
            System.out.println("-------------"+Thread.currentThread().getName()+" sHtml.length() = 0,url="+sUrl);
            return;
        }
        long cEndTime = System.currentTimeMillis();
        long singeLongTime = cEndTime - cStartTime;
        EndTime = (singeLongTime > EndTime)?singeLongTime:EndTime;

        //------------------------------------------------end-------------------------------------------

        System.out.println(Thread.currentThread().getName()+" [End],time= "+(cEndTime-cStartTime)+" urlist.size="+urlist.size());
    }











    public int AddCrawlList(String absolutePath, UrlInfo urlInfo){
        /*
        * 1 正常返回
        * -1 put url 超时
        * -2 urlInfo == null
        * -3 url put interrupted
        * -4 sUrl = null
        * -5 mb过滤错误,已存在url
        * */
        if (urlInfo == null) return -2;
        String sUrl = checkUrl(absolutePath, urlInfo.getsUrl());
        if (sUrl != null) {
            synchronized (mb) {
                if (mb.add(sUrl)) {//bloom过滤器去重复url
                    try {
                        boolean isPut = urlist.offer(urlInfo, 1, TimeUnit.SECONDS);
                        if (!isPut) {
                            return -1;
                        }
                        return 1;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return -3;
                    }

                } else {
                    return -5;
                }
            }
        }else{
            System.out.println(Thread.currentThread().getName()+" url == null!");
            return -4;
        }
        //if (true){
        //    return 1;
        //}else {
        //    return 2;
        //}

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


    public void parsePage(PageInfo pageInfo){
        String sHtml = null;
        String sCrawlUrl = null;
        int urlNum = 0;
        try {
            sHtml = pageInfo.getsHtml();
            sCrawlUrl = pageInfo.getsUrl();
            String regex = "<a.*?/a>";//任意字符任意次,尽可能少的匹配 .*表示任意字符,?表示懒惰匹配
            Pattern pt = Pattern.compile(regex);
            Matcher mt = pt.matcher(sHtml);
            boolean isPut = false;
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
                        }else {
                            //判断size
                            UrlInfo urlInfo =  new UrlInfo(sCrawlUrl, urLast);
                            int addCode = AddCrawlList(sCrawlUrl,urlInfo);
                            if(1 != addCode){
                                if (-1 ==addCode){
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
            //synchronized (crawlNum) {
            //    System.out.println(Thread.currentThread().getName() + "    parse 爬取第" + crawlNum.get() + "个" +",获取url:"+urlNum+ "个, 当前爬取url为:" + sCrawlUrl);
            //    int len = pageInfo.getContentLen();
            //    try {
            //        bufferedWriter.write(crawlNum.get() + "    " + Thread.currentThread().getName() + "    " + sCrawlUrl + "     " + len + "B\n");
            //    } catch (IOException e) {
            //        e.printStackTrace();
            //        //System.exit(1);
            //    }
            //}

        }
        catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public String checkUrl(String absolutePath, String sUrl) {
        if (sUrl == null)
            return absolutePath;
        if (sUrl.contains("javascript:") || sUrl.contains("<%=") || sUrl.indexOf("'") == 0 || sUrl.length() <= ("https://".length() + 1))
            return null;
        if (!sUrl.contains("http://") && !sUrl.contains("https://")) {
            if (sUrl.indexOf("/") == 0 || sUrl.indexOf("../") == 0 || sUrl.indexOf("./") == 0 || absolutePath.lastIndexOf("/") == (absolutePath.length() -1)) {
                URL absoluteUrl, parseUrl;
                try {
                    absoluteUrl = new URL(absolutePath);
                    parseUrl = new URL(absoluteUrl, sUrl);
                    return parseUrl.toString();

                } catch (MalformedURLException e) {
                    //e.printStackTrace();
                    return null;
                }
            }
            else {
                return "http://" + sUrl;
            }
        }else {
            return sUrl;
        }
    }
}
