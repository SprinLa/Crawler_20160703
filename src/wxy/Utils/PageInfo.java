package wxy.Utils;

/**
 * @author delia
 * @create 2016-06-30 下午4:23
 */

public class PageInfo {
    String sUrl;
    String sHtml;

    public PageInfo(String sUrl, String sHtml) {
        this.sUrl = sUrl;
        this.sHtml = sHtml;
    }

    public String getsUrl() {
        return sUrl;
    }

    public void setsUrl(String sUrl) {
        this.sUrl = sUrl;
    }

    public String getsHtml() {
        return sHtml;
    }

    public void setsHtml(String sHtml) {
        this.sHtml = sHtml;
    }
}
