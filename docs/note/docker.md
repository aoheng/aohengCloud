# docker指令

## redis

1.docker启动redis指令：

```java
docker run -p 6379:6379 --name redis -d redis --requirepass "password"
```

2.进入redis

```
docker exec -it redis(容器服务名称) redis-cli
```

3.启动redis，`-d`代表后台启动

```
docker run -d redis
```

4.认证密码

```
auth "password"
```



## nacos

1.启动（standalone表示非集群模式）

```java
startup.cmd -m standalone
```

2.docker启动指令

```
docker run --env MODE=standalone --name nacos -d -p 8848:8848 nacos/nacos-server
```



## nginx

重启nginx三种指令

```
service nginx restart

systemctl restart nginx

/usr/sbin/nginx -s reload
```



## zookeeper

```
docker run -d -e TZ="Asia/Shanghai" -p 2181:2181 -v /data/zookeeper:/data --name zookeeper --restart always zookeeper
```



## 启动容器镜像

1.启动命令

```
docker-compose up -d  CONTAINER_ID(容器ID) 
```

2.开机自启动，映射端口

```
docker run -d -p 2181:2181 --name zk --restart always zookeeper
```



## 查看日志

1.显示n行 

```
docker logs -f -t --tail n  CONTAINER_ID(容器ID) 
```

2.查看最近30分钟的日志

```
docker logs --since 30m CONTAINER_ID(容器ID)
```

3.查看关键字日志

```
docker logs -f --tail n  CONTAINER_ID(容器ID) |grep "关键字"
```





## npm

设置阿里镜像

```
npm install -g cnpm --registry=https://registry.npmmirror.com
```





### 1.Docker镜像打包部署

#### 1.1 切换目录 

```
cd 目录
```



#### 1.2 修改版本

```
vim .env
```



#### 1.3 构建镜像

```
vim docker-build-project.sh
```



#### 1.4 镜像导出***.tar

```
docker save -o ***.tar **:**version
```



#### 1.5 镜像导入***.tar

```
docker load -i **.tar
```



#### 1.6启动容器（远程部署启动）

```
docker-compose -f docker-compose.yml up --no-deps -d 服务名称
```



### 2.导出log日志

```
docker logs -t --since="2022-07-04T16:30:30" --until "2022-07-04T16:38:37"  CONTAINER_ID(容器ID) >> 名称.txt
```









## 安装Loki

Docker Compose 下载

```bash
wget https://raw.githubusercontent.com/grafana/loki/v2.5.0/production/docker-compose.yaml -O docker-compose.yaml
```

启动容器

```
docker-compose -f docker-compose.yaml up -d
```

进入容器

```
docker exec -it 容器ID sh
```





## jar包打包成镜像

1.上传jar包到固定路径

2.编写Dockerfile文件

```shell
FROM openjdk:8-jre-slim
MAINTAINER xuxueli

ENV PARAMS=""

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD xxl-job-admin-2.3.1.jar /xxl-job-admin.jar

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /xxl-job-admin.jar $PARAMS"]
```

3.通过dockerfile构建镜像

```shell
docker build -f Dockerfile文件路径 -t 镜像名称:版本号 .
例如当前路径：docker build -f Dockerfile -t xxl-job-admin:2.3.1.pg .
注意:当前路径后缀有.点
```

4.通过docker images查看构建成功的镜像

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9e83422433b648e9a16af7ec87b189e0~tplv-k3u1fbpfcp-watermark.image?)

5.将镜像文件打成tar包

```shell
docker save -o 压缩文件名称 镜像名称:版本号
例：docker save -o xxl-job-admin-2.3.1.pg.tar xxl-job-admin:2.3.1.pg
```

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8950b81931a344ae8916620d049f7db5~tplv-k3u1fbpfcp-watermark.image?)

至此已经完成镜像打包了

6.解压tar文件，解压后会直接生成该镜像，可通过docker iamges查看

```shell
docker load –i 压缩文件名称
例：docker load -i xxl-job-admin-2.3.1.pg.tar
```

7.启动docker镜像

编写docker-compose.yaml文件

```shell
version: "3.9"
services:
  xxl-job-admin:
    # docker 镜像
    image: xxl-job-admin:2.3.1.pg
    # 容器名称
    container_name: xxl-job-admin
    volumes:
      # 日志目录映射到主机目录
      - /workspace/xxl-job/logs:/data/applogs
    ports:
      # 端口映射
      - "8081:8080"
    environment:
      # 设置启动参数
      PARAMS: "
      --server.port=8080
      --server.servlet.context-path=/xxl-job-admin
      --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
      --spring.datasource.username=postgres
      --spring.datasource.password=postgres"
```

启动命令：

```
docker-compose up -d
```

