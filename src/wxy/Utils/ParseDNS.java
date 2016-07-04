package wxy.Utils;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Scanner;

/**
 * @author delia
 * @create 2016-06-29 下午3:19
 */

public class ParseDNS {
    public static void main(String args[]) throws Exception {
        InetAddress address = InetAddress.getByName("Delia-MacPro.local");// wxh-PC是我的计算机名
        System.out.println(address);
        System.out.println("-----");
        InetAddress address1 = InetAddress.getLocalHost();
        System.out.println(address1);

        InetAddress[] addresses = InetAddress.getAllByName("www.baidu.com");
        System.out.println(addresses.length);
        for (InetAddress addr : addresses) {
            System.out.println(addr);
            System.out.println("主机名字为:"+addr.getHostName());
            System.out.println("主机IP为:"+addr.getHostAddress());
        }



        //URL url = new URL("http://www.cnblogs.com/yzl-i/p/4442892.html");
        URL url = new URL("http://www.cnblogs.com/yzl-i/p/4442892.html");
        //建立连接
        URLConnection connection = url.openConnection();
        System.out.println("内容大小:" + connection.getContentLength());
        System.out.println("内容类型:" + connection.getContentType());
        System.out.println("内容编码:" + connection.getContentEncoding());
        System.out.println("获取时间: "+ new SimpleDateFormat("yyyy:mm:dd HH:mm:ss").format(connection.getDate()));
        Scanner in = new Scanner(connection.getInputStream());
        while (in.hasNextLine())
            System.out.println(in.nextLine());

        System.out.println("=================");
        Scanner innn = new Scanner(url.openStream());//下载网页源码
        while (in.hasNextLine())
            System.out.println(innn.nextLine());
    }
}