# SkyWalking极简入门

SkyWalking 是一个基于 Java 开发的分布式系统的应用程序性能监视和可观测性分析平台，专为微服务、云原生架构和基于容器（Docker、K8s、Mesos）架构而设计。

## 一、SkyWalking 简介

SkyWalking 是观察性分析平台和应用性能管理系统。

提供分布式追踪、服务网格遥测分析、度量聚合和可视化一体化解决方案。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4a6d26454c04436e9d82750be27eb9a8~tplv-k3u1fbpfcp-watermark.image?)

### 1.1：SkyWalking 特性

- 多种监控手段，语言探针和 service mesh

- 多语言自动探针，Java，.NET Core 和 Node.JS

- 轻量高效，不需要大数据

- 模块化，UI、存储、集群管理多种机制可选

- 支持告警

- 优秀的可视化方案

### 1.2：SkyWalking 核心概念

- **Service** - 服务。代表一组为传入请求提供相同的行为的工作负载。 使用代理或 SDK 时，可以定义服务名称。

- **Service Instance** - 服务实例。服务组中的每个工作负载都称为一个实例。就像 Kubernetes 中的 Pod 一样，它在 OS 中不必是单个进程。

- **Endpoint** - 端点。它是特定服务中用于传入请求的路径，例如 HTTP URI 路径或 RPC 服务类+方法签名。

## 二、SkyWalking 架构

从逻辑上讲，SkyWalking 分为四个部分：探针（Probes），平台后端，存储和 UI。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4a6d26454c04436e9d82750be27eb9a8~tplv-k3u1fbpfcp-watermark.image?)

- 上部分 **Agent** ：负责从应用中，收集链路信息，发送给 SkyWalking OAP 服务器。目前支持 SkyWalking、Zikpin、Jaeger 等提供的 Tracing 数据信息。而我们目前采用的是，SkyWalking Agent 收集 SkyWalking Tracing 数据，传递给服务器。

- 下部分 **SkyWalking OAP** ：负责接收 Agent 发送的 Tracing 数据信息，然后进行分析(Analysis Core) ，存储到外部存储器( Storage )，最终提供查询( Query )功能。

- 右部分 **Storage** ：Tracing 数据存储。目前支持 ES、MySQL、Sharding Sphere、TiDB、H2 多种存储器。而我们目前采用的是 ES ，主要考虑是 SkyWalking 开发团队自己的生产环境采用 ES 为主。

- 左部分 **SkyWalking UI** ：负责提供控台，查看链路等等。

## 三、SkyWalking 安装

进入 [Apache SkyWalking 官方下载页面](http://skywalking.apache.org/downloads/)，选择安装版本，下载解压到本地。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/783fac24d0bf459f8b705ec78318e0e5~tplv-k3u1fbpfcp-watermark.image?)

安装分为三个部分：

- [Backend setup document](https://github.com/apache/skywalking/blob/master/docs/en/setup/backend/backend-setup.md)

- [UI setup document](https://github.com/apache/skywalking/blob/master/docs/en/setup/backend/ui-setup.md)

- [CLI set up document](https://github.com/apache/skywalking-cli)

## 四、JavaAgent介绍

SkyWalking探针在使用上是无代码侵入的，而这种无侵入的自动埋点基于Java的JavaAgent技术。



**JAVA的JavaAgent特性介绍**

在JDK1.5之后可以在启动Java程序时加载JavaAgent，此特性提供了在JVM将字节码文件读入内存之后，使用对应的字节流在Java堆中生成一个Class对象之前，对其字节码进行修改的能力，而JVM也会使用用户修改过的字节码进行Class对象的创建。



SkyWalking探针依赖于JavaAgent在一些特殊点（某个类的某些方法）拦截对应的字节码数据并进行AOP修改。当某个调用链路运行至已经被SkyWalking代理过的方法时，SkyWalking会通过代理逻辑进行这些关键节点信息的收集、传递和上报，从而还原出整个分布式链路。



项目添加Agent探针

1：下载JavaAgent

在SkyWalking官方网站（http://skywalking.apache.org/downloads/）下载最新的官方Release包并找到其中的agent文件夹，





## 五、案例分析



![origin_img_v2_8fedfa39-151e-4934-a288-935e653b4a5g.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/470e6f9070f84bd2b051c2435ca17377~tplv-k3u1fbpfcp-watermark.image?)



1.链路追踪，通过TID查询。

2.Database模块可查询慢Sql语句，对系统性能排查有很大帮助。



## 6.架构图

![架构图.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/54eea6402ea74f9e9f1971901eaa8775~tplv-k3u1fbpfcp-watermark.image?)

如上图所示，云原生 DevOps 整个生命周期的工具链全景图，希望对大家有所启发......



