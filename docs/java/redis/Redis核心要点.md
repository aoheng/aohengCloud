# 清除策略和淘汰策略



# 清除策略

- 事件删除
  - 每个键都有一个定时器，到期时触发处理事件，在事件中删除。

- 缺点是需要为每个key维护定时器，key的量大时，cpu消耗较大。

- 惰性删除
  - 每次访问时才检查，如果没过期，正常返回，否则删除该键并返回空。

- 定期删除
  - 每隔一段时间，检查所有设置了过期时间的key，删除已过期的键。

Redis采用后两种结合的方式

- 读写一个key时，触发惰性删除策略。

- 惰性删除策略不能及时处理冷数据，因此redis会定期主动淘汰一批已过期的key。

- 内存超过maxmemory时，触发主动清理（内存淘汰策略）。

# 淘汰策略

当maxmemory限制达到的时候，Redis会使用的行为由 Redis的`maxmemory-policy`配置指令来进行配置。

| 策略                          | 描述                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| volatile-lru                  | 从已设置过期时间的数据集中挑选最近***最少使用***的数据淘汰   |
| volatile-ttl                  | 从已设置过期时间的数据集中挑选**将要过期**的数据淘汰         |
| volatile-random               | 从已设置过期时间的数据集中**随机选择**数据淘汰               |
| allkeys-lru                   | 从所有数据集中挑选**最近最少使用**的数据淘汰                 |
| allkeys-random                | 从所有数据集中**随机选择**数据进行淘汰                       |
| noeviction                    | 禁止驱逐数据（返回错误）                                     |
| volatile-lfu（Redis 4.0引入） | 从**已设置过期时间**的数据集中通过统计访问频率，将**访问频率最少的键值对**淘汰 |
| allkeys-lfu（Redis 4.0引入）  | 从**所有数据集**中通过统计访问频率，将访问**频率最少的键值对**淘汰 |





# **缓存穿透**

 **描述：**

   缓存穿透是指缓存和数据库中都没有的数据，而用户不断发起请求，如发起为id为“-1”的数据或id为特别大不存在的数据。这时的用户很可能是攻击者，攻击会导致数据库压力过大。

  **解决方案：**

接口层增加校验，如用户鉴权校验，id做基础校验，id<=0的直接拦截；

从缓存取不到的数据，在数据库中也没有取到，这时也可以将key-value对写为key-null，缓存有效时间可以设置短点，如30秒（设置太长会导致正常情况也没法使用）。这样可以防止攻击用户反复用同一个id暴力攻击



# **缓存击穿**

   **描述：**

   缓存击穿是指缓存中没有但数据库中有的数据（一般是缓存时间到期），这时由于并发用户特别多，同时读缓存没读到数据，又同时去数据库去取数据，引起数据库压力瞬间增大，造成过大压力

   **解决方案：**

1. 设置热点数据永远不过期。
2. 加互斥锁

# **缓存雪崩**

**描述：**

缓存雪崩是指缓存中数据大批量到过期时间，而查询数据量巨大，引起数据库压力过大甚至down机。和缓存击穿不同的是，        缓存击穿指并发查同一条数据，缓存雪崩是不同数据都过期了，很多数据都查不到从而查数据库。

**解决方案：**

1.**缓存数据的过期时间设置随机**，防止同一时间大量数据过期现象发生。

如果缓存数据库是分布式部署，将热点数据均匀分布在不同搞得缓存数据库中。

2.**设置热点数据永远不过期。**





## redis获取缓存，实战示例：

```java
//key前缀
public static final String THROWS_CONFIG_PREFIX = "feedback:channelconfig:";
//公平锁
private static final Lock reenLock = new ReentrantLock(true);

/**
 * @param channel 获取渠道投放配置
 * @return
 */
@Override
public ThrowChannelConfig getByRedisOrDb(String channel) {
    if (StringUtils.isEmpty(channel)) {
        return null;
    }
    String redisKey = THROWS_CONFIG_PREFIX + channel;
    ThrowChannelConfig channelConfig = (ThrowChannelConfig) redisTemplate.opsForValue().get(redisKey);
    if (channelConfig != null) {
        return channelConfig;
    }
    //避免缓存穿透方案：1.（互斥锁）
    channelConfig = getData(channel);
    if (channelConfig == null) {
        return null;
    }
    //更新缓存，设置随机过期时间,避免缓存雪崩
    redisTemplate.opsForValue().set(redisKey, channelConfig, RandomUtils.nextInt(0, 100) + 60, TimeUnit.MINUTES);
    return channelConfig;
}

private ThrowChannelConfig getData(String channel) {
    //获取锁成功
    ThrowChannelConfig config = null;
    if (reenLock.tryLock()) {
        //从数据库读取
        config = channelConfigMapper.getOne(channel);
        //释放锁
        reenLock.unlock();
    } else {
        //线程休眠100ms重新获取数据
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            log.error("sleep-error:", e);
        }
        config = getData(channel);
    }
    return config;
}

```







