# ELK接入集成方案

## ELK原理

### logstash采集

logstash搜集日志，进行过滤处理，并将 数据发送到elasticsearch的架构图如下：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6206efcbe99b49b48d15b3cfe013fb2b~tplv-k3u1fbpfcp-zoom-1.image)
Logstash事件处理有三个阶段：inputs → filters → outputs。是一个接收，处理，转发日志的工具

### FileBeat采集

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f9b5abe7bd214c96ab65ad5cec1fa9e0~tplv-k3u1fbpfcp-watermark.image?)


### docker-compose.yml

版本：7.16.2

```yaml
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.16.2
    container_name: elasticsearch
    volumes:
      - /root/elk/plugins:/usr/share/elasticsearch/plugins #插件文件挂载
      - /root/elk/data:/usr/share/elasticsearch/data #数据文件挂载
      - /root/elk/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml #配置文件挂载
    environment:
      - "cluster.name=elasticsearch" #设置集群名称为elasticsearch
      - "discovery.type=single-node" #以单一节点模式启动
      - "ES_JAVA_OPTS=-Xms256m -Xmx512m" #设置使用jvm内存大小
      - "ingest.geoip.downloader.enabled=false" # (Dynamic, Boolean) If true, Elasticsearch automatically downloads and manages updates for GeoIP2 databases from the ingest.geoip.downloader.endpoint. If false, Elasticsearch does not download updates and deletes all downloaded databases. Defaults to true.
    ports:
      - 9200:9200
  logstash:
    image: docker.elastic.co/logstash/logstash:7.16.2
    container_name: logstash
    volumes:
      - /root/elk/logstash.conf:/usr/share/logstash/pipeline/logstash.conf #挂载logstash的配置文件
    depends_on:
      - elasticsearch #kibana在elasticsearch启动之后再启动
    links:
      - elasticsearch:elasticsearch #可以用es这个域名访问elasticsearch服务
    ports:
      - 9600:9600
      - 5044:5044
  kibana:
    image: docker.elastic.co/kibana/kibana:7.16.2
    container_name: kibana
    depends_on:
      - elasticsearch #kibana在elasticsearch启动之后再启动
    links:
      - elasticsearch:es #可以用es这个域名访问elasticsearch服务
    environment:
      - "elasticsearch.hosts=http://elasticsearch:9200" #设置访问elasticsearch的地址
    ports:
      - 5601:5601


```

### elasticsearch.yml

```yaml
network.host: 0.0.0.0  #使用的网络
http.cors.enabled: true #跨域配置
http.cors.allow-origin: "*"
xpack.security.enabled: false #开启密码配置,xpack认证机制
xpack.security.transport.ssl.enabled: false
#开启ca证书认证,生产环境开启
#xpack.security.transport.ssl.verification_mode: certificate
#xpack.security.transport.ssl.client_authentication: required
#xpack.security.transport.ssl.keystore.path: certs/elastic-certificates.p12
#xpack.security.transport.ssl.truststore.path: certs/elastic-certificates.p12

```

### kibana.yml

```yaml
server:
  host: "0.0.0.0"
  port: 5601

# ES
elasticsearch:
  hosts: [ "elasticsearch:9200" ]
  username: "kibana_system"
  #password: "gaUWlRfIIQ27kaNzYJv6"  #若es开启密码配置,把这个打开

```

### logstash.conf

```
input {
  tcp {
    mode => "server"
    host => "0.0.0.0"
    port => 5044
    tags => ["tags"]
    codec => json_lines
  }
}
output {
  elasticsearch {
    hosts => "elasticsearch:9200"
    index => "%{[applicationName]}-%{+YYYY.MM.dd}"
    user => "elastic" #用户名
    #password => "5kzmClP3G4S8fx3oXQ4C"   #若es配置了密码 把这个打开
  }
}

```

## springboot项目接入

### maven配置pom.xml

```xml

<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.0</version>
</dependency>
<dependency>
<groupId>ch.qos.logback</groupId>
<artifactId>logback-classic</artifactId>
<version>1.2.8</version>
</dependency>
<dependency>
<groupId>ch.qos.logback</groupId>
<artifactId>logback-core</artifactId>
<version>1.2.8</version>
</dependency>
        <!--若配置skywalking需引入 -->
<dependency>
<groupId>org.apache.skywalking</groupId>
<artifactId>apm-toolkit-logback-1.x</artifactId>
<version>8.7.0</version>
</dependency>


```

### logback-spring.xml

```xml
<!--定义变量name的值是变量的名称，value的值时变量定义的值。通过定义的值会被插入到logger上下文中。定义变量后，可以使“${}”来使用变量。 -->
<springProperty name="APP_NAME" scope="context" source="spring.application.name" defaultValue="consult-service"/>
<springProperty name="LOG_ENV" scope="context" source="logging.env" defaultValue="uat"/>
<springProperty name="LOG_STASH_URL" scope="context" source="logging.stash.url" defaultValue="127.0.0.1:5044"/>


        <!--spring.profiles.active=uat,pro,logstash支持多环境logstash日志启用，作为可自定义开关-->
<springProfile name="uat,pro,logstash">
<appender name="LOG_STASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>${LOG_STASH_URL}</destination>
    <!-- 日志输出编码 -->
    <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder">
        <!--配置skywalking链路追踪 -->
        <provider class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.logstash.TraceIdJsonProvider"/>
        <customFields>{"applicationName":"${APP_NAME}-${LOG_ENV}"}</customFields>
    </encoder>
</appender>
</springProfile>


<root level="${LOG_LEVEL}">
<appender-ref ref="CONSOLE"/>
<springProfile name="uat,pro,logstash">
    <appender-ref ref="LOG_STASH"/>
</springProfile>
</root>


```

### nacos配置

服务***.yml

```yaml
#logstash生效开关
spring:
  profiles:
    active: logstash

#logstash配置
logging:
  stash:
    url: 192.168.1.134:5044  #logstash地址
  env: dev-134               #logstash日志环境变量，便于区分不同环境的日志，作为隔离环境变量


```

## kibana使用指南

### **新建索引**

步骤：**Management->Stack Management->Kibana->Index pattern->Create index pattern**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/414d6e0ca09f4378a67c14b96ff7f19b~tplv-k3u1fbpfcp-watermark.image?)

大功告成！索引新建成功

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/db6a28780674455792443bd137daa98b~tplv-k3u1fbpfcp-watermark.image?)

### 查询日志Discover

根据索引下标查询日志：Analytics->Discover

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a7ab6a75b52c41f9989deaba61fa10d1~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/583df6602d2b47b98b70b51d88238b5d~tplv-k3u1fbpfcp-watermark.image?)

以上为简单使用操作，kibana功能比较强大，支持多种场景日志查询以及分析，等待你去深入挖掘......

具体操作以及更多文档可参考[elastic官网](https://www.elastic.co/cn/)

## nginx反向代理

待补充

