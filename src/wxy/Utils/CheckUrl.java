package wxy.Utils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author delia
 * @create 2016-06-30 下午4:36
 */

public class CheckUrl {
    String absolutePath = "http://www.baidu.com/";
    public String checkUrl(String sUrl){
        if (sUrl.indexOf("javascript:") != -1)
            return null;
        if (sUrl.indexOf("http://") == -1){
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

    public static void main(String[] args) {
        CheckUrl usrl = new CheckUrl();
        System.out.println(usrl.checkUrl("/hh.html"));
        System.out.println();
    }
}
