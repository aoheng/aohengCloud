# Java后端项目Git流程和规范

# **前言**

信息中心后端项目Git工作流程适用于需要持续发布的后端项目（网站、接口和后端服务等），不适用于需要同时存在不同版本的项目（框架、组件、app等）。

该工作流程基于git flow分支策略，该策略的优点是分支清晰，能够应付开发流程中的许多情况，缺点是分支过多，开发过程会产生过多的合并操作，于是在基于该策略的基础上做适当的简化，并考虑并行迭代的情况，综合制定了该工作流程。

本文档共包含分支策略、工作流程、使用规范三个部分，分支策略主要是对git flow的介绍，工作流程部分则描述在具体开发过程中该如何实施，最后的使用规范需要大家共同遵守才能保证分支策略得到良好的执行。

## **1、术语说明**

- 持续集成/CI：使用该术语一般指feature分支的代码频繁集成到develop分支，并由CI自动构建到测试环境，feature集成到develop一天内最少一次

- 持续集成环境：根据特定分支配置的自动构建、运行测试和部署测试程序的环境

- 预发布环境：接近生产的环境，而非测试环境。

# **一、分支策略**

## **1、分支策略**

基于 git flow，但采用更适合我们公司的分支策略执行流程。

Git flow主要流程见下图，具体使用可参考：http://www.ruanyifeng.com/blog/2012/07/git.html

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6afb48ad49fe42078b15c375329181c5~tplv-k3u1fbpfcp-watermark.image?)

## **2、长期分支**

- master：用于存放对外发布的版本，任何时候在这个分支拿到的都是稳定的版本。

- develop：用于日常的开发，存放最新的开发版本。

## **3、临时分支**

- feature：用于开发特定功能从develop中分出来的，一个feature分支对应一次迭代开发，开发完成后合并到develop分支中；

- hotfix：用于修复bug，从master中分出来的，开发完成后合并到master、deveop分支。

- release：指发布正式版本之前（即合并到Master分支之前），我们可能需要有一个预发布的版本进行测试。

# **二、工作流程**

**注：以下文字中粗体为特殊情况，大多数时候不需考虑，从流程中去除的话，该工作流程并不复杂。**

## **1、项目初始**

- gitlab上创建项目

- 创建develop分支，并将其设置为保护性分支，具有Maintainer权限的用户才可以直接push和merge，其他用户需要将代码提交到自己的分支后发起merge request（以下简称MR）请求，由Maintainer和团队成员进行代码评审决定是否接受合并。

- 修改master分支为不允许任何人推送，该分支只能通过MR进行变更。



## **2、迭代开始阶段**

- 情况1：迭代时间较长、分工明确

- 开发人员各自新建属于自己的feature分支，代码持续集成到develop。

- **情况2：迭代时间较短、分工不明确或需要多个人开发同一功能**

- **开发人员共享一个feature分支，代码持续集成到develop。**

- **情况3：迭代A、迭代B同时启动，A为主要迭代**

- **主要迭代根据情况1、2选择开发方式，代码持续集成到develop**

- **次要迭代从develop分出sprint-{版本}，feature基于该版本开发，代码持续集成到sprint-{版本}**

- **情况4：主要迭代A已启动，突然启动迭代B**

- **迭代B从master分出sprint-{版本}，feature基于该版本开发，代码持续集成到sprint-{版本}**

**注意：情况2、3、4均为特殊情况，应尽量避免**，尤其是3、4两种情况需要将代码持续集成到另一个版本，那么需要配置另一套持续集成环境，同时也加大了代码评审、分支合并的麻烦。下面描述的开发阶段以情况1为准，若为3、4情况，则需要将develop分支替换为sprint分支。

## **3、迭代开发阶段**

- 开发人员完成feature的开发或阶段性开发时，发起到develop的MR。

- Maintainer和团队成员对MR进行review，拒绝的需要提交者重新修改代码后再次提交MR，接受的将合并到develop，并自动构建到测试环境。

- 迭代整体测试通过后进入发布准备阶段。

## **4、发布准备阶段**

- 对于无预发布环境的项目，跳过准备阶段，进入发布阶段。

- **对于有预发布环境的项目，从develop中新建release为保护性分支（若已有则合并，该分支可在部署稳定后删除）。**

- **release分支构建到预发布环境进行测试，若存在bug，在该分支上进行修改。**

- **完成release分支的测试后，进入发布阶段。**

## **5、发布阶段**

- 提交发布申请，运维审核通过后进行代码合并

- 存在release时，release分支合并到develop和master，不存在时，develop合并到master。

- 通过master构建并发布到生产环境。

- 在master上打上版本tag标签，标记发布。

## 5.1、线上发布回滚情况

- 出现发布回滚情况时，合并到master上的代码也需进行回滚操作。

## **6、线上bug修复**

- 从master新建hotfix分支用于修复bug。

- 完成后将hotfix合并到develop进行测试。

- 测试通过后合并到master进行发布，并打上小版本号的tag标签。

- **若当前存在release分支，还需将hotfix合并到release。**

- 删除hotfix分支。

# **三、使用规范**

## **1、临时分支命名规范**

| 临时分支 | 前缀     | 备注                                                         |
| -------- | -------- | ------------------------------------------------------------ |
| feature  | feature- | 若gitlab中包含任务issue，可以采用feature-{issueid}-{简单描述}命名 |
| release  | release- | 以版本名称或版本号结尾                                       |
| hotfix   | hotfix-  | 若gitlab中包含bug issue，可以采用feature-{issueid}-{简单描述}命名 |
| sprint   | sprint-  | 以迭代名称或版本号结尾                                       |



CI管道可通过前缀匹配进行相关的自动化操作。



分支前缀，一般用-/连接，很少用_，_表示两个词语有关联，feature分支这里实际上是一种分组，所以一般用-/，可以在前缀之后使用_。

## **2、feature分支使用规范**

- 分支粒度：以一个功能为单位，尽量对应gitlab上的一个任务issue，开发下一个功能时新建feature

- 存在时间：尽量短，合并到develop后选择合适的时间点删除feature

- 集成频率：每天最少一次，feature要经常合并到develop中，便于code∂ review，开发要经常从develop中获取最新代码；

- 合并请求：当feature完成的时候即可发起MR，但当一个feature开发时间较长时，也应当尽量提早发起MR，此时可在MR中添加WIP前缀，表示当前工作正在处理中，相关人员可以提前审核，但不合并



![img](https://oye0ns60i0.feishu.cn/space/api/file/out/1ch37iyIZOE94CPJBltSEiCTaLWNRhenfIB5nyE2w9k1AHlqgO/)

 

## **3、hotfix分支使用规范**

- 与feature鉴别：线上除紧急bug修复，当需要做功能性调整并快速上线时，也需要新建hotfix分支，而非feature。

## **4、sprint分支使用规范**

- sprint的使用与develop相同，也为保护性分支

- 需要从sprint中分出feature分支进行开发，从feature提交MR到sprint中做代码评审

## **5、release分支使用规范**

- release为保护性分支

- 使用feature分支进行bug修复

# **其他使用技巧**

1. Git使用规范流程

参阅：http://www.ruanyifeng.com/blog/2015/08/git-use-process.html

1. 在idea上通过rebase合并commit
