# Java多线程以及线程池



## **线程**

线程的概念，百度是这样解释的：

线程（英语：Thread）是操作系统能够进行运算调度的最小单位。它被包含在进程之中，是进程中的实际运作单位。一条线程指的是进程中一个单一顺序的控制流，一个进程中可以并发多个线程，每条线程并行执行不同的任务。在Unix System V及SunOS中也被称为轻量进程（Lightweight Processes），但轻量进程更多指内核线程（Kernel Thread），而把用户线程（User Thread）称为线程。



**1.1 线程与进程的区别**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9c1ea24a2b9941dba287a74a19ac1633~tplv-k3u1fbpfcp-watermark.image?)

进程：指在系统中正在运行的一个应用程序；程序一旦运行就是进程；进程——资源分配的最小单位。

线程：系统分配处理器时间资源的基本单元，或者说进程之内独立执行的一个单元执行流。线程——程序执行的最小单位。

也就是，进程可以包含多个线程，而线程是程序执行的最小单位。



**1.2 线程的状态**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8613b8ef21c7479d954b3244055db5cb~tplv-k3u1fbpfcp-watermark.image?)

- NEW：线程刚创建

- RUNNABLE: 在JVM中正在运行的线程，其中运行状态可以有运行中RUNNING和READY两种状态，由系统调度进行状态改变。

- BLOCKED：线程处于阻塞状态，等待监视锁，可以重新进行同步代码块中执行

- WAITING : 等待状态

- TIMED_WAITING: 调用sleep() join() wait()方法可能导致线程处于等待状态

- TERMINATED: 线程执行完毕，已经退出

  

**1.3 Notify和Wait ：**

Notify和Wait 的作用

首先看源码给出的解释，这里翻译了一下：

Notify：唤醒一个正在等待这个对象的线程监控。如果有任何线程正在等待这个对象，那么它们中的一个被选择被唤醒。选择是任意的，发生在执行的酌情权。一个线程等待一个对象通过调用一个{@code wait}方法进行监视。

Notify()需要在同步方法或同步块中调用，即在调用前，线程也必须获得该对象的对象级别锁

