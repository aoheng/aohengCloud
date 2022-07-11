# [DevOps操作手册] 华为云流水线配置

# 基本原则

为方便重用和简化编译构建的配置，项目代码需要遵守以下基本原则。

1. 项目结构
   1. 模块名称、app.id、目录名称保持一致。
   2. Git 仓库根目录不作为 app 模块，子目录为 app 模块。
   3. 示例如下：ufo-console-admin、ufo-console-job 为需要发布的服务，都处于二级目录且 app.id、模块名称和目录名称都相同。



1. 流水线命名规则：大写环境名称-app.id-自动发布

示例：



1. 流水线权限配置
   1. 开发人员仅能查看流水线，不能执行流水线
2. 流水线触发时机
   1. develop 分支的流水线为代码提交时触发
   2. Master 分支的流水线必须手动触发

# 数据库流水线

说明：一个数据库对应一个 liquibase 代码仓库，多个数据库放在一起影响自动发布的速度。

以下代码对应 `ufo-console-liquibase` 项目的配置，其他项目稍作微调即可。

## 创建 liquibase 项目

参考 ufo-console-liquibase 项目，注意模块名称中要包含 liquibase，以方便查找和区分，示例项目中为命名失误。



## Liquibase 文件组织

参考上图目录结构，master.xml 文件中进行数据库版本的组织，按每个迭代组织一个文件，文件中包含迭代版本，没有迭代版本概念的项目，推荐使用日期加注释的方式。

```
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
         http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">

    <include file="liquibase/changelog/db.changelog-1.0.xml"/>
    <!-- 增加 XXX 表 -->
    <include file="liquibase/changelog/db.changelog-210604.xml"/>
    
</databaseChangeLog>
```

在具体的 changelog.xml 中，包含具体的变更，一个变更对应一个sql，方便回滚。

## Liquibase 单元测试

Liquibase 项目使用 testcontainers 跑 docker 镜像，验证脚本是否正常，必须在项目中添加该单元测试，具体参考 ufo-console-liquibase 项目中的单元测试代码。

## Liquibase 流水线配置

Liquibase 项目不需要发布到 k8s 中，因此使用 jenkins 进行配置，可分为2-3个 jenkins 项目，分别对应 dev、uat、pro 环境的发布，dev 和 uat 可以视情况合并为 1 个，操作步骤如下：

1. 正常配置代码仓库，develop 分支对应 dev、uat 环境数据库的发布，master 对应 pro 环境。
2. develop 分支的构建触发器使用 Poll SCM，日程表：H/2 * * * *，master 分支不配置构建触发器，必须手动触发。
3. 增加构建步骤

步骤1

```
cd ufo-console-db-main
mvn package
```

步骤2（dev、uat）

```
cd ufo-console-db-main/target
java -Xms32m -Xmx128m -Denv=dev -jar ufo-console-db-main-1.0.0.jar --spring.profiles.active=dev
java -Xms32m -Xmx128m -Denv=uat -jar ufo-console-db-main-1.0.0.jar --spring.profiles.active=uat
```

在项目代码中做好 apollo 配置，避免在这个地方进行配置。

步骤2（pro环境）

```
cd ufo-console-db-main/target
java -Xms32m -Xmx128m -Denv=pro -jar ufo-console-db-main-1.0.0.jar --spring.profiles.active=pro
```

# Java 项目流水线

参考 `deploy-template` 项目，java 项目中需要包含其中的如下元素。



其中涉及 dockerfile、skywalking、Prometheus、logstash、apollo、pom依赖的相关配置，需要指定 apollo 的命名空间为 `application, global-metrics, global-web` 。

## 部署文件组织

用于构建的 dockerfile 按以下方式组织，build文件夹下子目录名称为服务模块的名称，dockerfile 名称前缀必须使用对应 apollo 环境变量。



按以上方式组织后，可直接使用 `DevOps服务平台` 项目中的 `Java-构建-推送镜像` 编译构建配置。

## 编译构建配置

构建步骤中包含镜像推送，uat 和 pro 镜像存储在同一个镜像仓库，通过参数传递，因此 uat 和pro 环境可共用同样的配置。

### 步骤1-Maven构建



### 步骤2-制作镜像并推送到SWR仓库



注意：自定义参数通过流水线配置，包含以下参数。



## 部署配置

部署配置中需要选择集群，不能使用参数传递，所以需要独立配置。

如图：



部署步骤-k8s部署：

UAT 环境选择 develop 分支，PRO 环境选择 master 分支的 yml 文件。



需要配置以下参数，供流水线设置，注意 env 参数不允许运行时设置，因为这里已经选择了相应的集群，若流水线上设置错误，反而容易引起问题。

- TODO：cpu和内存的配置暂时还未添加，后续需要加上。



## 流水线配置

构建阶段增加代码检查和质量门禁：



进行参数配置：



构建和部署中的 image_tag 参数均传递 releaseVersion 。



# Web 项目流水线

以下基于 ufo-console-web 项目，适用于基于 ant design pro 开发的后台项目。

## 部署文件组织

同样需要包含不同环境的 Dockerfile，dev 为可选，nginx 配置也保留多个环境的配置。



package.json 中新增以下配置。



## 编译构建配置

### 步骤1-Npm构建

增加下面一行即可。



### 步骤2-制作镜像并推送到SWR仓库

与 Java 项目类似，仅 DockerFile 路径不一样。



## 部署配置

与 Java 部署类似，区别在于不需要暴露 Prometheus 抓取地址，使用的 yml 不一样。



## 流水线配置

与 Java 流水线类似。



# 权限设置

配置好流水线后注意检查权限配置，默认开发人员是可以执行的，需要关闭其权限，仅允许查看。

## 流水线权限

注意：查看（不是编辑功能）流水线后才有权限操作。



## 编译构建权限

点击编辑后可修改权限。



## 部署权限

点击编辑后可修改权限。