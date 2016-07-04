package wxy.funcions;

import wxy.Utils.MyBloomFilter;

import java.io.*;

/**
 * 海量字符串查找
 *
 * @author delia
 * @create 2016-06-24 上午11:07
 */

public class StrSearch {
    private MyBloomFilter mb = new MyBloomFilter();

    public static void main(String[] args) {
        StrSearch strSearch = new StrSearch();
        if (args.length != 4){
            System.out.println("输入参数个数为:" + args.length);
            System.out.println("请输入参数: ./strsearch, emailist.txt,checklist.txt,checkedresult.dat");
            return;
        }
        String emailistPath = "//Users/delia/Desktop/test/"+args[1];
        String strCheckPath = "//Users/delia/Desktop/test/"+args[2];
        String strResultPath = "//Users/delia/Desktop/test/"+args[3];

        strSearch.putStrIntoBloom(emailistPath);
        strSearch.checkStrFromBloom(strCheckPath,strResultPath);
        System.out.println("处理完毕!");
        //strSearch.nineNineMulitTable();
    }

    public void putStrIntoBloom(String filePath){
        FileReader reader = null;
        BufferedReader br = null;
        try {
            reader = new FileReader(filePath);
            br = new BufferedReader(reader);
            String str = null;
            while ((str = br.readLine()) != null) {
                //System.out.println(str);
                mb.add(str);
            }
            br.close();
            reader.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void checkStrFromBloom(String strCheckPath,String strResultPath){
        FileReader reader = null;
        BufferedReader br = null;
        try {
            reader = new FileReader(strCheckPath);
            br = new BufferedReader(reader);

            FileWriter writer = new FileWriter(strResultPath);
            BufferedWriter bw = new BufferedWriter(writer);

            String str = null;
            while ((str = br.readLine()) != null) {
                if (mb.exits(str)){
                    bw.write("true\n");
                }
                else{
                    bw.write("false\n");
                }
            }
            br.close();
            reader.close();

            bw.close();
            writer.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    //public void nineNineMulitTable(){
    //    for (int i = 1, j = 1;j <= 9; i++) {
    //        System.out.println(i + "*" + j + "=" + i*j +"");
    //        if (i == j){
    //            i = 0;
    //            j ++;
    //            System.out.println();
    //        }
    //    }
    //}
}