Wait：导致当前线程等待，直到另一个线程调用{@link java.lang.Object#notify()}方法或{@link java.lang.Object#notifyAll()}方法。



换句话说，这个方法的行为就像它简单一样执行调用{@code wait(0)}。当前线程必须拥有该对象的监视器。

线程释放此监视器的所有权，并等待另一个线程通知等待该对象的监视器的线程，唤醒通过调用{@code notify}方法或{@code notifyAll}方法。然后线程等待，直到它可以重新取得监视器的所有权，然后继续执行。

Wait()的作用是使当前执行代码的线程进行等待，它是Object类的方法，该方法用来将当前线程置入预执行队列中，并且在Wait所在的代码行处停止执行，直到接到通知或被中断为止。



在调用Wait方法之前，线程必须获得该对象的对象级别锁，即只能在同步方法或同步块中调用Wait方法。

Wait和Sleep的区别：

- 它们最大本质的区别是，Sleep()不释放同步锁，Wait()释放同步锁。

- 还有用法的上的不同是：Sleep(milliseconds)可以用时间指定来使他自动醒过来，如果时间不到你只能调用Interreput()来强行打断；Wait()可以用Notify()直接唤起。

- 这两个方法来自不同的类分别是Thread和Object

- 最主要是Sleep方法没有释放锁，而Wait方法释放了锁，使得其他线程可以使用同步控制块或者方法。

  

**1.4 Thread.sleep() 和Thread.yield()的异同**

- 相同 ：Sleep()和yield()都会释放CPU。

- 不同：Sleep()使当前线程进入停滞状态，所以执行Sleep()的线程在指定的时间内肯定不会执行；yield()只是使当前线程重新回到可执行状态，所以执行yield()的线程有可能在进入到可执行状态后马上又被执行。Sleep()可使优先级低的线程得到执行的机会，当然也可以让同优先级和高优先级的线程有执行的机会；yield()只能使同优先级的线程有执行的机会。

  

**1.5 补充：死锁的概念**

死锁：指两个或两个以上的进程（或线程）在执行过程中，因争夺资源而造成的一种互相等待的现象，若无外力作用，它们都将无法推进下去。此时称系统处于死锁状态或系统产生了死锁，这些永远在互相等待的进程称为死锁进程。

死锁产生的四个必要条件（缺一不可）：

- 互斥条件：顾名思义，线程对资源的访问是排他性，当该线程释放资源后下一线程才可进行占用。
- 请求和保持：简单来说就是自己拿的不放手又等待新的资源到手。线程T1至少已经保持了一个资源R1占用,但又提出对另一个资源R2请求，而此时，资源R2被其他线程T2占用，于是该线程T1也必须等待，但又对自己保持的资源R1不释放。
- 不可剥夺：在没有使用完资源时，其他线性不能进行剥夺。
- 循环等待：一直等待对方线程释放资源。

我们可以根据死锁的四个必要条件破坏死锁的形成。



**1.6 补充：并发和并行的区别**

并发：是指在某个时间段内，多任务交替的执行任务。当有多个线程在操作时，把CPU运行时间划分成若干个时间段,再将时间段分配给各个线程执行。在一个时间段的线程代码运行时，其它线程处于挂起状。

并行：是指同一时刻同时处理多任务的能力。当有多个线程在操作时，CPU同时处理这些线程请求的能力。

区别就在于CPU是否能同时处理所有任务，并发不能，并行能。



**1.7 补充：线程安全三要素**

- 原子性：Atomic包、CAS算法、Synchronized、Lock。

- 可见性：Synchronized、Volatile（不能保证原子性）。

- 有序性：Happens-before规则。

  

**1.8 补充：如何实现线程安全**

- 互斥同步：Synchronized、Lock。

- 非阻塞同步：CAS。

- 无需同步的方案：如果一个方法本来就不涉及共享数据，那它自然就无需任何同步操作去保证正确性。

  

**1.9 补充：保证线程安全的机制：**

- Synchronized关键字
- Lock
- CAS、原子变量
- ThreadLocl：简单来说就是让每个线程，对同一个变量，都有自己的独有副本，每个线程实际访问的对象都是自己的，自然也就不存在线程安全问题了。
- Volatile
- CopyOnWrite写时复制

随着CPU核心的增多以及互联网迅速发展，单线程的程序处理速度越来越跟不上发展速度和大数据量的增长速度，多线程应运而生，充分利用CPU资源的同时，极大提高了程序处理速度。



**创建线程的方法**

继承Thread类：

```
public class ThreadCreateTest {  
    public static void main(String\[\] args) {  
        new MyThread().start();  
    }  
}  

class MyThread extends Thread {  
    @Override  
    public void run() {  
        System.out.println(Thread.currentThread().getName() + "\\t" + Thread.currentThread().getId());  
    }  
}  
```

实现Runable接口：

```
public class RunableCreateTest {  
    public static void main(String\[\] args) {  
        MyRunnable runnable = new MyRunnable();  
        new Thread(runnable).start();  
    }  
}  

class MyRunnable implements Runnable {  
    @Override  
    public void run() {  
        System.out.println(Thread.currentThread().getName() + "\\t" + Thread.currentThread().getId());  
    }  
}  
```

通过Callable和Future创建线程：

```
public class CallableCreateTest {  
    public static void main(String\[\] args) throws Exception {  
         // 将Callable包装成FutureTask，FutureTask也是一种Runnable  
        MyCallable callable = new MyCallable();  
        FutureTask<Integer> futureTask = new FutureTask<>(callable);  
        new Thread(futureTask).start();  

        // get方法会阻塞调用的线程  
        Integer sum = futureTask.get();  
        System.out.println(Thread.currentThread().getName() + Thread.currentThread().getId() + "=" + sum);  
    }  
}  


class MyCallable implements Callable<Integer> {  

    @Override  
    public Integer call() throws Exception {  
        System.out.println(Thread.currentThread().getName() + "\\t" + Thread.currentThread().getId() + "\\t" + new Date() + " \\tstarting...");  

        int sum = 0;  
        for (int i = 0; i <= 100000; i++) {  
            sum += i;  
        }  
        Thread.sleep(5000);  

        System.out.println(Thread.currentThread().getName() + "\\t" + Thread.currentThread().getId() + "\\t" + new Date() + " \\tover...");  
        return sum;  
    }  
}  
```

线程池方式创建：

实现Runnable接口这种方式更受欢迎，因为这不需要继承Thread类。在应用设计中已经继承了别的对象的情况下，这需要多继承（而Java不支持多继承，但可以多实现啊），只能实现接口。同时，线程池也是非常高效的，很容易实现和使用。

实际开发中，阿里巴巴开发插件一直提倡使用线程池创建线程，原因在下方会解释，所以上面的代码我就只简写了一些Demo。



## **2.1 线程池创建线程**

线程池，顾名思义，线程存放的地方。和数据库连接池一样，存在的目的就是为了较少系统开销，主要由以下几个特点：

降低资源消耗。通过重复利用已创建的线程降低线程创建和销毁造成的消耗（主要）。

提高响应速度。当任务到达时，任务可以不需要等到线程创建就能立即执行。

提高线程的可管理性。线程是稀缺资源，如果无限制地创建，不仅会消耗系统资源，还会降低系统的稳定性。



Java提供四种线程池创建方式：

- newCachedThreadPool创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程。
- newFixedThreadPool创建一个定长线程池，可控制线程最大并发数，超出的线程会在队列中等待。
- newScheduledThreadPool创建一个定长线程池，支持定时及周期性任务执行。
- newSingleThreadExecutor创建一个单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序（FIFO, LIFO, 优先级）执行。

通过源码我们得知ThreadPoolExecutor继承自AbstractExecutorService，而AbstractExecutorService实现了ExecutorService。

```
public class ThreadPoolExecutor extends AbstractExecutorService  

public abstract class AbstractExecutorService implements ExecutorService  
```

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f6089926ed8a41c6a759c47eb7291970~tplv-k3u1fbpfcp-watermark.image?)


