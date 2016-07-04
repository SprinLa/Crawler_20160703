package wxy;

import wxy.Utils.MyBloomFilter;
import wxy.Utils.PageInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @author delia
 * @create 2016-06-28 下午7:55
 */

public class MyCrawler {
    private String sUrl = null;
    private int maxNum = 1;
    public static MyBloomFilter mb = new MyBloomFilter();
    public static List<String> toCrawlist = new ArrayList<String>();
    public static volatile long crawledNum = 0;
    public static List<PageInfo> pagesList = new ArrayList<PageInfo>();
    public static long resIndex = 1;
    private FileWriter writerResult = null;
    private BufferedWriter bwResult = null;
    private String filePath = null;
    private long startTime = 0,endTime = 0;
    ExecutorService cachedTPool_GetPage = null,cachedTPool_ParsePage = null;
    public MyCrawler(String sUrl, int maxNum,String filePath) {
        this.sUrl = sUrl;
        this.maxNum = maxNum;
        this.filePath = filePath;

    }

    public static void main(String[] args) {
        if (args.length != 3){
            System.out.println("请输入参数 url,maxNumOfUrl,fileName");//http://www.cnblogs.com/yzl-i/p/4442892.html
            return;
        }
        String sUrl = args[0];
        int maxNum = Integer.parseInt(args[1]);
        String fileName = args[2];
        String filetPath = "//Users/delia/Desktop/test/"+args[2];//urlResult.txt
        //MyCrawler mc = new MyCrawler(sUrl,maxNum);
        MyCrawler mc = new MyCrawler("http://www.baidu.com",maxNum,filetPath);
        mc.startTime=System.currentTimeMillis();
        mc.crawler();
    }

