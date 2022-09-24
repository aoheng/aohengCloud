# 互联网医疗研发规范



## 前言

由阿里向 Java 社区开源的 Java 开发手册其中包含许多优秀的规范，本文档为该手册的扩展内容，针对当前项目开发现状而编写，其中部分内容来自于该手册，由于是重点内容，所以在该文档中再次强调。

适用于互联网团队的研发开发规范标准



## Pgsql规范

**数据库设计**

1.基础字段：id（int8）,org_id(int4)，created_by（int8），updated_by（int8），delete_flag，created_date（timestamp），updated_date（timestamp），遵循驼峰命名。

2.数据表，用delete_flag标识是否删除（bool类型，false/true），杜绝占用status字段。

3.新建数据表，凡是主键ID，数据表类型都统一设置为int8，杜绝使用递增序列，采用雪花算法赋值id。

4.jsonb字段，可以把多个字段作为一个json存储，但是需要明确json的层级不能过深，控制在2层以内，并记录每个字段及对应注释

5.检索频繁的字段（及日期），需要设计为物理字段，便于做索引优化；常用字段记得添加索引，并记录到SQL升级脚本，jsonb字段也可以添加索引，具体百度

6、jsonb字段，同一张表中，不同业务类型，但相同含义的字段，不可分别存储在两个jsonb中；不同含义的属性，不可使用相同命名存储在一个jsonb中；更新jsonb类型的字段时，带上COALESCE()函数（判断jsonb字段是否为空，如果为空默认为’{}’

7.【推荐】单表索引数量不超过五个，注意查询sql语句索引失效语句，比如is null等等



**liqiubase版本管理**

[Liquibase结合postgresql使用指南](https://alidocs.dingtalk.com/document/edit?dentryKey=49jPoYaGsmmGa5JE)





## Git版本规范

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6afb48ad49fe42078b15c375329181c5~tplv-k3u1fbpfcp-watermark.image?)

### **1、长期分支**

- master：用于存放对外发布的版本，任何时候在这个分支拿到的都是稳定的版本。

- develop：用于日常的开发，存放最新的开发版本。

### **2、临时分支**

- feature：用于开发特定功能从develop中分出来的，一个feature分支对应一次迭代开发，开发完成后合并到develop分支中；

- hotfix：用于修复bug，从master中分出来的，开发完成后合并到master、deveop分支。

- release：指发布正式版本之前（即合并到Master分支之前），我们可能需要有一个预发布的版本进行测试。

### 3、分支命名规范

- **feature_crm_2.0.4_****：开发分支feature+产品线+版本号+（功能特性）开发人员新建自己的分支，开发完成及时合并到develop分支
- **release_crm_2.0.4**：预发布分支release+产品线+版本号
- **hotfixes_crm_2.0.4**：热修复分支hotfixes+产品线+版本号

### 4、代码评审

- 设定项目负责人代码评审机制
- git评分机制



## Java规范

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d6d12dac071e45b3b930a58e0c1330ac~tplv-k3u1fbpfcp-watermark.image?)



