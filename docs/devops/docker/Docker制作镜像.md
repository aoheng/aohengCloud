# 制作一个超级精简的 Docker 镜像只需7步





**目录**

- 介绍

- 镜像层(Layers)

- 制作步骤

- - lab-1：初始化构建 Redis 镜像
  - lab-2：优化基础镜像
  - lab-3：串联 Dockerfile 指令
  - lab-4：压缩你的镜像
  - lab-5：使用最精简的 base image
  - lab-6：提取动态链接的 .so 文件
  - lab-7：为 Go 应用构建精简镜像

- 总结



![图片](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ed7021b614bc43a1a4549507a5abcf74~tplv-k3u1fbpfcp-zoom-1.image)



**介绍**

前段时间网易蜂巢曾经推出蜂巢 `Logo` T恤，用的正是 Docker 镜像制作，最神奇的是，它最终的镜像大小只有 `585` 字节。

```
$ docker images | grep hub.c.163.com/public/logo
REPOSITORY                      TAG           IMAGE ID         CREATED       SIZE
hub.c.163.com/public/logo       latest        6fbdd13cd204     11 days ago   585 B
```

有些镜像都不是我们自己来打包的（比如下载公共镜像），那是否有一些通用的精简 Docker 镜像的手段呢？答案是肯定的，甚至有的镜像可以精简 98%。精简镜像大小的好处不言而喻，既节省了存储空间，又能节省带宽，加快传输等。那好，接下来就请跟随我来学习怎么制作精简 Docker 镜像。

## 镜像层(Layers)

在开始制作镜像之前，首先了解下镜像的原理，而这其中最重要的概念就是`镜像层(Layers)`。镜像层依赖于一系列的底层技术，比如文件系统(filesystems)、写时复制(copy-on-write)、联合挂载(union mounts)等，幸运的是你可以在很多地方学习到**这些技术**[1]，这里就不再赘述技术细节。

![图片](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/41cd7ecaf5d74c07b08b90bc5c4e3f78~tplv-k3u1fbpfcp-zoom-1.image)

总的来说，你最需要记住这点：

```
在 Dockerfile 中， 每一条指令都会创建一个镜像层，继而会增加整体镜像的大小。
```

举例来说：

```
FROM busybox
RUN mkdir /tmp/foo
RUN dd if=/dev/zero of=/tmp/foo/bar bs=1048576 count=100
RUN rm /tmp/foo/bar
```

以上 Dockerfile 干了几件事：

1. 基于一个官方的基础镜像 busybox(只有1M多)
2. 创建一个文件夹(/tmp/foo)和一个文件(bar)，该文件分配了100M大小
3. 再把这个大文件删除

实际上它最终什么也没做，我们把它构建成镜像（构建可以参考**一期**[2]）：

```
docker build -t busybox:test .
```

再让我们来对比下原生的 busybox 镜像大小和我们生成的镜像大小：

```
$ docker images | grep busybox
busybox    test     896c63dbdb96    2 seconds ago    106 MB
busybox    latest   2b8fd9751c4c    9 weeks ago      1.093 MB
```

出乎意料的是，却生成了 106 MB 的镜像。

多出了 100 M，这是为何？这点和 Git 类似（都用到了Copy-On-Write技术），我用 git 做了如下两次提交（添加了又删除），请问 `A_VERY_LARGE_FILE` 还在 git 仓库中吗？

```
$ git add  A_VERY_LARGE_FILE
$ git commit
$ git rm  A_VERY_LARGE_FILE
$ git commit
```

答案是:在的，并且会占用仓库的大小。Git 会保存每一次提交的文件版本，而 Dockerfile 中每一条指令都可能增加整体镜像的大小，即使它最终什么事情都没做。

## 制作步骤

了解了镜像层知识，有助于我们接下来制作精简镜像。这里开始，以最常用的开源缓存软件 `Redis` 为例，从一步步试验，来介绍如何制作更精简的 Docker 镜像。

### lab-1：初始化构建 Redis 镜像

**直接上 `Dockerfile` ：**