# BigKey

通常我们会将含有较大数据或含有大量成员、列表数的Key称之为大Key，下面我们将用几个实际的例子对大Key的特征进行描述：

| 类型   | 大小    | 描述                                                         |
| ------ | ------- | ------------------------------------------------------------ |
| STRING | 5MB     | 字符串常用，注意value大小，5 兆字节(mb)=5120 千字节(kb)      |
| LIST   | 20000个 |                                                              |
| ZSET   | 20000个 |                                                              |
| HASH   | 100MB   | 它的成员数量虽然只有1000个但这些成员的value总大小为100MB（成员体积过大） |

需要注意的是，在以上的例子中，为了方便理解，我们对大Key的数据、成员、列表数给出了具体的数字。为了避免误导，在实际业务中，大Key的判定仍然需要根据Redis的实际使用场景、业务场景来进行综合判断。


#### **通过Redis官方客户端redis-cli的bigkeys参数发现大Key**

```XML
redis-cli -a yyP9lUkTOHiy3quI --bigkeys
```



# HotKey

在某个Key接收到的访问次数、显著高于其它Key时，我们可以将其称之为热Key，常见的热Key如：



- 某Redis实例的每秒总访问量为10000，而其中一个Key的每秒访问量达到了7000（访问次数显著高于其它Key）



- 对一个拥有上千个成员且总大小为1MB的HASH Key每秒发送大量的HGETALL（带宽占用显著高于其它Key）



- 对一个拥有数万个成员的ZSET Key每秒发送大量的ZRANGE（CPU时间占用显著高于其它Key）



#### **通过Redis官方客户端redis-cli的hotkeys参数发现热Key**

```XML
#需配置淘汰策略为LFU（最不经常使用）
redis-cli -a yyP9lUkTOHiy3quI --hotkeys
```



## 常用redis指令

```xml
#查看客户端列表
redis-cli -a yyP9lUkTOHiy3quI  client list

#查看memory信息
info memory

#db大小
dbsize

hlen big:hash(integer)
#哈希类型
hgetall big:hash

#字符串类型
strlen key

#Redis 4.0+，你就可以用scan + memory usage(pipeline)了
#memory usage  
memory usage big:hash

redis-cli -a yyP9lUkTOHiy3quI --bigkeys
#可找出bigkeys为VERNAMECHANNELKEY
get VERNAMECHANNELKEY

#需配置淘汰策略为LFU（最不经常使用）
redis-cli -a yyP9lUkTOHiy3quI --hotkeys

#查看内存逐出策略
config get maxmemory-policy
#设置内存淘汰策略：allkeys-lfu
config set maxmemory-policy allkeys-lfu



```



# **redis五大基本类型**

Redis是一个开源，内存存储的数据结构服务器，可用作数据库，高速缓存和消息队列代理。它支持5大基本类型+2种特殊类型

1. **字符串(String)、**
2. **哈希表(hash)、**
3. **列表(List)、**
4. **集合(Set)、**
5. **有序集合(sorted set)，**
6. **位图(bitmap)，**
7. **hyperloglogs**

等数据类型。内置复制、Lua脚本、LRU收回、事务以及不同级别磁盘持久化功能，同时通过Redis Sentinel提供高可用，通过Redis Cluster提供自动分区。

由于redis类型大家很熟悉，且网上命令使用介绍很多，下面重点介绍五大基本类型的底层数据结构与应用场景，以便当开发时，可以熟练使用redis。



# 应用场景

### 1、String（字符串）

```
1.String类型是redis的最基础的数据结构，也是最经常使用到的类型。  而且其他的四种类型多多少少都是在字符串类型的基础上构建的，所以String类型是redis的基础。2.String 类型的值最大能存储 512MB，这里的String类型可以是简单字符串、  复杂的xml/json的字符串、二进制图像或者音频的字符串、以及可以是数字的字符串
```

#### **应用场景**

1、缓存功能：String字符串是最常用的数据类型，不仅仅是redis，各个语言都是最基本类型，因此，利用redis作为缓存，配合其它数据库作为存储层，利用redis支持高并发的特点，可以大大加快系统的读写速度、以及降低后端数据库的压力。