**2.2 ThreadPoolExecutor介绍** 

实际项目中，用的最多的就是ThreadPoolExecutor这个类，而《阿里巴巴Java开发手册》中强制线程池不允许使用Executors去创建，而是通过New ThreadPoolExecutor实例的方式，这样的处理方式让写的同学更加明确线程池的运行规则，规避资源耗尽的风险。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/797a76e396184950962f8e843f93368d~tplv-k3u1fbpfcp-watermark.image?)



我们从ThreadPoolExecutor入手多线程创建方式，先看一下线程池创建的最全参数。

```java
    public ThreadPoolExecutor(int corePoolSize,  
                              int maximumPoolSize,  
                              long keepAliveTime,  
                              TimeUnit unit,  
                              BlockingQueue<Runnable> workQueue,  
                              ThreadFactory threadFactory,  
                              RejectedExecutionHandler handler) {  
        if (corePoolSize < 0 ||  
            maximumPoolSize <= 0 ||  
            maximumPoolSize < corePoolSize ||  
            keepAliveTime < 0)  
            throw new IllegalArgumentException();  
        if (workQueue == null || threadFactory == null || handler == null)  
            throw new NullPointerException();  
        this.corePoolSize = corePoolSize;  
        this.maximumPoolSize = maximumPoolSize;  
        this.workQueue = workQueue;  
        this.keepAliveTime = unit.toNanos(keepAliveTime);  
        this.threadFactory = threadFactory;  
        this.handler = handler;  
    }  
```

参数说明如下：

- corePoolSize：线程池的核心线程数，即便线程池里没有任何任务，也会有corePoolSize个线程在候着等任务。

- maximumPoolSize：最大线程数，不管提交多少任务，线程池里最多工作线程数就是maximumPoolSize。

- keepAliveTime：线程的存活时间。当线程池里的线程数大于corePoolSize时，如果等了keepAliveTime时长还没有任务可执行，则线程退出。

- Unit：这个用来指定keepAliveTime的单位，比如秒：TimeUnit.SECONDS。

- BlockingQueue：一个阻塞队列，提交的任务将会被放到这个队列里。

- threadFactory：线程工厂，用来创建线程，主要是为了给线程起名字，默认工厂的线程名字：pool-1-thread-3。

- handler：拒绝策略，当线程池里线程被耗尽，且队列也满了的时候会调用。

  

**2.2.1BlockingQueue** 

对于BlockingQueue个人感觉还需要单独拿出来说一下。

BlockingQueue：阻塞队列，有先进先出（注重公平性）和先进后出（注重时效性）两种，常见的有两种阻塞队列：ArrayBlockingQueue和LinkedBlockingQueue

队列的数据结构大致如图：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0e1d4b127e104e42b72816f6307a32ef~tplv-k3u1fbpfcp-watermark.image?)

队列一端进入，一端输出。而当队列满时，阻塞。BlockingQueue核心方法：1. 放入数据put2. 获取数据take。常见的两种Queue：



**2.2.2 ArrayBlockingQueue**

