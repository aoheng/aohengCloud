# docker构建jar包镜像

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
    image: kemingheng/xxl-job-admin:2.3.1.pg
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

8.**docker tag :** 标记本地镜像，将其归入某一仓库。

```
docker tag [OPTIONS] IMAGE[:TAG] [REGISTRYHOST/][USERNAME/]NAME[:TAG]

例：docker tag xxl-job-admin:2.3.1.pg kemingheng/xxl-job-admin:2.3.1.pg
```

9.**push**推送镜像

```
docker push kemingheng/xxl-job-admin:2.3.1.pg
```

10.查看容器运行jar包

```
docker exec -it 容器id sh
```

11.导出容器jar到当前目录

```
docker cp 容器id:/**.jar ./
```