```
FROM ubuntu:trusty
ENV VER     3.0.0
ENV TARBALL http://download.redis.io/releases/redis-$VER.tar.gz
# ==> Install curl and helper tools...
RUN apt-get update
RUN apt-get install -y  curl make gcc
# ==> Download, compile, and install...
RUN curl -L $TARBALL | tar zxv
WORKDIR  redis-$VER
RUN make
RUN make install
#...
# ==> Clean up...
WORKDIR /
RUN apt-get remove -y --auto-remove curl make gcc
RUN apt-get clean
RUN rm -rf /var/lib/apt/lists/*  /redis-$VER
#...
CMD ["redis-server"]
```

结合注释，读起来并不困难，用到的都是常规的几个命令，简要介绍如下：

- FROM：顶头写，指定一个基础镜像，此处基于 `ubuntu:trusty`
- ENV：设置环境变量，这里设置了 `VER` 和 `TARBALL` 两个环境变量
- RUN：最常用的 Dockerfile 指令，用于运行各种命令，这里调用了 8 次 RUN 指令
- WORKDIR：指定工作目录，相当于指令 `cd`
- CMD：指定镜像默认执行的命令，此处默认执行 redis-server 命令来启动 redis

**执行构建：**

```
$ docker build  -t redis:lab-1  .
```

*注：国内网络，更新下载可能会较慢*

**查看大小：**

| Lab  | iamge | Base     | Lang | .red[*] | Size (MB) | Memo        |
| :--: | :---- | :------- | :--: | :-----: | --------: | :---------- |
|  1   | redis | `ubuntu` |  C   |   dyn   |     347.3 | base ubuntu |

动辄就有 300多 M 的大小，不能忍，下面我们开始一步步优化。

### lab-2：优化基础镜像

**精简1：选用更小的基础镜像。**

常用的 Linux 系统镜像一般有 `ubuntu`、`centos`、`debian`，其中`debian` 更轻量，而且够用，对比如下：

```
REPOSITORY          TAG        IMAGE ID         VIRTUAL SIZE
---------------     ------     ------------     ------------
centos              7          214a4932132a     215.7 MB
centos              6          f6808a3e4d9e     202.6 MB
ubuntu              trusty     d0955f21bf24     188.3 MB
ubuntu              precise    9c5e4be642b7     131.9 MB
debian              jessie     65688f7c61c4     122.8 MB
debian              wheezy     1265e16d0c28     84.96 MB
```

替换 `debian:jessie` 作为我们的基础镜像。

**优化 Dockerfile：**

```
FROM debian:jessie

#...
```

**执行构建：**

```
$ docker build  -t redis:lab-2  .
```

**查看大小：**

| Lab  | image | Base     | Lang | .red[*] | Size (MB) | Memo        |
| :--: | :---- | :------- | :--: | :-----: | --------: | :---------- |
|  01  | redis | `ubuntu` |  C   |   dyn   |     347.3 | base ubuntu |
|  02  | redis | `debian` |  C   |   dyn   |     305.7 | base debian |

减少了42M，稍有成效，但并不明显。细心的同学应该发现，只有 122 MB 的 `debian` 基础镜像，构建后增加到了 305 MB，看来这里面肯定有优化的空间，如何优化就要用到我们开头说到的 `Image Layer` 知识了。

### lab-3：串联 Dockerfile 指令

**精简2：串联你的 Dockerfile 指令（一般是 `RUN` 指令）。**

Dockerfile 中的 RUN 指令通过 `&&` 和 `/` 支持将命令串联在一起，有时能达到意想不到的精简效果。

**优化 Dockerfile：**

```
FROM debian:jessie

ENV VER     3.0.0
ENV TARBALL http://download.redis.io/releases/redis-$VER.tar.gz


RUN echo "==> Install curl and helper tools..."  && \
    apt-get update                      && \
    apt-get install -y  curl make gcc   && \
    \
    echo "==> Download, compile, and install..."  && \
    curl -L $TARBALL | tar zxv  && \
    cd redis-$VER               && \
    make                        && \
    make install                && \
    ...
    echo "==> Clean up..."  && \
    apt-get remove -y --auto-remove curl make gcc  && \
    apt-get clean                                  && \
    rm -rf /var/lib/apt/lists/*  /redis-$VER

#...
CMD ["redis-server"]
```

构建：

```
$ docker build  -t redis:lab-3  .
```

查看大小：