基于数组实现，在ArrayBlockingQueue内部，维护了一个定长数组，以便缓存队列中的数据对象，这是一个常用的阻塞队列，除了一个定长数组外，ArrayBlockingQueue内部还保存着两个整形变量，分别标识着队列的头部和尾部在数组中的位置。

一段代码来验证一下：

```java
    package map;  

    import java.util.concurrent.*;  

    public class MyTestMap {  
        // 定义阻塞队列大小  
        private static final int maxSize = 5;  
        public static void main(String\[\] args){  
            ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(maxSize);  
            new Thread(new Productor(queue)).start();  
            new Thread(new Customer(queue)).start();  
        }  
    }  

    class Customer implements Runnable {  
        private BlockingQueue<Integer> queue;  
        Customer(BlockingQueue<Integer> queue) {  
            this.queue = queue;  
        }  

        @Override  
        public void run() {  
            this.cusume();  
        }  

        private void cusume() {  
            while (true) {  
                try {  
                    int count = (int) queue.take();  
                    System.out.println("customer正在消费第" + count + "个商品===");  
                    // 只是为了方便观察输出结果  
                    Thread.sleep(10);  
                } catch (InterruptedException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
    }  

    class Productor implements Runnable {  
        private BlockingQueue<Integer> queue;  
        private int count = 1;  
        Productor(BlockingQueue<Integer> queue) {  
            this.queue = queue;  
        }  

        @Override  
        public void run() {  
            this.product();  
        }  
        private void product() {  
            while (true) {  
                try {  
                    queue.put(count);  
                    System.out.println("生产者正在生产第" + count + "个商品");  
                    count++;  
                } catch (InterruptedException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
    }  

//输出如下  
/**  
生产者正在生产第1个商品  
生产者正在生产第2个商品  
生产者正在生产第3个商品  
生产者正在生产第4个商品  
生产者正在生产第5个商品  
customer正在消费第1个商品===  
*/  
```

**2.2.3 LinkedBlockingQueue**

基于链表的阻塞队列，内部也维护了一个数据缓冲队列。需要我们注意的是如果构造一个LinkedBlockingQueue对象，而没有指定其容量大小。

LinkedBlockingQueue会默认一个类似无限大小的容量（Integer.MAX_VALUE），这样的话，如果生产者的速度一旦大于消费者的速度，也许还没有等到队列满阻塞产生，系统内存就有可能已被消耗殆尽了。



**2.2.4 LinkedBlockingQueue和ArrayBlockingQueue的主要区别**

- ArrayBlockingQueue的初始化必须传入队列大小，LinkedBlockingQueue则可以不传入。

- ArrayBlockingQueue用一把锁控制并发，LinkedBlockingQueue俩把锁控制并发，锁的细粒度更细。即前者生产者消费者进出都是一把锁，后者生产者生产进入是一把锁，消费者消费是另一把锁。

- ArrayBlockingQueue采用数组的方式存取，LinkedBlockingQueue用Node链表方式存取。

  

**2.2.5handler拒绝策略**

Java提供了4种丢弃处理的方法，当然你也可以自己实现，主要是要实现接口：RejectedExecutionHandler中的方法。

- AbortPolicy：不处理，直接抛出异常。

- CallerRunsPolicy：只用调用者所在线程来运行任务，即提交任务的线程。

- DiscardOldestPolicy：LRU策略，丢弃队列里最近最久不使用的一个任务，并执行当前任务。

- DiscardPolicy：不处理，丢弃掉，不抛出异常。

  

**2.2.6线程池五种状态**

```
    private static final int RUNNING    = -1 << COUNT_BITS;  
    private static final int SHUTDOWN   =  0 << COUNT_BITS;  
    private static final int STOP       =  1 << COUNT_BITS;  
    private static final int TIDYING    =  2 << COUNT_BITS;  
    private static final int TERMINATED =  3 << COUNT_BITS;  
```

RUNNING：在这个状态的线程池能判断接受新提交的任务，并且也能处理阻塞队列中的任务。

SHUTDOWN：处于关闭的状态，该线程池不能接受新提交的任务，但是可以处理阻塞队列中已经保存的任务，在线程处于RUNNING状态，调用shutdown()方法能切换为该状态。

STOP：线程池处于该状态时既不能接受新的任务也不能处理阻塞队列中的任务，并且能中断现在线程中的任务。当线程处于RUNNING和SHUTDOWN状态，调用shutdownNow()方法就可以使线程变为该状态。



