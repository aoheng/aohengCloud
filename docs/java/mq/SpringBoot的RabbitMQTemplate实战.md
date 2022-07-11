# [RabbitMQ]SpringBoot的RabbitMQTemplate实战



### 前言

此文面向的是对RabbitMQ有实战需求的Java开发者，希望读者有一定的RabbitMQ基础上进行阅读，本文主要解决的是SpringBoot中如何去使用`RabbitMQTemplate`

### 通信模型

> 在代码中使用MQ发送消息的过程是异步执行的，消息到达RabbitMQ后，会在通信模型中找到适合的队列进行入队。

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/66f0e232107a41edb24304adc9aea9bf~tplv-k3u1fbpfcp-watermark.image?)
RabbitMQ通信模型

下面来看看消息到达RabbitMQ会发生什么，`Exchange`会将消息通过`RoutingKey`将消息路由到相应的队列，每当有消息进入到队列中时，消费端就会监听到该消息进行消费。

### Exchange

SpringBoot中对于Exchange有以下几种类型

| 交换机类型  | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| Default     | Spring默认创建的交换机，它会将消息路由至名称与`RoutingKey`相同的队列。 |
| Direct      | 通过`RoutingKey`路由到与`binding key`相同的队列。            |
| Topic       | 支持通配符级别的`binding key`，支持一对多的消息发布          |
| Fanout      | 将消息发生到与交换机绑定的队列                               |
| Headers     | 与Topic类似，基于消息的头信息进行路由                        |
| Dead letter | 死信交换机，无法投递的消息会到达这个交换机                   |

### SpringBoot集成RabbitMQ

- **pom.xml**

> 可能会出现版本兼容的问题，可自行更换



```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>2.3.5.RELEASE</version>
</dependency>
```

- **yml**

> SpringBoot的RabbitMQ具有丰富的配置，比如消费失败重试、消息确认模式、超时等,这里不做复杂的配置。用于简单的应用。



```yml
spring:
  rabbitmq:
    host: 192.168.14.148
    port: 5672
    username: root
    password: root
```

- **RabbitMQTemplate**



```java
package com.xjm.spring.data.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Configuration
@Slf4j
public class RabbitMQTemplateConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(){
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        return jackson2JsonMessageConverter;
    }
}
```

### 1. 默认交换机-简单的MQ发送与接收

- **config**



```java
package com.xjm.spring.data.rabbitmq.config;


import lombok.Getter;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简单的MQ配置类
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Configuration
@Getter
public class SimpleMQConfig {
    /**
     * 队列名
     */
    public static final String SIMPLE_QUEUE_NAME = "com.xjm.mq.simple";
    /**
     * 处理对象的MQ队列
     */
    public static final String HANDLER_OBJECT_QUEUE_NAME = "com.xjm.mq.simple.object";

    @Bean
    public Queue simpleQueue() {
        return new Queue(SIMPLE_QUEUE_NAME);
    }

    @Bean
    public Queue handleObjectQueue() {
        return new Queue(HANDLER_OBJECT_QUEUE_NAME);
    }

}
```

- **生产者**



```java
package com.xjm.spring.data.rabbitmq.producer;

import com.xjm.modules.model.Order;
import com.xjm.spring.data.rabbitmq.config.SimpleMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
public class SimpleProducer {

    private RabbitTemplate rabbitTemplate;

    @Autowired
    public SimpleProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发生消息到RabbitMQ,使用SpringBoot默认的交换机<br>
     *
     * @param message
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(SimpleMQConfig.SIMPLE_QUEUE_NAME, message);
    }

    /**
     * 消息体为对象。配置MessageConverter为Jackson2JsonMessageConverter即可
     * @param order
     */
    public void sendOrderMessage(Order order){
        rabbitTemplate.convertAndSend(SimpleMQConfig.HANDLER_OBJECT_QUEUE_NAME, order);
    }
}
```

- **消费者**



