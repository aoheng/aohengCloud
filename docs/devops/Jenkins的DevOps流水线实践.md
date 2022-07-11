# 基于Jenkins的DevOps流水线实践



## DevOps是什么

DevOps集文化理念、实践和工具于一身，可以提高组织高速交付应用程序和服务的能力，与使用传统软件开发和基础设施管理流程相比，能够帮助组织更快地发展和改进产品。这种速度使组织能够更好地服务于客户，并在市场上更高效地参与竞争。

DevOps（Development和Operations的组合）是一种重视软件开发人员（Dev）和IT运维技术人员（Ops）之间沟通合作的文化、运动或惯例。通过自动化软件交付和架构变更的流程，使得构建、测试、发布软件能够更加快捷、频繁和可靠。简单地来说，DevOps的目的是为了交付更加快速和敏捷，提高软件工程生产力。

而Jenkins是DevOps落地的常用方案之一。



![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cecf69546e28470d987b64ca145a9097~tplv-k3u1fbpfcp-watermark.image?)



## 什么是流水线

从某种抽象层次上讲，部署流水线（Deployment pipeline）是指从软件版本控制库到用户手中这一过程的自动化表现形式。举个列子，就像富士康生产iPhone流水线一样，一台iPhone的生产由几百道工序组成，每道工序由不同的人+机器来完成，而在软件工程中，也可以采用这种方式来实现自动化。

Jenkins 1.x版本只能通过界面手动操作来“描述”部署流水线。Jenkins 2.x开始支持pipeline as code了，可以通过“代码”来描述部署流水线。



 **使用“代码”而不是UI的意义在于：**

- 更好地版本化：将pipeline提交到软件版本库中进行版本控制。
- 更好地协作：pipeline的每次修改对所有人都是可见的。除此之外，还可以对pipeline进行代码审查。
- 更好的重用性：手动操作没法重用，但是代码可以重用。



## Jenkinsfile又是什么

熟悉Dockerfile的你肯定不会陌生，Jenkinsfile做的事情跟Dockerfile是相似的，所有部署流水线的逻辑都写在Jenkinsfile中。

Jenkins1.0默认不支持Jenkinsfile，需要升级的Jenkins到2.0版本才能支持Jenkinsfile功能。



## 多分支流水线构建流程

1. #### 创建一个多分支流水线项目

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6fbc6afd09fd448997bc6023c3ebe34b~tplv-k3u1fbpfcp-watermark.image?)

#### 配置多分支流水线项目

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3825fc22ed104dd1b66be357706e936a~tplv-k3u1fbpfcp-watermark.image?)

#### 在项目中加入Jenkinsfile文件

Jenkins会自动扫描所有包含jenkinsfile的分支，每个分支都会创建一个构建子项子，不需要手动去更改构建分支。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bc20420b46d3456a9687ea65d5043668~tplv-k3u1fbpfcp-watermark.image?)



#### 使用Blue Ocean界面查看多分支流水线项目

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3bf2b8e283624dca8c0f44a78ae66650~tplv-k3u1fbpfcp-watermark.image?)

## Jenkins pipeline语法入门

Jenkins使用groovy脚本语言来实现pipeline，最新版本的pipeline支持声明式groovy脚本，声明式脚本比原生的groovy脚本使用上更加简单，只需关注构建逻辑，不用花太多的时间学习groovy语言。

如下代码所示，这是一段最简单的pipeline脚本：

```Groovy
pipeline {    
     agent any     
     stages {      
        stage('Stage 1') {         
            steps {                
              echo 'Hello world!'            
          }        
      }     
   } 
}
```

上面的脚本声明了一个流水线阶段和一个步骤，在这个步骤中输出 hello world 内容。



### 常用指令

- #### stage

> 指持续交付过程的每一个离散部分，如构建、测试、打包、部署

- #### Steps

> 每个stage中的步骤

- #### enironment

> 环境变量，相当于java类中的全局变量，声明后可以在代码中引用这些变量。

- #### options

> 选项配置，可以配置pipeline的功能，比如禁止并发构建等。

```Groovy
options { 
    disableConcurrentBuilds() 
}
```

- #### tools

> 声明pipeline使用的工具，如maven，docker等。

```Groovy
# The tool name must be pre-configured in Jenkins under Manage Jenkins → Global Tool Configuration
tools {
   maven 'apache-maven-3.0.1' 
}
```

- #### when

> 指令允许流水线根据给定的条件决定是否应该执行阶段