    //爬取方法
    public void crawler(){
        try {
            this.writerResult = new FileWriter(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.bwResult = new BufferedWriter(writerResult);
        AddCrawlist(sUrl,null);

        //Thread tGetPages = new Thread(new ThreadGetPages());//抓取网页线程
        //tGetPages.start();
        //Thread tParsePages = new Thread(new ThreadParsePages());//解析网页内容线程
        //tParsePages.start();

        startThreadPool();
    }
    public void startThreadPool(){
        cachedTPool_GetPage = Executors.newCachedThreadPool();
        cachedTPool_ParsePage = Executors.newCachedThreadPool();
        while (true) {
            if (toCrawlist.size()>0)
                cachedTPool_GetPage.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (toCrawlist.size() > 0) {//改成线程池后需要改成if
                            String sUrl = GetUrlFromCrawlList();
                            //System.out.println("爬取线程:当前爬取的URL为:" + sUrl);
                            crawlPage(sUrl);//抓取网页
                        }
                    }
                });
            if (pagesList.size()>0)
                cachedTPool_ParsePage.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (pagesList.size() > 0) {
                            PageInfo pageInfo = GetPagesFromPagesList();
                            //System.out.println("分析线程:当前分析的URL为:"+pageInfo.getsUrl());
                            analyzePage(pageInfo);//分析网页,提取url
                        }
                    }
                });
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    /**
     * 抓取页面线程
     *
     * @author delia
     * @create 2016-06-28 下午8:27
     */
    class ThreadGetPages implements Runnable {

        @Override
        public void run() {
            while (true) {
                while (toCrawlist.size() > 0) {//改成线程池后需要改成if
                    String sUrl = GetUrlFromCrawlList();
                    //System.out.println("爬取线程:当前爬取的URL为:" + sUrl);
                    crawlPage(sUrl);//抓取网页
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ThreadParsePages implements Runnable {
        @Override
        public void run() {
            while (true) {
                while (pagesList.size() > 0) {
                    PageInfo pageInfo = GetPagesFromPagesList();
                    //System.out.println("分析线程:当前分析的URL为:"+pageInfo.getsUrl());
                    analyzePage(pageInfo);//分析网页,提取url
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    public void crawlPage(String sUrl){
        if (crawledNum >= maxNum) {
            endTime = System.currentTimeMillis();
            System.out.println("已爬取 " + crawledNum + " 个,程序终止,执行时间:" + (endTime - startTime) + "ms");
            try {
                bwResult.close();
                writerResult.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
        //else{
        //    crawledNum ++;
        //    System.out.println("当前爬取第 "+crawledNum+" 个,Url为:"+sUrl);
        //}
        try {
            URL url=new URL(sUrl);
            //建立连接
            HttpURLConnection connection =(HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);//设置是否向httpUrlConnection输出，post请求,参数要放在http正文内要设true, 默认情况下是false;
            connection.setDoInput(true);// 设置是否从httpUrlConnection读入，默认情况下是true;
            connection.setUseCaches(false);// Post 请求不能使用缓存
            connection.setRequestProperty("Accept-Encoding", "identity");//告诉服务器你的浏览器支持gzip解压 gzip,deflate
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-type", "application/x-java-serialized-object");// 设定传送的内容类型是可序列化的java对象(如果不设此项,在传送序列化对象时,当WEB服务默认的不是这种类型时可能抛java.io.EOFException)
            //connecion.connect();//根据HttpURLConnectiony对象的配置值生成http头部信息
            try {
                OutputStream outStrm = connection.getOutputStream();
                ObjectOutputStream objOutputStrm = new ObjectOutputStream(outStrm);
                objOutputStrm.writeObject(new String(""));
                objOutputStrm.flush();
                objOutputStrm.close();
            }catch (Exception e){
                System.out.println("出错url:"+url);
                e.printStackTrace();
            }

            try {
                int iState = connection.getResponseCode();
                if (iState != 200) {
                    System.out.println("状态码: [" + iState + "],url为:" + sUrl);
                    return;//只存储200 OK成功响应,其他无视
                }
            }catch (Exception e){
                System.out.println("出错url:"+sUrl);
                e.printStackTrace();
            }
            bwResult.write(String.valueOf(resIndex++)+"    "+sUrl+"   "+connection.getContentLength()+" B\n");
            BufferedReader reader;
            //String charSet = getCharset(connection.getContentType());
            //System.out.println("内容编码:"+charSet);
            InputStream in = connection.getInputStream();
            String sContentEncoding = connection.getContentEncoding();
            if (sContentEncoding != null && sContentEncoding.length() != 0 && sContentEncoding.equals("gzip")){
                GZIPInputStream gzin = new GZIPInputStream(in);
                reader = new BufferedReader(new InputStreamReader(gzin));
            }
            else {
                //reader = new BufferedReader(new InputStreamReader(in, charSet));
                reader = new BufferedReader(new InputStreamReader(in));
            }
            String line;
            StringBuffer sHtml = new StringBuffer();// Read page into buffer.
            while ((line = reader.readLine()) != null) {
                sHtml.append(line+"\n");
            }
            reader.close();
            //System.out.println(sHtml.toString());
            synchronized (this){
                if(sHtml != null && sHtml.length() != 0)
                    pagesList.add(new PageInfo(sUrl,sHtml.toString()));
                //crawledNum ++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void analyzePage(PageInfo pageInfo){
        if (crawledNum >= maxNum) {
            endTime = System.currentTimeMillis();
            System.out.println("已爬取 " + crawledNum + " 个,程序终止,执行时间:" + (endTime - startTime) + "ms");
            try {
                bwResult.close();
                writerResult.close();
                cachedTPool_GetPage.shutdownNow();
                cachedTPool_ParsePage.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
        int iSum=0,invalidNum = 0;
        //System.out.println("当前解析网址:"+pageInfo.getsUrl());
        String sHtml = pageInfo.getsHtml();
        String regex ="<a.*?/a>";//任意字符任意次,尽可能少的匹配
        Pattern pt=Pattern.compile(regex);
        Matcher mt=pt.matcher(sHtml);
        while(mt.find())
        {
            //System.out.println(mt.group());
            Matcher title=Pattern.compile(">.*?</a>").matcher(mt.group());
            //Matcher myurl= Pattern.compile("href=.*?>").matcher(mt.group());
            Matcher myurl= Pattern.compile("href=[ ]*\".*?\"").matcher(mt.group());
            while(myurl.find())
            {
                iSum++;
                //System.out.println("截取:"+myurl.group());
                //System.out.println("网址:"+myurl.group().replaceAll("href=|>",""));
                String sUrl = myurl.group().replaceAll("href=|>","").trim();
                int len = sUrl.length();
                sUrl = sUrl.substring(1,len - 1).replaceAll("\\s*", "");;
                if (sUrl != null && sUrl.length() != 0){
                    if (checkUrl(pageInfo.getsUrl(),sUrl) == null){
                        invalidNum ++;
                    }
                    AddCrawlist(pageInfo.getsUrl(),sUrl);
                }
            }
        }
        crawledNum ++;
        System.out.println("当前爬取第 "+crawledNum+" 个,Url为:"+sUrl);
        //System.out.println("共有"+iSum+"个结果, "+invalidNum+" 个不合法");
        //System.out.println("待爬取的队列为: "+toCrawlist.size()+"个");
        //System.out.println();
    }
    public boolean AddCrawlist(String absolutePath,String reletivePath){//DNS解析
        String sUrl =  checkUrl(absolutePath,reletivePath);
        if (sUrl != null){
            if (!mb.exits(sUrl)){
                mb.add(sUrl);
                synchronized(this) {
                    toCrawlist.add(sUrl);
                    return true;
                }
            }else{
                //System.out.println("bloom 重了!"+sUrl);
            }
        }
        return false;
    }

    public String checkUrl(String absolutePath,String sUrl){
        if (sUrl == null)
            return absolutePath;
        if (sUrl.indexOf("javascript:") != -1 || sUrl.indexOf("'") == 0 || sUrl.length() <= ("https://".length()+1))
            return null;
        if (sUrl.indexOf("http://") == -1 && sUrl.indexOf("https://") == -1){
            if(sUrl.indexOf("/") == 0 || sUrl.indexOf("../") == 0 || sUrl.indexOf("./") == 0){
                URL absoluteUrl, parseUrl;
                try {
                    absoluteUrl = new URL(absolutePath);
                    parseUrl = new URL(absoluteUrl, sUrl);
                    return parseUrl.toString();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }else{
                return "http://"+sUrl;
            }
        }
        return sUrl;

    }

    public synchronized String  GetUrlFromCrawlList(){//DNS解析
        if (toCrawlist.size() <= 0) {
            System.out.println("待爬取url队列 toCrawlist 为空!");
            return null;
        }
        String sUrl = toCrawlist.iterator().next();
        toCrawlist.remove(sUrl);
        return sUrl;
    }
    public synchronized PageInfo  GetPagesFromPagesList(){//网页内容解析
        if (pagesList.size() <= 0) {
            System.out.println("待解析网页队列 pagesList 为空!");
            return null;
        }
        PageInfo pageInfp = pagesList.iterator().next();
        //System.out.println("pagesList remove");
        pagesList.remove(pageInfp);
        //crawledNum ++;
        return pageInfp;
    }

    public void getHeader(HttpURLConnection connection){
        System.out.println("内容大小:"+connection.getContentLength()/1024+" KB");
        System.out.println("内容类型:"+connection.getContentType());
        System.out.println("内容编码:" + connection.getContentEncoding());
        System.out.println();
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        Set<Entry<String, List<String>>> entrySet = headerFields.entrySet();
        Iterator<Entry<String, List<String>>> iterator = entrySet.iterator();
        while(iterator.hasNext()) {
            Entry<String, List<String>> next = iterator.next();
            String key=next.getKey();
            List<String> value = next.getValue();
            if(key==null)
                System.out.println(value.toString());
            else {
                if (key.equals("Content-Encoding")){
                    if (value.toString().equals("[gzip]")){
                        System.out.println("网页采用了gzip压缩!");
                    }
                }
                System.out.println(key + ":" + value.toString());
            }
        }
        System.out.println();
    }

    String getCharset(String contenType){
        Pattern pattern = Pattern.compile("charset=.*");
        Matcher matcher = pattern.matcher(contenType);
        if (matcher.find())
            return matcher.group(0).split("charset=")[1];
        return "utf-8";
    }


}
