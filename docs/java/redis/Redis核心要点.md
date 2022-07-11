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