详细指令参考：https://www.jenkins.io/zh/doc/book/pipeline/syntax/

1. ## 微服务项目pipeline脚本分析

```Groovy
def buildAll = false
def affectedModules = []
pipeline {
 agent any
 options {
   //禁止并发打包
    disableConcurrentBuilds()
 }
 triggers{
   gitlab(
    triggerOnPush: true,
    triggerOnMergeRequest: true,
    branchFilterType: 'All',
    secretToken: '0123456789'
        )
 }
 environment {
    // 项目根目录
    BASE_DIR = "compose-controller"
}
tools {
    maven 'maven'
 }
 stages {
  stage("diff") {
    when {
      expression {
        return env.BRANCH_NAME !='master' 
                && env.BRANCH_NAME!='develop'
             }
          }
 steps {
  sh 'printenv'
  echo "branch=${env.BRANCH_NAME}"
  script {
   def changes = []
   //check if triggered via Pull Request
   if(env.CHANGE_ID) {
      echo "Pull Request Trigger"
      changes = sh(returnStdout: true, script: "git --no-pager diff origin/${CHANGE_TARGET} --name-only").trim().split()
   } else if(currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size() > 0) {
      echo "用户触发构建"
      buildAll=true
      return
    } else {
      echo "Push推送触发构建"
      def changeLogSets = currentBuild.changeSets
      for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            def files = new ArrayList(entry.affectedFiles)
            for (int k = 0; k < files.size(); k++) {
                def file = files[k]
                   changes.add(file.path.replaceFirst("^${BASE_DIR}/",""))
             }
          }
       }
       // 提取要构建的模块
       changes.each {c ->
           def moduleName=""
           if(c.indexOf("/") > 1) {
             moduleName=c.substring(0,c.indexOf('/'))
           }
       // 修改了通用模块和父pom.xml，将全部构建
        if(moduleName.contains("common") || c == "pom.xml") {
            affectedModules = []
            buildAll = true
            echo "build whole project"
             return
         }else if(moduleName!=""){
              affectedModules.add(c.substring(0,c.indexOf('/')))
          }
         }
        }
      }
    }
// 构建整个项目
stage("build project") {
     when {
        expression {
            return buildAll
        }
     }
     steps {
        dir("${env.BASE_DIR}"){
           sh "mvn clean install -B -DskipTests -Pbuild -T 5"
        }
     }
  }
// 构建修改的模块
stage("build module") {
     when {
        expression {
            return affectedModules.size() > 0
        }
     }
    steps {
        dir("${BASE_DIR}"){
          script{
             affectedList = affectedModules.unique().join(",")
                echo "build modules -> ${affectedList}"
                sh "mvn clean install -B -pl ${affectedList} -amd -T 3"}
         }
      }
    }
    
stage("docker build"){
 steps{
   dir("${env.BASE_DIR}/controller-build-images"){
      echo "copy jar"
      sh 'mvn clean dependency:copy -Puat'
      script {
       if(buildAll && affectedModules.size()==0){
          files = findFiles(glob: 'target/jars/*.jar')
          files.each { file->
             affectedModules.add(file.name.take(file.name.indexOf('.')))
           }
        }
    if(affectedModules.size()==0){
            echo 'build finish.'
            return
     }
      def time= new Date().format('yyyyMMdd-hhmm')
      echo "current time->${time}"
      affectedModules.each { item->
      echo "start docker build -> ${item}"
      if(item.indexOf("controller")>=0){
      def tag="${env.BRANCH_NAME}-${time}"
      def image="swr.cn-south-1.myhuaweicloud.com/eighteen/${item}:${tag}"
      sh "docker build --build-arg JAR_FILE=${item}.jar --build-arg PROFILE=uat --build-arg PROFILE_UPPERCASE=UAT -t ${image} . "
      echo "push image to registry"
      docker.withRegistry('http://swr.cn-south-1.myhuaweicloud.com', 'docker-credential') {
                    docker.image("${image}").push()
                 }
             }
         }
      }
     }
   }
  }
 }
}
```

这段脚本分为三个阶段：

1. 第一个stage是分析git代码提交的commit文件信息，提取出变动的模块和鉴别出本次构建是整个项目还是单个模块。
2. 2、3个stage做的事情是同样的，就是对项目调用maven进行构建打包，两都区别在于2 stage是构建整个项目，3 stage是构建单个模块
3. 最后一个stage是把打包好的jar包进行docker 镜像打包，并推送到华为云仓库中