2、计数器：许多系统都会使用redis作为系统的实时计数器，可以快速实现计数和查询的功能。而且最终的数据结果可以按照特定的时间落地到数据库或者其它存储介质当中进行永久保存。

3、统计多单位的数量：eg，uid：gongming  count：0  根据不同的uid更新count数量。

4、共享用户session：用户重新刷新一次界面，可能需要访问一下数据进行重新登录，或者访问页面缓存cookie，这两种方式做有一定弊端，1）每次都重新登录效率低下 2）cookie保存在客户端，有安全隐患。这时可以利用redis将用户的session集中管理，在这种模式只需要保证redis的高可用，每次用户session的更新和获取都可以快速完成。大大提高效率。



### 2、List（列表）

```
list类型是用来存储多个有序的字符串的，列表当中的每一个字符看做一个元素2.一个列表当中可以存储有一个或者多个元素，redis的list支持存储2^32次方-1个元素。3.redis可以从列表的两端进行插入（pubsh）和弹出（pop）元素，支持读取指定范围的元素集，  或者读取指定下标的元素等操作。redis列表是一种比较灵活的链表数据结构，它可以充当队列或者栈的角色。4.redis列表是链表型的数据结构，所以它的元素是有序的，而且列表内的元素是可以重复的。  意味着它可以根据链表的下标获取指定的元素和某个范围内的元素集。
```

#### **应用场景**

1、消息队列：reids的链表结构，可以轻松实现阻塞队列，可以使用左进右出的命令组成来完成队列的设计。比如：数据的生产者可以通过Lpush命令从左边插入数据，多个数据消费者，可以使用BRpop命令阻塞的“抢”列表尾部的数据。

2、文章列表或者数据分页展示的应用。比如，我们常用的博客网站的文章列表，当用户量越来越多时，而且每一个用户都有自己的文章列表，而且当文章多时，都需要分页展示，这时可以考虑使用redis的列表，列表不但有序同时还支持按照范围内获取元素，可以完美解决分页查询功能。大大提高查询效率。



### 3、Set（集合）

```
1.redis集合（set）类型和list列表类型类似，都可以用来存储多个字符串元素的集合。2.但是和list不同的是set集合当中不允许重复的元素。而且set集合当中元素是没有顺序的，不存在元素下标。3.redis的set类型是使用哈希表构造的，因此复杂度是O(1)，它支持集合内的增删改查，  并且支持多个集合间的交集、并集、差集操作。可以利用这些集合操作，解决程序开发过程当中很多数据集合间的问题。
```

**应用场景**

1、标签：比如我们博客网站常常使用到的兴趣标签，把一个个有着相同爱好，关注类似内容的用户利用一个标签把他们进行归并。

2、共同好友功能，共同喜好，或者可以引申到二度好友之类的扩展应用。

3、统计网站的独立IP。利用set集合当中元素不唯一性，可以快速实时统计访问网站的独立IP。

**数据结构**

set的底层结构相对复杂写，使用了intset和hashtable两种数据结构存储，intset可以理解为数组。



### 4、sorted set（有序集合）

redis有序集合也是集合类型的一部分，所以它保留了集合中元素不能重复的特性，但是不同的是，有序集合给每个元素多设置了一个分数。

```
redis有序集合也是集合类型的一部分，所以它保留了集合中元素不能重复的特性，但是不同的是，有序集合给每个元素多设置了一个分数，利用该分数作为排序的依据。
```

#### **应用场景**

1、 排行榜：有序集合经典使用场景。例如视频网站需要对用户上传的视频做排行榜，榜单维护可能是多方面：按照时间、按照播放量、按照获得的赞数等。

2、用Sorted Sets来做带权重的队列，比如普通消息的score为1，重要消息的score为2，然后工作线程可以选择按score的倒序来获取工作任务。让重要的任务优先执行。



### 5、hash（哈希）

```
  Redis hash数据结构 是一个键值对（key-value）集合,它是一个 string 类型的 field 和 value 的映射表，redis本身就是一个key-value型数据库，因此hash数据结构相当于在value中又套了一层key-value型数据。所以redis中hash数据结构特别适合存储关系型对象
```

#### **应用场景**

1、由于hash数据类型的key-value的特性，用来存储关系型数据库中表记录，是redis中哈希类型最常用的场景。一条记录作为一个key-value，把每列属性值对应成field-value存储在哈希表当中，然后通过key值来区分表当中的主键。

2、经常被用来存储用户相关信息。优化用户信息的获取，不需要重复从数据库当中读取，提高系统性能。