| Lab  | Image | Base     | Lang | .red[*] | Size (MB) | Memo         |
| :--: | :---- | :------- | :--: | :-----: | --------: | :----------- |
|  01  | redis | `ubuntu` |  C   |   dyn   |     347.3 | base ubuntu  |
|  02  | redis | `debian` |  C   |   dyn   |     305.7 | base debian  |
|  03  | redis | `debian` |  C   |   dyn   |     151.4 | cmd chaining |

哇！一下子减少了 50%，效果明显啊！这是最常用的一个精简手段了。

### lab-4：压缩你的镜像

**优化3：试着用命令或工具压缩你的镜像。**

docker 自带的一些命令还能协助压缩镜像，比如 `export` 和 `import`

```
$ docker run -d redis:lab-3
$ docker export 71b1c0ad0a2b | docker import - redis:lab-4
```

但麻烦的是需要先将容器运行起来，而且这个过程中你会丢失镜像原有的一些信息，比如：导出端口，环境变量，默认指令。

所以一般通过命令行来精简镜像都是实验性的，那么这里再推荐一个小工具：**docker-squash**[3]。用起来更简单方便，并且不会丢失原有镜像的自带信息。

**下载安装：**

https://github.com/jwilder/docker-squash#installation

**压缩操作：**

```
$ docker save redis:lab-3 \
  | sudo docker-squash -verbose -t redis:lab-4  \
  | docker load
```

*注：该工具在 Mac 下并不好使，请在 Linux 下使用*

**对比大小：**

| Lab  | Image | Base     |  PL  | .red[*] | Size (MB) | Memo          |
| :--: | :---- | :------- | :--: | :-----: | --------: | :------------ |
|  01  | redis | `ubuntu` |  C   |   dyn   |     347.3 | base ubuntu   |
|  02  | redis | `debian` |  C   |   dyn   |     305.7 | base debian   |
|  03  | redis | `debian` |  C   |   dyn   |     151.4 | cmd chaining  |
|  04  | redis | `debian` |  C   |   dyn   |     151.4 | docker-squash |

好吧，从这里看起来并没有太大作用，所以我只能说`试着`，而不要报太大期望。

### lab-5：使用最精简的 base image

![图片](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b380c35da2614478a03778c98ff56f65~tplv-k3u1fbpfcp-zoom-1.image)

使用 `scratch` 或者 `busybox` 作为基础镜像。

关于 scratch：

- 一个空镜像，只能用于构建镜像，通过 `FROM scratch`
- 在构建一些基础镜像，比如 `debian` 、 `busybox`，非常有用
- 用于构建超少镜像，比如构建一个包含所有库的二进制文件

关于 `busybox`

- 只有 1~5M 的大小
- 包含了常用的 UNIX 工具
- 非常方便构建小镜像

这些超小的基础镜像，结合能生成静态原生 ELF 文件的编译语言，比如C/C++，比如 Go，特别方便构建超小的镜像。

cloudcomb-logo（C语言开发） 就是用到了该原理，才能构建出 585 字节的镜像。

`redis` 同样使用 C语言 开发，看来也有很大的优化空间，下面这个实验，让我们介绍具体的操作方法。

### lab-6：提取动态链接的 .so 文件

实验上下文：

```
$ cat /etc/os-release

NAME="Ubuntu"
VERSION="14.04.2 LTS, Trusty Tahr"
$ uname -a
Linux localhost 3.13.0-46-generic #77-Ubuntu SMP
Mon Mar 2 18:23:39 UTC 2015
x86_64 x86_64 x86_64 GNU/Linux
```

隆重推出 ldd：打印共享的依赖库

```
$ ldd  redis-3.0.0/src/redis-server
    linux-vdso.so.1 =>  (0x00007fffde365000)
    libm.so.6 => /lib/x86_64-linux-gnu/libm.so.6 (0x00007f307d5aa000)
    libpthread.so.0 => /lib/x86_64-linux-gnu/libpthread.so.0 (0x00007f307d38c000)
    libc.so.6 => /lib/x86_64-linux-gnu/libc.so.6 (0x00007f307cfc6000)
    /lib64/ld-linux-x86-64.so.2 (0x00007f307d8b9000)
```

将所有需要的 .so 文件打包：

