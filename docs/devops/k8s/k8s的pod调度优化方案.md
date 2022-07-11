# k8s pod调度优化方案

# 一、前言

## **问题及原因**

k8s是通过sceduler来调度pod的，在调度过程中，由于一些原因，会出现调度不均衡的问题，例如：

- 节点故障

- 新节点被加到集群中

- 节点资源利用不足

这些都会导致pod在调度过程中分配不均，例如会造成节点负载过高，引发pod触发OOM等操作造成服务不可用。

其中，节点资源利用不足时是最容易出现问题的，例如，设置的requests和limits不合理，或者没有设置requests/limits都会造成调度不均衡。

## **解决办法及分析**

k8s在进行调度时，计算的就是requests的值，不管你limits设置多少，k8s都不关心。所以当这个值没有达到资源瓶颈时，理论上，该节点就会一直有pod调度上去。所以这个时候就会出现调度不均衡的问题。有什么解决办法？

- 给每一个pod设置 requests 和 limits 。

- 重平衡，采取人为介入或定时任务方式，根据多维度去对当前pod分布做重平衡。

# 二、工作负载设置指导

基于核心概念——`requests` 影响调度，`limits` 影响的是运行，来设置请求和限制。

1. requests 的设置
   1. `应该取工作负载运行的平均值`，需要根据负载运行情况定期调整。
   2. requests 应该 >= 容器中 java 程序 jvm 最小内存
2. limits 的设置
   1. limits 的设置要大于 requests
   2. 最大内存必须大于容器中 java 程序 jvm 最大内存的 15% 以上
3. 为命名空间配置默认的请求和限制（此项措施主要是防止单个负载引起集群不稳定，并不能解决调度不均衡的问题）
   1. 创建 LimitRange，应用在命名空间上，参考文档：https://kubernetes.io/zh/docs/tasks/administer-cluster/manage-resources/memory-default-namespace/
   2. `正式环境参考值`：内存 requests 256m limits 2.4g，cpu requests 0.25 core limits 4 core，按此设置和正式环境189个pod计算，总申请 47.25G 内存、47.25 core cpu
   3. `测试环境参考值`：内存 requests 256m limits 2.4g，cpu requests 0.05 core limits 2 core，按此设置和正式环境189个pod计算，总申请 47.25G 内存、9.45 core cpu

# 三、JVM 内存设置指导

建议：

Xms 128M起步，一般不超过512M，并发量大的视情况增大；

Xmx 256M起步，一般不超过2G，并发量大的视情况增大；后台系统建议设置不超过 1G，并发量小和次要服务建议设置为 256M-512M。

最大内存也可使用百分比进行设置，参考：https://dzone.com/articles/best-practices-java-memory-arguments-for-container。

注意：

limits 设置要大于 Xmx 设置，例 Xmx 为 2G，则 limits 可设置为 2.4G。

# 四、重新调度 pod 的工具

在 uat 环境可以使用下面的工具重新调度 pod，以平衡工作负载：

https://github.com/kubernetes-sigs/descheduler