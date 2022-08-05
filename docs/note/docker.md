# docker指令



## nginx

重启nginx

```output
sudo systemctl restart nginx
```



## redis

1.docker启动redis指令：

```java
docker run -p 6379:6379 --name redis -d redis --requirepass "password"
```

2.进入redis

```
docker exec -it redis redis-cli
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



## 启动容器镜像

1.启动命令

```
docker-compose up -d  CONTAINER_ID(容器ID) 
```

2.开机自启动，映射端口

```
docker run -d -p 2181:62181 --name zk --restart always zookeeper
```

在启动容器的时候可以通过 -v双向绑定本地的某文件，这样任意修改哪一个都会同步变化`docker run [OPTIONS] IMAGE [COMMAND] [ARG...]`

- -p: 指定端口映射，格式为：主机(宿主)端口:容器端口
- –volume , -v: 绑定一个数据卷
- -d: 后台运行容器，并返回容器ID；
- –name=“redis”: 为容器指定一个名称；
- -e username=“ritchie”: 设置环境变量；
- -m :设置容器使用内存最大值；



## 查看日志

1.显示n行 

```
docker logs -f -t --tail n  CONTAINER_ID(容器ID) 
```

2.查看最近30分钟的日志

```
docker logs --since 30m CONTAINER_ID(容器ID)
```



## npm

设置阿里镜像

```
npm install -g cnpm --registry=https://registry.npmmirror.com
```





## 微信部署步骤

### 1.wx小程序打包部署

#### 1.1 切换目录 

wxdb

#### 1.2 修改版本

```
vim .env
```



#### 1.3 构建镜像

```
vim docker-build-project.sh
```



#### 1.4 保存镜像到本地

```
docker save -o open-api-service-V2.9.16.3.tar harbor.cnhis.com/cnhis-wx/cloud-health-open-api-service:Vhotfix2.9.16.3-RELEASE-RC1
docker save -o wx-service-Vhotfix2.9.16.2.tar harbor.cnhis.com/cnhis-wx/cloud-health-wx-service:Vhotfix2.9.16.2-RELEASE-RC1
docker save -o wx-pay-service-Vhotfix2.9.16.2.tar harbor.cnhis.com/cnhis-wx/cloud-health-wx-pay-service:Vhotfix2.9.16.2-RELEASE-RC1
docker save -o wx-quartz-Vhotfix2.9.16.2.tar harbor.cnhis.com/cnhis-wx/cloud-health-wx-quartz:Vhotfix2.9.16.2-RELEASE-RC1
docker save -o wx-rest-Vhotfix2.9.16.10.tar harbor.cnhis.com/cnhis-wx/cloud-health-wx-rest:Vhotfix2.9.16.10-RELEASE-RC1
```



#### 1.5启动容器（远程部署启动）

```
docker-compose -f docker-compose.yml up --no-deps -d 服务名称（如wx-rest）
```



### 2.导出log日志

```
docker logs -t --since="2022-07-04T16:30:30" --until "2022-07-04T16:38:37"  wx-rest >>logpay3.txt
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