```
$ tar ztvf rootfs.tar.gz
4485167  2015-04-21 22:54  usr/local/bin/redis-server
1071552  2015-02-25 16:56  lib/x86_64-linux-gnu/libm.so.6
 141574  2015-02-25 16:56  lib/x86_64-linux-gnu/libpthread.so.0
1840928  2015-02-25 16:56  lib/x86_64-linux-gnu/libc.so.6
 149120  2015-02-25 16:56  lib64/ld-linux-x86-64.so.2
```

再制作成 Dockerfile：

```
FROM scratch
ADD  rootfs.tar.gz  /
COPY redis.conf     /etc/redis/redis.conf
EXPOSE 6379
CMD ["redis-server"]
```

执行构建：

```
$ docker build  -t redis-05  .
```

查看大小：

| Lab  |       | Base      |  PL  | .red[*] | Size (MB) | Memo          |
| :--: | :---- | :-------- | :--: | :-----: | --------: | :------------ |
|  01  | redis | `ubuntu`  |  C   |   dyn   |     347.3 | base ubuntu   |
|  02  | redis | `debian`  |  C   |   dyn   |     305.7 | base debian   |
|  03  | redis | `debian`  |  C   |   dyn   |     151.4 | cmd chaining  |
|  04  | redis | `debian`  |  C   |   dyn   |     151.4 | docker-squash |
|  05  | redis | `scratch` |  C   |   dyn   |      7.73 | rootfs: .so   |

哇！显著提高啦！[编写 Dockerfile 最佳实践](http://mp.weixin.qq.com/s?__biz=MzAwNTM5Njk3Mw==&mid=2247503126&idx=1&sn=331da918ad86a65351b9b297cf943bcc&chksm=9b1fcd94ac6844824e8820cb6715c853063d3f37eaaccc62454f897b2cc87709e9c0e383a3c2&scene=21#wechat_redirect)

测试一下：

```
$ docker run -d --name redis-05 redis-05

$ redis-cli  -h  \
  $(docker inspect -f '{{.NetworkSettings.IPAddress}}' redis-05)

$ redis-benchmark  -h  \
  $(docker inspect -f '{{.NetworkSettings.IPAddress}}' redis-05)
```

总结一下：

1. 用 `ldd` 查出所需的 .so 文件
2. 将所有依赖压缩成 `rootfs.tar` 或 `rootfs.tar.gz`，之后打进 `scratch` 基础镜像

### lab-7：为 Go 应用构建精简镜像

Go 语言天生就方便用来构建精简镜像，得益于它能方便的打包成包含静态链接的二进制文件。

打个比方，你有一个 4 MB 大小的包含静态链接的 Go 二进制，并且将其打进 scratch 这样的基础镜像，你得到的镜像大小也只有区区的 4 MB。这可是包含同样功能的 Ruby 程序的百分之一啊。

这里再给大家介绍一个非常好用开源的 Go 编译工具：golang-builder，并给大家实际演示一个例子

程序代码：

```
package main // import "github.com/CenturyLinkLabs/hello"

import "fmt"

func main() {
    fmt.Println("Hello World")
}
```

Dockerfile：

```
FROM scratch
COPY hello /
ENTRYPOINT ["/hello"]
```

通过 golang-builder 打包成镜像：

```
docker run --rm \
    -v $(pwd):/src \
    -v /var/run/docker.sock:/var/run/docker.sock \
    centurylink/golang-builder
```

查看镜像大小(Mac下测试)：

```
$ docker images
REPOSITORY   TAG      IMAGE ID       CREATED          VIRTUAL SIZE
hello        latest   1a42948d3224   24 seconds ago   1.59 MB
```

哇！这么省力，就能创建几 M 大小的镜像，Go 简介就是为 Docker 镜像量身定做的！

## 总结

我们介绍了镜像层的知识，并且通过实验，介绍三种如何精简镜像的技巧。这里主要介绍了三种精简方法：选用更精小的镜像，串联 Dockerfile 运行指令，以及试着压缩你的镜像。通过这几个技巧，已经可以将 300M 大小的镜像压缩到 150M，压缩率50%到98%，效果还是不错。

1. 优化基础镜像
2. 串接 Dockerfile 命令：
3. 压缩 Docker images
4. 优化程序依赖
5. 选用更合适的开发语言

