# Jenkins部署docker



## Jenkins流水线脚本

```shell
env.BRANCH = "$branch"
env.PUBLISH = "$publish"

node {
   def mvnHome = tool 'maven384-jdk11'
   def jdk11 = tool 'jdk11'
   env.PATH = "${jdk11}/bin:${mvnHome}/bin:${env.PATH}"

    def remote = [:]
  remote.name = 'test'
  remote.host = '192.168.*.*'
  remote.user = '***'
  remote.password = '***'
  remote.allowAnyHosts = true
   
  def remote888 = [:]
  remote888.name = 't***'
  remote888.host = '192.168.*.**'
  remote888.user = '***'
  remote888.password = '***'
  remote888.allowAnyHosts = true
    
    sh "echo '$PATH'"
    sh "which java"
    sh "which mvn"

   stage('Preparation') {
     echo 'Preparation'
     sh 'git config --global user.email "****@qq.com" '
 

    dir('aohengcloud-***') {
        git branch: BRANCH, credentialsId: '****', url: 'git代码地址'
    }      

   }


    stage('build') {
        echo 'Building aohengcloud-***'
        sh 'cd aohengcloud-*** && /home/apache-maven-3.8.1-jdk11/bin/mvn -U -P prod -Dmaven.test.skip=true clean install'      
   }
    
   stage('archiveArtifacts') {      
        echo 'archiveArtifacts'          
        archiveArtifacts artifacts: 'aohengcloud-***/aohengcloud-***-rest/target/aohengcloud-***-rest-1.0.0-SNAPSHOT-exec.jar', fingerprint: true, followSymlinks: false, onlyIfSuccessful: true      
   } 

    stage('copyArtifacts') {
      echo 'copyArtifacts'       
    // copyArtifacts  fingerprintArtifacts: true, projectName: 'aohengcloud-***', selector: lastSuccessful(),target: 'target'
    }

    stage('copy2linux') {
     echo 'copy2linux'      
     sshCommand remote: remote, command: "mkdir -p /home/aohengcloud-***/aohengcloud-***"
	 sshPut remote: remote, from: 'aohengcloud-***/aohengcloud-***-rest/target/aohengcloud-***-rest-1.0.0-SNAPSHOT-exec.jar', into: '/home/aohengcloud/aohengcloud-***'
    }

    stage('restart aohengcloud-*') {
     echo 'restart aohengcloud-*'     
     sshCommand remote: remote, command: "/home/aohengcloud/aohengcloud-***/start.sh"
     sshCommand remote: remote, command: "pwd" 

    }

	stage('copy888') {
		if(PUBLISH == 'true'){
		  echo 'copy888'      
		  sshCommand remote: remote888, command: "mkdir -p /home/aohengcloud-***/aohengcloud-***"
		  sshPut remote: remote888, from: 'aohengcloud-***/aohengcloud-***-rest/target/aohengcloud-***-rest-1.0.0-SNAPSHOT-exec.jar', into: '/home/aohengcloud-***/aohengcloud-***'
		  sshCommand remote: remote888, command: "cd /home/aohengcloud-***&&docker-compose -f docker-compose.yml up --build --no-deps -d aohengcloud-***"
		}
   }
}


```



## jar启动脚本

start.sh脚本

```
#!/bin/bash
source /etc/profile
NACOS_HOST="192.168.1.888:8848"
NACOS_NAMESPACE="aohengcloud-ALL"
JAVA_OPTS="-Dnacos.config.server-addr=${NACOS_HOST} \
            -Dnacos.config.namespace=${NACOS_NAMESPACE}"
PIDS=`ps -ef |grep  /home/aohengcloud/aohengcloud-* |grep -Ev 'grep|start.sh'| awk '{print $2}' `

for pid in $PIDS
do
  echo $pid
  kill -9 $pid
done
nohup java  -Dlogging.file=/home/aohengcloud/logs/aohengcloud-*  -Xms256m -Xmx768m -Dapp.path=/home/aohengcloud/aohengcloud-* -jar /home/aohengcloud/aohengcloud-*/aohengcloud-*-rest-1.0.0-SNAPSHOT-exec.jar >/home/aohengcloud/aohengcloud-*/aohengcloud-*.log 2>&1 &
exit 0

```



## docker构建脚本

Dockerfile

```
FROM ibm-semeru-runtimes:open-11.0.15_10-jre

ARG APP_NAME="aohengcloud-*-rest"
ARG APP_VERSION="1.0.0-SNAPSHOT-exec"

ENV TZ=Asia/Shanghai \
    APP_NAME=${APP_NAME}

COPY ${APP_NAME}-${APP_VERSION}.jar ${APP_NAME}.jar

CMD java ${JAVA_OPTS} ${JVM_OPTS} -jar ${APP_NAME}.jar
```



## 推送docker仓库

```
export BUILD_PLATFORM=linux/amd64,linux/arm64
APP_VERSION="1.0.4.12"

docker buildx build -f Dockerfile --platform $BUILD_PLATFORM -t harbor.aohengcloud.com/aohengcloud-aohengcloud/aohengcloud-*:${APP_VERSION} -o type=registry ../aohengcloud-*
```

