# Redis分布式锁实现（使用Redisson）

Redisson Gibhub地址-> https://github.com/redisson/redisson

自己实现redis分布式锁，高并发情况下可能会有出现非原子性操作问题，所以可以通过redisson这个库来实现，redisson通过lua脚本来实现，保证每次操作都是原子性的。



Redisson tryLock的lua脚本

```Ruby
if (redis.call('exists', KEYS[1]) == 0) then redis.call('hincrby', KEYS[1], ARGV[2], 1); redis.call('pexpire', KEYS[1], ARGV[1]); return nil; end; if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then redis.call('hincrby', KEYS[1], ARGV[2], 1); redis.call('pexpire', KEYS[1], ARGV[1]); return nil; end; return redis.call('pttl', KEYS[1]);
```

unlock lua脚本

```Ruby
if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then return nil;end; local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); if (counter > 0) then redis.call('pexpire', KEYS[1], ARGV[2]); return 0; else redis.call('del', KEYS[1]); redis.call('publish', KEYS[2], ARGV[1]); return 1; end; return nil;
```

### 1、引入redisson到pom

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.13.1</version>
</dependency>
```

或

```xml
<dependency> 
    <groupId>org.redisson</groupId> 
    <artifactId>redisson</artifactId> 
    <version>3.14.0</version> 
</dependency> 
```

*推荐使用starter的配置方式



### 2、配置RedissonClient Bean

```java
@Bean
public RedissonClient redissonClient() {
   Config config = new Config();
   String address = "redis://" + properties.getHost() + ":" + properties.getPort();
   config.useSingleServer()
         .setAddress(address)
         .setPassword(properties.getPassword());
   RedissonClient redisson = Redisson.create(config);

   return redisson;
}
```



### 3、使用锁

```java
@Autowired
private RedissonClient redissonClient;

// 获取锁
RLock lock = redissonClient.getLock(lockKey);
if(!lock.tryLock(expire_time,TimeUnit.Seconds)){
   return "lock fail"
}
// lock successful
try{
  // 业务代码
}
catch()
finally{
  // 释放锁
  lock.unlock()
}
```

### 4、连接配置

如果使用的是apollo的配置，在项目application.properties添加mobile.redis配置即可

```XML
apollo.bootstrap.namespaces = application,mobile.rabbitmq,mobile.redis
```

非Apollo配置方式

```xml
spring.redis.host = 139.159.183.48
spring.redis.port = 6379
spring.redis.password = FBZ75m6HzsDVz19C
```
