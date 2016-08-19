package wxy.Utils;

import java.util.BitSet;

/**
 * 自定义布隆过滤器
 *
 * @author delia
 * @create 2016-06-21 上午10:17
 */

public class CBloomFilter {
    private static final int bitsLen = 2 << 26;//需开辟的存储空间个数 1500万时,143239214
    private static BitSet bs = new BitSet(bitsLen);//位数组
    private static final int[] seeds = {3,5,7, 11, 13, 31, 37, 61};//这里要选取质数，能很好的降低错误率
    private static final int hashNum = seeds.length;//哈希函数的个数
    private int[] resHash = new int[seeds.length];

    public boolean add(String s){
        if (!exits(s)){
            int[] res = getHash(s);
            for (int i = 0; i < res.length; i++) {
                    bs.set(res[i]);
            }
            return true;
        }else{
            return false;
        }
    }

    //public void set0(String s){
    //    if (exits(s)){
    //        int[] res = getHash(s);
    //        for (int i = 0; i < res.length; i++) {
    //            bs.clear(res[i]);
    //        }
    //    }
    //}


    public boolean exits(String s){
        if (s == null){
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
        for (int i = 0; i < hashNum; i++) {
            resHash[i] = hash(bitsLen, seeds[i],str);// % bitsLen
            //System.out.println("*************resHash[i]="+resHash[i]+" bitsLen="+bitsLen+" "+(resHash[i] > bitsLen));
        }
        return resHash;
    }

    private int hash(int cap, int seed,String value) {//字符串哈希，选取好的哈希函数很重要
        int result = 0;
        int len = value.length();
        for (int i = 0; i < len; i++) {
            result = seed * result + value.charAt(i);
        }
        return (cap - 1) & result;
    }

    //public static void main(String[] args) {
    //    MyBloomFilter mbf = new MyBloomFilter();
    //    //mbf.add("hello!");
    //    //mbf.add("631857976@qq.com");
    //    //mbf.add("actri631@126.com");
    //    //
    //    //System.out.println(mbf.exits("631857976@qq.com"));
    //    //System.out.println(mbf.exits("actri631@126.com"));
    //    //System.out.println(mbf.exits("631857975@qq.com"));
    //    //System.out.println(mbf.exits("hello!"));
    //    //System.out.println(mbf.add("hello!"));
    //}
}