TIDYING：在SHUTDOWN状态下阻塞队列为空，且线程中的工作线程数量为0就会进入该状态，当在STOP状态下时，只要线程中的工作线程数量为0就会进入该状态。

TERMINATED：在TIDYING状态下调用terminated()方法就会进入该状态。可以认为该状态是最终的终止状态。

回到线程池创建ThreadPoolExecutor，我们了解了这些参数，再来看看ThreadPoolExecutor的内部工作原理：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dd21421d644d4bb7a0363967b9587b47~tplv-k3u1fbpfcp-watermark.image?)

- 判断核心线程是否已满，是进入队列，否：创建线程

- 判断等待队列是否已满，是：查看线程池是否已满，否：进入等待队列

- 查看线程池是否已满，是：拒绝，否创建线程

  

**2.3深入理解ThreadPoolExecutor**

进入Execute方法可以看到：

```java
  public void execute(Runnable command) {  
        if (command == null)  
            throw new NullPointerException();  
        int c = ctl.get();  
      //判断当前活跃线程数是否小于corePoolSize,如果小于，则调用addWorker创建线程执行任务  
        if (workerCountOf(c) < corePoolSize) {  
            if (addWorker(command, true))  
                return;  
            c = ctl.get();  
        }  
      //如果不小于corePoolSize，则将任务添加到workQueue队列。  
        if (isRunning(c) && workQueue.offer(command)) {  
            int recheck = ctl.get();  
            if (! isRunning(recheck) && remove(command))  
                reject(command);  
            else if (workerCountOf(recheck) == 0)  
                addWorker(null, false);  
        }  
      //如果放入workQueue失败，则创建线程执行任务，如果这时创建线程失败(当前线程数不小于maximumPoolSize时)，就会调用reject(内部调用handler)拒绝接受任务。  
        else if (!addWorker(command, false))  
            reject(command);  
    }  
```

AddWorker方法：

- 创建Worker对象，同时也会实例化一个Thread对象。在创建Worker时会调用threadFactory来创建一个线程。

- 然后启动这个线程。

  

**2.3.1线程池中CTL属性的作用是什么？**

看源码第一反应就是这个CTL到底是个什么东东？有啥用？一番研究得出如下结论：

CTL属性包含两个概念：

```java
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));  

    private static int ctlOf(int rs, int wc) { return rs | wc; }  
```

- runState：即rs 表明当前线程池的状态，是否处于Running，Shutdown，Stop，Tidying。
- workerCount：即wc表明当前有效的线程数。

我们点击workerCount即工作状态记录值，以RUNNING为例，RUNNING = -1 << COUNT_BITS;，即-1无符号左移COUNT_BITS位，进一步我们得知COUNT_BITS位29，因为Integer位数为31位（2的五次方减一）

```java
    private static final int COUNT_BITS = Integer.SIZE - 3;  
```

既然是29位那么就是Running的值为：

```
1110 0000 0000 0000 0000 0000 0000 0000   
|||  
31~29位  
```

那低28位呢，就是记录当前线程的总线数啦：

```java
    // Packing and unpacking ctl  
    private static int runStateOf(int c)     { return c & ~CAPACITY; }  
    private static int workerCountOf(int c)  { return c & CAPACITY; }  
    private static int ctlOf(int rs, int wc) { return rs | wc; }  
```

从上述代码可以看到workerCountOf这个函数传入ctl之后，是通过CTL&CAPACITY操作来获取当前运行线程总数的。

也就是RunningState|WorkCount&CAPACITY，算出来的就是低28位的值。因为CAPACITY得到的就是高3位（29-31位）位0，低28位（0-28位）都是1，所以得到的就是ctl中低28位的值。

而runStateOf这个方法的话，算的就是RunningState|WorkCount&CAPACITY，高3位的值，因为CAPACITY是CAPACITY的取反，所以得到的就是高3位（29-31位）为1，低28位（0-28位）为0，所以通过&运算后，所得到的值就是高3为的值。

简单来说就是ctl中是高3位作为状态值，低28位作为线程总数值来进行存储。



**2.3.2 shutdownNow和shutdown的区别**

看源码发现有两种近乎一样的方法，shutdownNow和shutdown，设计者这么设计自然是有它的道理，那么这两个方法的区别在哪呢？

- shutdown会把线程池的状态改为SHUTDOWN，而shutdownNow把当前线程池状态改为STOP。

- shutdown只会中断所有空闲的线程，而shutdownNow会中断所有的线程。

