# Loki日志系统接入指南



| 作者     | Aoheng               |
| -------- | -------------------- |
| 当前版本 | v1.0.0               |
| 需求文档 | Loki日志系统接入指南 |

![img](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/8ABmOoxX5bx2lawZ/img/f327b772-72fd-46e7-8edb-6a84d9f358fa.png)

**github**:https://github.com/grafana/loki/



## 简介

**Loki**是受 *Prometheus* 启发由Grafana Labs团队开源的项目，是一款可扩展，高可用，支持多租户的日志聚合系统。开发语言: Google Go，设计的理念就是为了让日志聚合更简单，它被设计为非常经济高效且易于操作。它不索引日志的内容，而是为每个日志流设置一组标签，而不是对全文进行检索。系统架构十分简单，它主要由三部分组成：

- **Promtail**：是日志收集器，负责收集应用的日志并发送给**Loki**。
- **Loki**：用于日志的存储和解析，并提供查询API给下游展示。
- **Grafana**：负责将Loki的日志可视化。

Loki流程图

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d40a0cd0c1e440b587a042f4a5243c1e~tplv-k3u1fbpfcp-watermark.image?)

只要在应用程序服务器上安装promtail来收集日志然后发送给Loki存储，就可以在Grafana UI界面通过添加Loki为数据源进行日志查询（如果Loki服务器性能不够，可以部署多个Loki进行存储及查询）。作为一个日志系统不关只有查询分析日志的能力，还能对日志进行监控和报警



### 系统架构

![img](https://img-blog.csdnimg.cn/20201209155608293.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xhenljaGVlcnVw,size_16,color_FFFFFF,t_70)



**promtail**收集并将日志发送给loki的 Distributor 组件

**Distributor**会对接收到的日志流进行正确性校验，并将验证后的日志分批并行发送到Ingester

**Ingester** 接受日志流并构建数据块，压缩后存放到所连接的存储后端

**Querier** 收到HTTP查询请求，并将请求发送至Ingester 用以获取内存数据 ，Ingester 收到请求后返回符合条件的数据 ；

如果 Ingester 没有返回数据，Querier 会从后端存储加载数据并遍历去重执行查询 ，通过HTTP返回查询结果



## 对比ELK

- ELK虽然功能丰富，但规模复杂，资源占用高，操作苦难，很多功能往往用不上，有点杀鸡用牛刀的感觉。
- 不对日志进行全文索引。通过存储压缩非结构化日志和仅索引元数据，Loki 操作起来会更简单，更省成本。
- 通过使用与 Prometheus 相同的标签记录流对日志进行索引和分组，这使得日志的扩展和操作效率更高。
- docker安装部署简单快速，且受 Grafana 原生支持。



**尤其是对于互联网医院的客户，本地私有化部署，客户可提供的主机配置有限，Loki的轻量级好处就有了用武之地！！！**



## 安装部署

### 测试环境

访问地址：http://192.168.1.134:3000/login    账号密码：admin/admin

### 官方文档

具体可查看官网部署文档，点击[Docker Compose](https://grafana.com/docs/loki/latest/installation/docker/)

### Docker Compose安装

**Docker Compose 下载**

```bash
wget https://raw.githubusercontent.com/grafana/loki/v2.5.0/production/docker-compose.yaml -O docker-compose.yaml
```

**docker-compose.yml文件**

```
version: "3"

networks:
  loki:

services:
  loki:
    image: grafana/loki:2.5.0
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - loki

  promtail:
    image: grafana/promtail:2.5.0
    volumes:
      - /data/log/:/data/log/        #挂载日志目录
      - /var/log:/var/log
      - /etc/promtail/config.yml:/etc/promtail/config.yml   #-v双向绑定宿主主机配置文件，方便添加job
    command: -config.file=/etc/promtail/config.yml
    networks:
      - loki

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    networks:
      - loki
```

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ea8417a7378e405093d9c1e0a24b626b~tplv-k3u1fbpfcp-watermark.image?)



**添加job日志** (敲重点)

/etc/promtail/config.yml配置文件添加job，job就是在grafana查询时指定的job项目，不同项目代表不同job名称，可添加多个Job项目。图示添加两个项目：

- wx-consult
- wx-org

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9481632f276f45a1bc4645e19101e6cc~tplv-k3u1fbpfcp-watermark.image?)

**挂载日志**

docker启动容器，通过-v挂载日志到宿主主机的目录，

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/32bb710a5d9a42fa9ac819a02d33d4de~tplv-k3u1fbpfcp-watermark.image?)



**promtail收集挂载日志**

挂载到宿主主机的日志文件，如图所示：

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ec873f65ce434d098873b179be2e3061~tplv-k3u1fbpfcp-watermark.image?)



**启动容器**

```
docker-compose -f docker-compose.yaml up -d
```



**部署成功！！！**

是不是so easy？let's go！开启探索之旅



## grafana使用指南

登录grafana，测试环境访问地址：http://192.168.1.134:3000/login  账号密码：admin/admin

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1c522afc3af148c5bbd80324b77ad64e~tplv-k3u1fbpfcp-watermark.image?)



explore->开启你的搜索探索之旅......

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/43fb3e480a1749ae826a21f98fc2065b~tplv-k3u1fbpfcp-watermark.image?)





## 大功告成，Congratulation！！！



具体可参考文档

[轻量级日志系统Loki原理简介和使用]: https://developer.aliyun.com/article/893358





