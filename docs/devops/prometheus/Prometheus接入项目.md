# 项目接入Prometheus

## Prometheus 监控介绍

Prometheus文章：https://www.ibm.com/developerworks/cn/java/j-using-micrometer-to-record-java-metric/index.html

## 1：添加依赖

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```



## 2：添加配置

```xml
management.endpoints.web.exposure.include = prometheus
management.endpoints.web.exposure.exclude = env,info,beans,jolokia,logfile,health,loggers,liquibase,auditevents,conditions,configprops,flyway,sessions,threaddump,scheduledtasks,metrics,mappings,httptrace,jvm,caches,heapdump,integrationgraph
management.endpoints.jmx.exposure.exclude= *

```

include = prometheus 用于自定义指标的监控

### 配置对照表

| 名称             | 描述                                             | 默认开启 |
| ---------------- | ------------------------------------------------ | -------- |
| auditevents      | 公开审计事件信息                                 | Yes      |
| beans            | 列出所有Spring管理的的Beans                      | Yes      |
| caches           | 公开可用缓存                                     | Yes      |
| conditions       | 展示所有自动配置的条件，并显示匹配或不匹配的原因 | Yes      |
| configprops      | 展示@ConfigurationProperties列表                 | Yes      |
| env              | 公开ConfigurableEnvironment下的属性              | Yes      |
| flyway           | 展示由Flyway管理的数据库脚本                     | Yes      |
| health           | 展示程序的健康信息                               | Yes      |
| httptrace        | 展示HTTP轨迹信息，默认显示最后100次              | Yes      |
| info             | 展示应用程序信息                                 | Yes      |
| integrationgraph | 显示Spring集成视图                               | Yes      |
| loggers          | 显示和修改日志配置文件                           | Yes      |
| liquibase        | 显示由Liquibase管理的数据库脚本                  | Yes      |
| metrics          | 显示指标信息                                     | Yes      |
| mappings         | 显示所有被@RequestMapping拦截的路径              | Yes      |
| scheduledtasks   | 显示定时任务                                     | Yes      |
| sessions         | 可以检索或删除会话（Spring管理的会话）           | Yes      |
| shutdown         | 发送优雅关闭指令                                 | No       |
| threaddump       | 下载线程dump文件                                 | Yes      |
| heapdump         | 下载堆dump文件(只在Web应用下有效)                | Yes      |
| jolokia          | 通过HTTP公开JMX Beans(只在Web应用下有效)         | Yes      |
| logfile          | 返回日志文件内容(只在Web应用下有效)              | Yes      |
| prometheus       | 以Prometheus服务格式公开指标(只在Web应用下有效)  | Yes      |



## 3：发布k8s添加监控路径

如图：需要调整在spec.template.metadata 下面添加如下配置

```yaml
annotations:
  prometheus.io/scrape: 'true'
  prometheus.io/port: '9189'  #容器端口
  prometheus.io/path: '/actuator/prometheus'  #prometheus监控路径，固定即可
```

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a93188ba79094124b5e0414509d6fbe3~tplv-k3u1fbpfcp-watermark.image?)



## 4：按需要自建指标

示例如下：

1：添加TimedAspect(计时器)和CountedAspect(计数器)的Bean
用来通过Aop的方式注入函数

```java
@Configuration
public class PrometheusAutoConfiguration {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry){
        return new TimedAspect(meterRegistry);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry){
        return new CountedAspect(meterRegistry);
    }
}
```

2：调整具体要监控的方法
比如下面的process方法，添加了一个计时器，和一个调用计数器，一个调用失败计数器

```java
@Autowired
MeterRegistry meterRegistry;
Counter errorCounter;
//CounterName 自行定义对应的名称
@PostConstruct
private void init() {
    errorCounter = meterRegistry.counter( CounterName + "_Error_Counter");
}

@Timed(value = CounterName +"_Timed")
@Counted(value = CounterName +"_Counted")
public void process() throws Exception {
    boolean isSuccess = false;
    try {
       dosome()；
    } catch (Exception ex) {
        log.error("process failed,message: {}", ex);
        errorCounter.increment();
    }
}
```



## grafana查看监控

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7352492838f644e1b60027712a3a1ed8~tplv-k3u1fbpfcp-watermark.image?)