1. ## 新猫后台脚本分析

```Groovy
// jenkins流水线脚本
pipeline {
   agent any
   options {
      // 禁止并发打包
      disableConcurrentBuilds()
    }
    triggers{
        pollSCM("H/1 * * * *")
    }
   environment {
    // 项目根目录
    BASE_DIR = "owl-management/owl-management"
   }
    tools {
        maven 'maven'
    }
 stages {
   stage("maven build") {
         steps {
            dir("${env.BASE_DIR}"){
               sh "mvn clean install -B -DskipTests -Puat -T 3"
            }
        }
    }
    stage("docker build"){
     steps{
       dir("${env.BASE_DIR}"){
          script{
           def time=sh(returnStdout: true, script: "date +%Y%m%d%H%M").trim()
           sh "mvn dockerfile:build -Puat -Dtimestamp=${time}"
           sh "mvn dockerfile:push -Puat -Dtimestamp=${time}"
          }
        }
       }
     }
   }
}
```

新猫后台的脚本比较简单，只做两件事，一是对项目进行maven打包，二是docker镜像打包，镜像打包使用的是dockerfile-maven-plugin这个插件。

1. ## Blue Ocean介绍

Blue Ocean是jenkins提供的全新UI界面，更加简洁，更加现代，更加符合多分支流水线构建流程。

#### Blue Ocean插件安装

最新版本的jenkins是已经包含了blue ocean插件，如果没有这个插件，需要手动通过jenkins插件管理来安装此插件。

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7f99feb7c77740fe956e1c2f9fe31a27~tplv-k3u1fbpfcp-watermark.image?)



#### Blue Ocean界面风格

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6d60c9b9a7644591a43c341115d4bd64~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5fc361b1ad734df08717517bf9094b08~tplv-k3u1fbpfcp-watermark.image?)



#### 与旧版本的界面对比

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/21d7d5e96f1b44a7a1aa4a4989dc71bb~tplv-k3u1fbpfcp-watermark.image?)

经典界面风格

#### 使用blue ocean创建pipeline

Blue Ocean支持通过界面操作来实现pipeline脚本的编写，编写好之前脚本会自动保存到git代码仓库。简单的构建脚本可以通过blue ocean来实现，复杂的脚本仍需要手动编写。

##### 点击创建流水线项目

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/515a3c349d56425aadf873ac62efb2ed~tplv-k3u1fbpfcp-watermark.image?)



##### 填写项目git地址

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1d9218192c0042fb99ef21c9f8b22e97~tplv-k3u1fbpfcp-watermark.image?)



![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bf3c5053f27e452f8304c6d1c25e1286~tplv-k3u1fbpfcp-watermark.image?)

如果项目没有包含jenkinsfile，会提示创建流水线文件。



##### 通过创建stage和step

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e86b465dbd2f4ea58399845f7a102204~tplv-k3u1fbpfcp-watermark.image?)



##### 保存脚本

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c4e26e600f2b4400b7b7c4044bcad75d~tplv-k3u1fbpfcp-watermark.image?)

可以选择将Jenkinsfile脚本保存到master分支还是创建新的分支

1. ## 触发pipeline执行

过去我们都是通过先将代码推送到gitlab，再手动去修改jenkins构建的git分支代码和手动触发构建，这个过程从devops的角度来看，都应该自动化，用工具实现。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/803e8d3ba63d469ca8e17df557c3a3ef~tplv-k3u1fbpfcp-watermark.image?)

Jenkins Pipeline的触发条件分两种：时间触发和事件触发。

### 时间触发

时间触发是通过jenkins定时查询git仓库是否有变更，如果有变量就立即进行构建，比如可以配置每分钟查询一次。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8e3e68613e2c4a92a686ff29166a77e1~tplv-k3u1fbpfcp-watermark.image?)



### 事件触发

事件触发主要由某一类事件的发生来触发jenkins的构建，比如开发者提交了代码、另一个pipeline发生了构建等。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f8c4a7c7e3984372baa0cff175f14373~tplv-k3u1fbpfcp-watermark.image?)

## pipeline与k8s集成

解决了手动触发jenkins构建只是实现了CI/CD流程的半自动化，没有全自动化，将jenkins与k8s集成生，就可以实现CI/CD合自动化，整个过程无须人工干预，全部由工具完成。

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/94b9a177f0e648ddb95f7956f0874ab4~tplv-k3u1fbpfcp-watermark.image?)