```java
package com.xjm.spring.data.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjm.modules.model.Order;
import com.xjm.spring.data.rabbitmq.config.SimpleMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@Slf4j
public class SimpleConsumer {

    @RabbitListener(queues = {SimpleMQConfig.SIMPLE_QUEUE_NAME})
    @RabbitHandler
    public void receiveMessage(String message) {
        log.info("simple consumer receive the message:{}", message);
    }

    @RabbitListener(queues = {SimpleMQConfig.HANDLER_OBJECT_QUEUE_NAME})
    @RabbitHandler
    public void receiveObject(Order order) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(order);
        log.info("simple consumer receive the object:{}", message);
    }
}
```

- **单元测试**



```java
package com.xjm.rabbit;

import com.xjm.modules.model.Order;
import com.xjm.spring.data.rabbitmq.producer.SimpleProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class SimpleMQTest {
    @Autowired
    private SimpleProducer simpleProducer;

    @Test
    public void test() throws InterruptedException {
        simpleProducer.sendMessage("First message in spring boot.");
        Thread.sleep(10000);
    }

    @Test
    public void testOrder() throws Exception {
        simpleProducer.sendOrderMessage(Order.builder()
                .createTime(new Date())
                .name("Phone")
                .price("2000")
                .build());
        Thread.sleep(10000);
    }
}
```

- **Result**

