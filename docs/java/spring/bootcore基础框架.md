# bootcore 1.0.0基础框架

# 简要说明

bootcore 1.0.0 基于 spring boot 2.2.5.RELEASE 版本。

1. 模块分类

- 框架主要分为三类模块，一类为基础模块，一类为依赖管理类模块（starters），一类为自动配置模块。

- 基础模块主要实现工具类、公用类、公用模块等封装。

- 自动配置模块主要用于简化项目配置。

- starter模块主要用于简化项目依赖和封装外部依赖，比如bootcore-cloud-starter-service-registry模块当前版本内部使用eureka-client， 将该引用进行封装，以便日后进行更换，在有需要时也可在starter模块中增加相关模块的通用扩展代码。

1. 使用说明

- 在提供了starter的情况下，外部项目使用starter进行引用，简化项目依赖管理，并隐藏外部依赖细节，为将来可能出现的变更提供最大的灵活性。

- bootcore-samples 文件夹下提供部分使用示例。

# 模块说明

## 顶层pom

### bootcore-dependencies

依赖管理pom模块，是基础框架项目中所有项目的顶层pom。

### bootcore-parent

项目父pom模块，外部项目可使用该pom作为父pom。

## 核心模块

### bootcore-core

核心模块，主要有工具类、接口dto、异常类、服务接口设计等实现，该模块设计成无任何外部jar包依赖，使得其他项目可以非常方便地进行引用。

每个子 package 的功能如下：

- common
  - 基础序列化实体、通用 Api 结果对象、枚举、分页模型、异常等通用功能。
- core
  - 核心层，与 common 类似，但比 common 抽象层次更低，主要是语言层面的功能，如 bean、io、类型转换（门面类为 Convert ）、日期处理、map 增强、反射等工具类，这部分代码来源于 hutool 工具包。
- service
  - 服务层封装，目前提供了一个最通用的数据服务接口，只有保存、更新、获取三个方法。
- web
  - web 层封装，目前提供了一个 BaseController 封装了返回 Api 对象的实现。

## 数据访问模块

### bootcore-data-mybatis

数据库访问模块，主要含有对mybatis-plus使用的简单封装，以及服务接口的抽象实现。

每个子 package 的功能如下：

- pagination
  - 分页模型的封装。
- service
  - core 模块中 DataService 的实现，在微服务项目开发过程中应当使用该实现作为父类，不应该使用 mybatis-plus 中的 IService 接口，后台项目中可选择性使用 IServie 接口。
- util
  - 反射工具类，主要提供从 Lambda 表达式中获取列名的能力，开发过程中尽量使用 Lambda 表达式代替列名的硬编码。

### bootcore-data-redis

redis模块，主要包括对pika的简单封装及序列化工具类。

### bootcore-data-db-datasource

数据源模块，目前支持动态数据源，未来计划加入数据库分片等支持。

