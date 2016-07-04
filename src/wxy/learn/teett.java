package wxy.learn;

/**
 * @author delia
 * @create 2016-06-29 下午5:48
 *
 * 指向子类的父引用无法调用单纯属于子类的函数
 */
class Super{
    public void print(){
        System.out.println("Super A");
    }
}

class Sun extends Super{
    public void print(){
        System.out.println("sun B");
    }
    public void sunMethod(){
        System.out.println("sun method");
    }
}
public class teett {
    public static void main(String[] args) {
        //
        System.out.println("https://".length());
    }
}