-  开放接口层：可直接封装 Service 方法暴露成 RPC 接口；通过 Web 封装成 http 接口；进行网关安

  全控制、流量控制等。

  • 终端显示层：各个端的模板渲染并执行显示的层。当前主要是 velocity 渲染，JS 渲染，JSP 渲染，移动端展示等。

  • Web 层：主要是对访问控制进行转发，各类基本参数校验，或者不复用的业务简单处理等，**Controller 层不可以直接调用 DAO 层**。

  • Service 层：相对具体的业务逻辑服务层。

  • Manager 层：通用业务处理层，它有如下特征：

  1）对第三方平台封装的层，预处理返回结果及转化异常信息。

  2）对 Service 层通用能力的下沉，如缓存方案、中间件通用处理。

  3）与 DAO 层交互，对多个 DAO 的组合复用。

  • DAO 层：数据访问层，与底层 PgSQL、Mysql、Hbase 等进行数据交互。

  

  ### **分层领域模型规约：**

  • DO（Data Object）：此对象与数据库表结构一一对应，通过 DAO 层向上传输数据源对象。 

  • DTO（Data Transfer Object）：数据传输对象，Service 或 Manager 向外传输的对象。 

  • BO（Business Object）：业务对象，由 Service 层输出的封装业务逻辑的对象。 

  • AO（Application Object）：应用对象，在 Web 层与 Service 层之间抽象的复用对象模型，极为贴 近展示层，复用度不高。 

  • VO（View Object）：显示层对象，通常是 Web 向模板渲染引擎层传输的对象。 

  • Query：数据查询对象，各层接收上层的查询请求。注意超过 2 个参数的查询封装，禁止使用 Map 类 来传输。 

  

  

  ### **命名风格**

  【强制】方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase 风格，必须遵 从驼峰形式。 

  【强制】POJO 类中布尔类型变量都不要加 is 前缀，否则部分框架解析会引起序列化错误。

  

  ### **注释规约** 

  1. 【强制】类、类属性、类方法的注释必须使用 Javadoc 规范，使用/**内容*/格式，不得使用 // xxx 方式。 

  说明：在 IDE 编辑窗口中，Javadoc 方式会提示相关注释，生成 Javadoc 可以正确输出相应注释；在 IDE 中，工程调用方法时，不进入方法即可悬浮提示方法、参数、返回值的意义，提高阅读效率。 2. 【强制】所有的抽象方法（包括接口中的方法）必须要用 Javadoc 注释、除了返回值、参数、 异常说明外，还必须指出该方法做什么事情，实现什么功能。 

  说明：对子类的实现要求，或者调用注意事项，请一并说明。 3. 【强制】所有的类都必须添加创建者和创建日期。 4. 【强制】方法内部单行注释，在被注释语句上方另起一行，使用//注释。方法内部多行注释 使用/* */注释，注意与代码对齐。 5. 【强制】所有的枚举类型字段必须要有注释，说明每个数据项的用途。 6. 【推荐】与其“半吊子”英文来注释，不如用中文注释把问题说清楚。专有名词与关键字保 持英文原文即可。 

  

  ### **日志级别**

  【推荐】谨慎地记录日志。生产环境禁止输出 debug 日志；有选择地输出 info 日志；如果使 用 warn 来记录刚上线时的业务行为信息，一定要注意日志输出量的问题，避免把服务器磁盘 撑爆

  【推荐】可以使用 warn 日志级别来记录用户输入参数错误的情况，避免用户投诉时，无所 适从。如非必要，请不要在此场景打出 error 级别，避免频繁报警。 

  说明：注意日志输出的级别，error 级别只记录系统逻辑出错、异常或者重要的错误信息。

  【推荐】异常信息应该包括两类信息：案发现场信息和异常堆栈信息。如果不处理，那么通 过关键字 throws 往上抛出。 

  正例：logger.error( "方法名", e); 

  

  ### [代码问题整理](https://alidocs.dingtalk.com/document/edit?dentryKey=0BODEJnKCVVx2k10)

  



**代码审查的重点模块**

代码审查标准：[代码审查报告V1.0](https://space.dingtalk.com/s/gwHOBBOHmgLOAwFNQQPaACA5ZmMzZjUzNThjYTI0ZDMwOGQ2ZGM1NWZlMGVhNmVmNw )  密码: UgZD

### 引用

请阅读：

- **阿里巴巴Java开发手册（详尽版）** **[「Java开发手册（嵩山版）.pdf](https://www.aliyundrive.com/s/hneQcHpJ9QN)**

- **Google Java编程风格指南：** http://hawstein.com/2014/01/20/google-java-style/



## redis规范

- ![img](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/W4j6OJQ3kKRjq3p8/img/ff50e32c-e7c3-40bd-9775-3e43174a4646.png)

### 1.命名规范

【推荐】Redis key 命名需具有可读性以及可管理性，不该使用含义不清的 key 以及特别长的 key 名；

【强制】以英文字母开头，命名中只能出现**小写字母、数字**、英文点号 (.) 和英文半角冒号(:)；

【强制】不要包含特殊字符，如下划线、空格、换行、单双引号以及其他转义字符；

【强制】命名规范：业务模块名: 业务逻辑含义: 其他

举例：spring:user:info:***

举例：crm:card:***

### 2.避免**BigKey**

通常我们会将含有较大数据或含有大量成员、列表数的Key称之为大Key，下面我们将用几个实际的例子对大Key的特征进行描述：

| **类型** | **大小** | **描述**                                                     |
| -------- | -------- | ------------------------------------------------------------ |
| STRING   | 5MB      | 字符串常用，注意value大小，5 兆字节(mb)=5120 千字节(kb)      |
| LIST     | 20000个  |                                                              |
| ZSET     | 20000个  |                                                              |
| HASH     | 100MB    | 它的成员数量虽然只有1000个但这些成员的value总大小为100MB（成员体积过大） |

需要注意的是，在以上的例子中，为了方便理解，我们对大Key的数据、成员、列表数给出了具体的数字。为了避免误导，在实际业务中，大Key的判定仍然需要根据Redis的实际使用场景、业务场景来进行综合判断。

举例：用户登录信息，采用hash结构，spring:session:sessions:**



### 3.注意问题

- 使用过程场景中应注意**缓存穿透，缓存击穿，缓存雪崩**等问题。
- Redis key 一定要设置过期时间。要跟自己的业务场景，需要对 key 设置合理的过期时间。可以在写入 key 时，就要追加过期时间；也可以在需要写入另一个 key 时，删除上一个 key。
- 要预估 O(N) 操作的元素数量，避免全量操作，可以使用 HSCAN，SSCAN，ZSCAN 进行渐进操作







## idea插件

【必装】代码格式化：Save Actions

【必装】git提交模板：Git Commit Template

【必装】代码分析：SonarLint

【推荐】代码扫描：Alibaba Java Coding Guidelines

【推荐】





欢迎补充......