![img](https:////upload-images.jianshu.io/upload_images/19836894-b39f5d30a1456398.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

result

![img](https:////upload-images.jianshu.io/upload_images/19836894-5ef8b8741e593cc7.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

result

### 2. Direct型交换机的MQ模型:routingKey与bindingKey一致则投递

- **config**



```java
package com.xjm.spring.data.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Configuration
public class DirectMQConfig {
    /**
     * 消息的routing key与队列的binding key相同的队列
     */
    public static final String DIRECT_QUEUE_NAME = "com.xjm.mq.direct";
    /**
     * direct 交换机
     */
    public static final String DIRECT_EXCHANGE_NAME = "com.xjm.mq.direct.exchange";
    /**
     * routing key
     */
    public static final String DIRECT_ROUTING_KEY_NAME = "com.xjm.mq.direct.routing.key";

    /**
     * 交换机
     * @return
     */
    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    /**
     * 创建一条持久化的、非排他的、非自动删除的队列
     * @return
     */
    @Bean
    public Queue directQueue(){
        return new Queue(DIRECT_QUEUE_NAME);
    }

    /**
     * Binding,将该routing key的消息通过交换机转发到该队列
     * @return
     */
    @Bean
    public Binding directBinding(){
        return BindingBuilder.bind(directQueue()).to(directExchange()).with(DIRECT_ROUTING_KEY_NAME);
    }

}
```

- **生产者:这里展示两种发消息的模式，一种是简单的发送消息，一种是往消息的Header中添加参数**



```java
package com.xjm.spring.data.rabbitmq.producer;

import com.xjm.spring.data.rabbitmq.config.DirectMQConfig;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class DirectProducer {

    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(DirectMQConfig.DIRECT_EXCHANGE_NAME, DirectMQConfig.DIRECT_ROUTING_KEY_NAME, message);
    }

    /**
     * 发送消息时往请求头添加信息
     * @param message
     */
    public void sendMessageWithProperties(String message) {
        rabbitTemplate.convertAndSend(DirectMQConfig.DIRECT_EXCHANGE_NAME,
                DirectMQConfig.DIRECT_ROUTING_KEY_NAME,
                message,
                originalMessage -> {
                    MessageProperties messageProperties = originalMessage.getMessageProperties();
                    messageProperties.setHeader("X-TOKEN", UUID.randomUUID().toString());
                    return originalMessage;
                });
    }
}
```

- **消费者**



```java
package com.xjm.spring.data.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjm.spring.data.rabbitmq.config.DirectMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@Slf4j
public class DirectConsumer {

    @RabbitListener(queues = {DirectMQConfig.DIRECT_QUEUE_NAME})
    @RabbitHandler
    public void receiveMessage(String message, Message originalMessage) throws JsonProcessingException {
        Map<String, Object> headers = originalMessage.getMessageProperties().getHeaders();
        ObjectMapper objectMapper = new ObjectMapper();
        String headersParam = objectMapper.writeValueAsString(headers);
        log.info("direct consumer receive the message:{},original message:{},\n headers param:{}", message, originalMessage.toString(), headersParam);
    }
}
```

- **单元测试**



```java
package com.xjm.rabbit;

import com.xjm.spring.data.rabbitmq.producer.DirectProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class DirectMQTest {

    @Autowired
    private DirectProducer directProducer;

    @Test
    public void test() throws InterruptedException {
        directProducer.sendMessageWithProperties("Hello,2021");
        Thread.sleep(10000);
    }
}
```

- **Result**



result

### 3. Fanout型交换机MQ模型:订阅模式，消息到达交换机会转发到与该交换机绑定的队列

- **config**



```java
package com.xjm.spring.data.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * fanout:订阅交换机,可以实现发布订阅模式的消费模型<br>
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Configuration
public class FanoutMQConfig {
    /**
     * 与fanout绑定的第一个队列
     */
    public static final String FIRST_FANOUT_QUEUE_NAME = "com.xjm.mq.fanout.first";
    /**
     * 与fanout交换机绑定的第二个队列
     */
    public static final String SECOND_FANOUT_QUEUE_NAME = "com.xjm.mq.fanout.second";
    /**
     * fanout 交换机
     */
    public static final String FANOUT_EXCHANGE_NAME = "com.xjm.mq.fanout.exchange";

    /**
     * FanoutExchange,持久化、非自动删除
     *
     * @return
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE_NAME);
    }

    @Bean
    public Queue firstFanoutQueue() {
        return new Queue(FIRST_FANOUT_QUEUE_NAME);
    }

    @Bean
    public Queue secondFanoutQueue() {
        return new Queue(SECOND_FANOUT_QUEUE_NAME);
    }

    @Bean
    public Binding firstFanoutBinding() {
        return BindingBuilder.bind(firstFanoutQueue()).to(fanoutExchange());
    }

    @Bean
    public Binding secondFanoutBinding() {
        return BindingBuilder.bind(secondFanoutQueue()).to(fanoutExchange());
    }
}
```

- **生产者**



```java
package com.xjm.spring.data.rabbitmq.producer;

import com.xjm.spring.data.rabbitmq.config.FanoutMQConfig;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class FanoutProducer {

    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息至fanout交换机,由于fanout只关注订阅关系，所以routing key随便指定都可以
     * @param message
     */
    public void sendMessage(String message){
        rabbitTemplate.convertAndSend(FanoutMQConfig.FANOUT_EXCHANGE_NAME, StringUtils.EMPTY, message);
    }
}
```

- **消费者**



```java
package com.xjm.spring.data.rabbitmq.consumer;

import com.xjm.spring.data.rabbitmq.config.FanoutMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@Slf4j
public class FanoutConsumer {

    @RabbitListener(queues = {FanoutMQConfig.FIRST_FANOUT_QUEUE_NAME})
    @RabbitHandler
    public void firstReceiveMessage(String message) {
        log.info("first fanout consumer receive the message:{}", message);
    }

    @RabbitListener(queues = {FanoutMQConfig.SECOND_FANOUT_QUEUE_NAME})
    @RabbitHandler
    public void secondReceiveMessage(String message) {
        log.info("second fanout consumer receive the message:{}", message);
    }
}
```

- **单元测试**



```java
package com.xjm.rabbit;

import com.xjm.spring.data.rabbitmq.producer.FanoutProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class FanoutMQTest {
    @Autowired
    private FanoutProducer fanoutProducer;

    @Test
    public void test() throws InterruptedException {
        fanoutProducer.sendMessage("Produce once,consume many times");
        Thread.sleep(10000);
    }
}
```

- **Result**



result

### 4. Topic型交换机模型:将routingKey与binding key做通配符匹配，转发消息到匹配的队列

- **config**



```java
package com.xjm.spring.data.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Configuration
public class TopicMQConfig {
    /**
     * 体育主题的篮球队列
     */
    public static final String BASKETBALL_TOPIC_QUEUE_NAME = "com.xjm.mq.topic.sports.basketball";
    /**
     * 体育主题的足球队列
     */
    public static final String FOOTBALL_TOPIC_QUEUE_NAME = "com.xjm.mq.topic.sports.football";
    /**
     * 读书主题的阅读队列
     */
    public static final String BOOK_TOPIC_QUEUE_NAME = "com.xjm.mq.topic.book";
    /**
     * 主题交换机
     */
    public static final String TOPIC_EXCHANGE_NAME = "com.xjm.mq.topic.exchange";
    /**
     * 体育主题
     */
    public static final String SPORTS_TOPIC = "topic.sports.#";
    /**
     * 读书主题
     */
    public static final String BOOK_TOPIC = "topic.book.#";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Queue basketBallQueue() {
        return new Queue(BASKETBALL_TOPIC_QUEUE_NAME);
    }

    @Bean
    public Queue footBallQueue() {
        return new Queue(FOOTBALL_TOPIC_QUEUE_NAME);
    }

    @Bean
    public Queue bookQueue() {
        return new Queue(BOOK_TOPIC_QUEUE_NAME);
    }

    @Bean
    public Binding basketBallBinding() {
        return BindingBuilder.bind(basketBallQueue()).to(topicExchange()).with(SPORTS_TOPIC);
    }

    @Bean
    public Binding footBallBinding() {
        return BindingBuilder.bind(footBallQueue()).to(topicExchange()).with(SPORTS_TOPIC);
    }

    @Bean
    public Binding bookBinding() {
        return BindingBuilder.bind(bookQueue()).to(topicExchange()).with(BOOK_TOPIC);
    }
}
```

- **生产者**



```java
package com.xjm.spring.data.rabbitmq.producer;

import com.xjm.spring.data.rabbitmq.config.TopicMQConfig;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class TopicProducer {
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message, String topic) {
        rabbitTemplate.convertAndSend(TopicMQConfig.TOPIC_EXCHANGE_NAME, topic, message);
    }
}
```

- **消费者**



```java
package com.xjm.spring.data.rabbitmq.consumer;

import com.xjm.spring.data.rabbitmq.config.FanoutMQConfig;
import com.xjm.spring.data.rabbitmq.config.TopicMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@Component
@Slf4j
public class TopicConsumer {

    @RabbitListener(queues = {TopicMQConfig.BASKETBALL_TOPIC_QUEUE_NAME})
    @RabbitHandler
    public void basketballReceiveMessage(String message) {
        log.info("basketball consumer receive the message:{}", message);
    }

    @RabbitListener(queues = {TopicMQConfig.FOOTBALL_TOPIC_QUEUE_NAME})
    @RabbitHandler
    public void footballReceiveMessage(String message) {
        log.info("football consumer receive the message:{}", message);
    }

    @RabbitListener(queues = {TopicMQConfig.BOOK_TOPIC_QUEUE_NAME})
    @RabbitHandler
    public void bookReceiveMessage(String message) {
        log.info("book consumer receive the message:{}", message);
    }
}
```

- **单元测试**



```java
package com.xjm.rabbit;

import com.xjm.spring.data.rabbitmq.producer.TopicProducer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: Aoheng
 * @date: 2021/10/15 17:05
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TopicMQTest {

    @Autowired
    private TopicProducer topicProducer;

    @Test
    public void test() throws InterruptedException {
        topicProducer.sendMessage("start the game!", "topic.sports.news");
        topicProducer.sendMessage("SpringBoot 编程思想", "topic.book.springboot");
        Thread.sleep(10000);
    }
}
```



### 如何更好地理解RabbitMQ消息队列中间件

> 首先,我们需要投递的消息可以理解成一封信
>  交换机充当邮局的作用，负责帮我们转发消息
>  每个队列充当的是收信箱
>  此时，发送信息的端为生产者；接收消息的端为消费者。

end