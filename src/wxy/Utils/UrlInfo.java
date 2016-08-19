package wxy.Utils;

/**
 * @author delia
 * @create 2016-08-12 下午12:48
 */

public class UrlInfo {
    String sCrawlUrl;
    String sUrl;

    public UrlInfo(String sCrawlUrl, String sUrl) {
        this.sCrawlUrl = sCrawlUrl;
        this.sUrl = sUrl;
    }

    public String getsCrawlUrl() {
        return sCrawlUrl;
    }

    public void setsCrawlUrl(String sCrawlUrl) {
        this.sCrawlUrl = sCrawlUrl;
    }

    public String getsUrl() {
        return sUrl;
    }

    public void setsUrl(String sUrl) {
        this.sUrl = sUrl;
    }
}
