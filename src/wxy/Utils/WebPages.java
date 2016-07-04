package wxy.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author delia
 * @create 2016-06-29 上午10:52
 */

public class WebPages {

    public static void main(String[] args) {
        try {
            //URL url = new URL("http://www.cnblogs.com/yzl-i/p/4442892.html");
            //URL url = new URL("http://www.baidu.com");
            ////建立连接
            //URLConnection connection = url.openConnection();
            //connection.setRequestProperty("Accept-Encoding", "identity");
            //System.out.println("内容大小:" + connection.getContentLength());
            //System.out.println("内容类型:" + connection.getContentType());
            //System.out.println("内容编码:" + connection.getContentEncoding());
            //System.out.println("获取时间: "+ new SimpleDateFormat("yyyy:mm:dd HH:mm:ss").format(connection.getDate()));

            //System.out.println("=================");
            //Scanner in = new Scanner(url.openStream());//下载网页源码
            //while (in.hasNextLine())
            //    System.out.println(in.nextLine());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void analyzePage(String sHtml){
        int i=0;
        String regex ="<a.*?/a>";//任意字符任意次,尽可能少的匹配
        Pattern pt=Pattern.compile(regex);
        Matcher mt=pt.matcher(sHtml);
        while(mt.find())
        {
            System.out.println(mt.group());
            i++;
            Matcher title=Pattern.compile(">.*?</a>").matcher(mt.group());
            while(title.find())
            {//获取标题
                System.out.println("标题:"+title.group().replaceAll(">|</a>",""));
            }
            //Matcher myurl= Pattern.compile("href=.*?>").matcher(mt.group());
            Matcher myurl= Pattern.compile("href=[ ]*\".*?\"").matcher(mt.group());
            while(myurl.find())
            {//获取网址
                System.out.println("href:"+myurl.group());
                System.out.println("网址:"+myurl.group().replaceAll("href=|>",""));
                String sUrl = myurl.group().replaceAll("href=|>","").trim();
            }
            System.out.println();
        }
        System.out.println("共有"+i+"个符合结果");
    }

    public void analyzePage1(String sHtml){
        int i=0;
        String regex ="href[ ]*=[ ]*\".*?\"|href[ ]*=[ ]*\'.*?\'";//任意字符任意次,尽可能少的匹配
        //String regex ="href=\'.*?\'";//任意字符任意次,尽可能少的匹配
        Pattern pt=Pattern.compile(regex);
        Matcher mt=pt.matcher(sHtml);
        while(mt.find())
        {
            System.out.println(mt.group());
            String sUrl = mt.group().replaceAll("href=|>","").trim();
            if (sUrl.indexOf("javascript:void(0)") != -1 && sUrl.equals("#")){
                i ++;
            }
            System.out.println("网址:"+sUrl);
            System.out.println();

        }
        System.out.println("共有"+i+"个符合结果");

        String sUrl = null;//解析网页中的url
    }
}