该模块取自 [dynamic-datasource-spring-boot-starter](https://github.com/baomidou/dynamic-datasource-spring-boot-starter) 开源项目。

## 消息队列模块

### bootcore-amqp-rabbitmq

rabbitmq模块，主要扩展了消息发送，实现可靠的消息发送机制。

核心接口介绍：

- MessageSender
  - 消息发送接口，需要发送消息时使用该接口。
- RetryMessageStore
  - 需要重试的消息的存储接口，目前提供了进程内 Map 的存储实现，可提供 Redis 的存储实现以进一步提升可用性，Map 的实现相对来说简化了架构，视情况使用。

## 自动配置模块

### bootcore-autoconfigure

采用与 spring boot 一样的设计，提供了该模块，主要作用是运用 COC 原则简化配置，即按照使用惯例进行自动配置。

自动配置模块依赖许多第三方模块，这些模块应该采用 option 的方式做依赖管理，则可以同时兼具功能性与灵活性。

相关自动配置类介绍：

- RabbitMqSenderAutoConfiguration

  - 自动配置消息发送 bean，默认使用进程内存储重试消息。

- PackageDataSourceAutoConfiguration

  - 可以按 package 配置数据源，简化 DS 注解的使用，如下:

- ```yaml
  bootcore:  
      datasource:
          packages[com.ybdx.bootcore.sample.web.mapper]: slave
  ```

  MybatisPlusAutoConfiguration

  - 分页配置

- PikaAutoConfiguration

  - pika 自动配置，pika 与 redis 协议兼容，配置方式同 redis，其中指定序列化方式为 JSON 序列化。

- RedisTemplateAutoConfiguration

  - redis 自动配置，指定了序列化方式为 JSON 序列化。

- JobExecutorAutoConfiguration

  - 分布式 xxl job 的相关配置。

## 分布式任务后台

### bootcore-job-admin

分布式任务后台，该模块来自 xxl 开源项目，已部署至生产环境，项目中需要使用任务调度时引用 bootcore-starter-job-executor 模块。

## 依赖管理模块

依赖管理模块按 spring boot 项目的方式统一使用 starter 命名，starter 项目可以仅包含依赖管理，也可以包含部分代码，如工具类、自动配置等。

### bootcore-starter

基于 bootcore-starter 框架开发时首先引入的 starter，类似 spring-boot-starter，其中包含 bootcore-core 和 bootcore-autoconfigure 的依赖。

### bootcore-starter-data-db-datasource

目前包含 bootcore-data-db-datasource，未来可能引入其他数据源相关依赖。

### bootcore-starter-data-mybatis

包含 bootcore-data-mybatis 及 mysql-connector-java 依赖。

### bootcore-starter-data-redis

包含 bootcore-data-redis 和 commons-pool2 依赖，其中 commons-pool2 在 pika 配置中使用到了，需要使用 pika 时直接引用该 starter 即可。

### bootcore-starter-job-executor

包含 xxl-job-core 依赖。

### bootcore-starter-log

包含 logstash-logback-encoder 依赖，为发送日志到 ELK 所需依赖。

### bootcore-starter-rabbitmq

包含 bootcore-amqp-rabbitmq 依赖。

### bootcore-starter-test

包含 assertj-core 、mockito-core 、spring-boot-starter-test 依赖，都为单元测试中所需引入的依赖，在开发项目时引入这一个 starter 即可。

### bootcore-starter-web

目前仅含有 spring-boot-starter-web 依赖，未来的版本中会加入 web 工具、Shiro 等模块。

## 微服务相关 starter

### bootcore-cloud-starter-config

配置中心 starter ，含有 apollo-client 依赖。

### bootcore-cloud-starter-openfeign

服务间调用 starter ，含有 spring-cloud-starter-openfeign 、 bootcore-cloud-openfeign 、 feign-okhttp 依赖。

项目中使用以下配置启用 okhttp。

```yaml
feign:  
    okhttp:    
        enabled: true
```

熔断器模块，含有 spring-cloud-starter-alibaba-sentinel 依赖，其也基于 spring boot 2.2.5 版本。

项目中使用以下配置启用 sentinel 作为熔断器。

```yaml
feign:  
    sentinel:    
        enabled: true
```

服务注册模块，含有 spring-cloud-starter-netflix-eureka-client ，未来的版本可能采用其他注册中心。

## 示例模块

- bootcore-rabbitmq-sample

  - MessageSender 的使用示例。

- bootcore-web-sample

  - 主要包含数据访问，包括分页的示例。

- cloud-samples

  - bootcore-eureka-sample
    - eureka 服务端。
  - bootcore-contract-sample
    - 服务协议定义，不同于旧有的架构，在协议定义处是没有 feign 依赖的，因为服务调用的方式应当是可以灵活变更的，具体取决于客户端（服务调用方）想用何种技术，服务接口参考以下源代码。

- ```Java
  public interface MicroService {
  
      @GetMapping("/hello")
      String hello(@RequestParam(value="name") String name);
  
  }
  ```

  bootcore-service-sample

  - 服务提供方。

- bootcore-client-sample

  - 服务调用方，此处可以引入 feign 依赖，不要直接使用协议接口，而是继承它，参考如下代码：

```java
@FeignClient(value = "micro-service",fallback = MicroServiceFallback.class)
   public interface MicroServiceClient extends MicroService {
}
```



# 1.1.0 版本计划

## APM

引入 Skywalking 、 Prometheus 做性能监控、链路追踪。

## 缓存

引入 jetcache。

## Web

更多 web 工具类、安全相关工具类，引入 Shiro 做权限控制。

## 网关

替换掉现有网关。

# 2.0 版本计划

全新管理系统开发框架，大幅提升目前管理系统的开发效率。

# 其他资源

架构图：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d47c9c86493e41a19704e1387aa5bdf9~tplv-k3u1fbpfcp-watermark.image?)