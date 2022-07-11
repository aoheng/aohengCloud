package java.com.aohengcloud.singleModel;

/**
 * @description: 单例模式-懒汉模式-双重校验:线程安全；延迟加载；效率较高
 * @author: Aoheng
 * @date: 2022/3/9 9:13
 */
public class Singleton {

    private static volatile Singleton instance;

    private Singleton() {

    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class){
                if (instance == null) {
                    instance=new Singleton();
                }
            }
        }
        return instance;
    }
}