- shutdown返回方法为空，会将当前任务队列中的所有任务执行完毕；而shutdownNow把任务队列中的所有任务都取出来返回。

  

**2.3.3 线程复用原理**

```java
final void runWorker(Worker w) {  
        Thread wt = Thread.currentThread();  
        Runnable task = w.firstTask;  
        w.firstTask = null;  
        w.unlock(); // allow interrupts  
        boolean completedAbruptly = true;  
        try {  
            while (task != null || (task = getTask()) != null) {  
                w.lock();  
                // If pool is stopping, ensure thread is interrupted;  
                // if not, ensure thread is not interrupted.  This  
                // requires a recheck in second case to deal with  
                // shutdownNow race while clearing interrupt  
                if ((runStateAtLeast(ctl.get(), STOP) ||  
                     (Thread.interrupted() &&  
                      runStateAtLeast(ctl.get(), STOP))) &&  
                    !wt.isInterrupted())  
                    wt.interrupt();  
                try {  
                    beforeExecute(wt, task);  
                    Throwable thrown = null;  
                    try {  
                        task.run();  
                    } catch (RuntimeException x) {  
                        thrown = x; throw x;  
                    } catch (Error x) {  
                        thrown = x; throw x;  
                    } catch (Throwable x) {  
                        thrown = x; throw new Error(x);  
                    } finally {  
                        afterExecute(task, thrown);  
                    }  
                } finally {  
                    task = null;  
                    w.completedTasks++;  
                    w.unlock();  
                }  
            }  
            completedAbruptly = false;  
        } finally {  
            processWorkerExit(w, completedAbruptly);  
        }  
    }  
```

就是任务在并不只执行创建时指定的firstTask第一任务，还会从任务队列的中自己主动取任务执行，而且是有或者无时间限定的阻塞等待，以保证线程的存活。

默认的是不允许。



**2.4 CountDownLatch和CyclicBarrier区别**

countDownLatch是一个计数器，线程完成一个记录一个，计数器递减，只能只用一次。

CyclicBarrier的计数器更像一个阀门，需要所有线程都到达，然后继续执行，计数器递增，提供Reset功能，可以多次使用。



**3. 多线程间通信的几种方式**

提及多线程又不得不提及多线程通信的机制。首先，要短信线程间通信的模型有两种：共享内存和消息传递，以下方式都是基本这两种模型来实现的。我们来基本一道面试常见的题目来分析：

题目：有两个线程A、B，A线程向一个集合里面依次添加元素"abc"字符串，一共添加十次，当添加到第五次的时候，希望B线程能够收到A线程的通知，然后B线程执行相关的业务操作。



**3.1使用volatile关键字**

```java
package thread;  

/**  
 *   
 * @author hxz  
 * @description 多线程测试类  
 * @version 1.0  
 * @data 2020年2月15日 上午9:10:09  
 */  
public class MyThreadTest {  

    public static void main(String\[\] args) throws Exception {  

        notifyThreadWithVolatile();  

    }  

    /**  
     * 定义一个测试  
     */  
    private static volatile boolean flag = false;  
    /**  
     * 计算I++，当I==5时，通知线程B  
     * @throws Exception  
     */  
    private static void notifyThreadWithVolatile() throws Exception {  
        Thread thc = new Thread("线程A"){  
            @Override  
            public void run() {  
                for (int i = 0; i < 10; i++) {  
                    if (i == 5) {  
                        flag = true;  
                        try {  
                            Thread.sleep(500L);  
                        } catch (InterruptedException e) {  
                            // TODO Auto-generated catch block  
                            e.printStackTrace();  
                        }  
                        break;  
                    }  
                    System.out.println(Thread.currentThread().getName() + "====" + i);  
                }  
            }  
        };  

        Thread thd = new Thread("线程B") {  
            @Override  
            public void run() {  
                while (true) {  
                    // 防止伪唤醒 所以使用了while  
                    while (flag) {  
                        System.out.println(Thread.currentThread().getName() + "收到通知");  
                        System.out.println("do something");  
                        try {  
                            Thread.sleep(500L);  
                        } catch (InterruptedException e) {  
                            // TODO Auto-generated catch block  
                            e.printStackTrace();  
                        }  
                        return ;  
                    }  

                }  
            }  
        };  

        thd.start();  
        Thread.sleep(1000L);  
        thc.start();  

    }  
}  
```

个人认为这是基本上最好的通信方式，因为A发出通知B能够立马接受并Do Something。