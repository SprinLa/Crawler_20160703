package wxy.Utils;

import java.util.BitSet;

/**
 * 自定义布隆过滤器
 *
 * @author delia
 * @create 2016-06-21 上午10:17
 */

public class MyBloomFilter {
    //private double NUM_CON = Math.log(2);
    private int dataNum = 15000000;//待存储的字符串个数 1500w
    private int bitsLen;//需开辟的存储空间位数
    private int hashNum;//哈希函数的个数
    private double errorRate = 0.01;//期望的错误率
    private BitSet bs;//位数组

    public MyBloomFilter() {
        initParam(dataNum, errorRate);
    }

    public MyBloomFilter(int dataNum, double errorRate) {
        this.dataNum = dataNum;
        this.errorRate = errorRate;
        initParam(dataNum,errorRate);
    }

    private void initParam(int dataNum, double errorRate){
        double dTmp = dataNum*1.44*1.44*Math.log(1/errorRate);
        bitsLen = (int)Math.ceil(dTmp);
        hashNum = (int) Math.ceil(Math.log(2)*bitsLen/(double)dataNum);
        //System.out.println("待存储的字符串个数:"+dataNum+" 期望的错误率:"+errorRate+" 需开辟的存储空间位数:"+bitsLen
        //                   +"bit, "+bitsLen/1024/1024/8+"MB " +" 哈希函数的个数:"+hashNum);
        bs = new BitSet(bitsLen);
        //hashNum = 13;
        if (hashNum > 10){
            System.out.println("没有那么多的Hash函数,最多10个!");
            System.exit(0);
        }
    }

    public void add(String s){
        if (!exits(s)){
            int[] res = getHash(s);
            for (int i = 0; i < res.length; i++) {
                bs.set(res[i]);
            }
        }
    }

    public void set0(String s){
        if (exits(s)){
            int[] res = getHash(s);
            for (int i = 0; i < res.length; i++) {
                bs.clear(res[i]);
            }
        }
    }


    public boolean exits(String s){
        if (s == null || s.length() == 0){
            return false;
        }
        int[] res = getHash(s);
        for (int i = 0; i < res.length; i++) {
            if (!bs.get(res[i])){
                return false;
            }
        }
        return true;
    }

    public int[] getHash(String str){
        int[] resHash = new int[hashNum];
        for (int i = 0; i < hashNum; i++) {
            resHash[i] = HashAlgorithms.calHash(str,i);
        }
        return resHash;
    }

    //public static void main(String[] args) {
    //    MyBloomFilter mbf = new MyBloomFilter();
    //    JavaFile jFile = new JavaFile(mbf);
    //    jFile.readFile("//Users/delia/Desktop/email_addr.txt");
    //    System.out.println(mbf.exits("631857976@qq.com"));
    //    System.out.println(mbf.exits("actri631@126.com"));
    //    System.out.println(mbf.exits("631857975@qq.com"));
    //}
}
