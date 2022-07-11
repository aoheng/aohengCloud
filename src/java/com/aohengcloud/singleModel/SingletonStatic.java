package java.com.aohengcloud.singleModel;

/**
 * @description: 单例模式-静态内部类【推荐使用】:避免了线程不安全，延迟加载，效率高
 * @author: Aoheng
 * @date: 2022/3/9 9:04
 */
public class SingletonStatic {

    private SingletonStatic() {}

    private static class Singleton{
        private static final SingletonStatic INSTANCE = new SingletonStatic();
    }

    public static SingletonStatic getInstance() {
        return Singleton.INSTANCE;
    }

}
