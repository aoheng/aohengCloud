# Grafana报警配置

##### 1：配置报警通知渠道

有各种通知类型包括DingDing,Email,Webhook等，这里使用Webhook进行演示

如图，添加默认的Notification channels，settings栏目勾选Default选项

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f01e1a3f4a6b45119b3fb0b2586f7544~tplv-k3u1fbpfcp-watermark.image?)

###### 1.1:url地址由来

1：登录报警后台： 

2：添加配置报警项目，类型选择运维监控

3：上图ProjectId = 317对应的信息如图所示，配置到对应的飞书通知群，如需电话或者短信通知可在报警后台配置相关联系人

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ba5bbfc5844d41fb9682007e5c05df59~tplv-k3u1fbpfcp-watermark.image?)

##### 2:监控面板-添加对应的Alert信息

案例如下，可查看uid facade服务的配置:

Message文本框：为对应飞书接收到的报警提示信息

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d1a484c6bfb04c06952b397c7e739991~tplv-k3u1fbpfcp-watermark.image?)

###### 2.1:Alert配置图解

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e84a56f024174713ae9a40b800e74d98~tplv-k3u1fbpfcp-watermark.image?)



### 2.2 tps监控设置

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3758a2227fa64b56847d55360a03ac34~tplv-k3u1fbpfcp-watermark.image?)

监控图展示，可看到各个服务的tps监控

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c1f72b7c28144a2f93ee670f0db74e43~tplv-k3u1fbpfcp-watermark.image?)



### 2.3 K8s监控设置

CPU占用比率，磁盘读写率，资源服务器总览等信息

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d9c62fbdb60f45239c907d786328b605~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5a22ce852d784b3091de99b679a5a33c~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f3795c6e76944108a6fd15f0b1610368~tplv-k3u1fbpfcp-watermark.image?)



配置信息

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9469e2186232482586e1279a428ea414~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d6d27b850e34419f9707e9ee48f277e8~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/03b7f658a234467ca8e1caf4d872adc4~tplv-k3u1fbpfcp-watermark.image?)

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aca7b023ccad4c3795cd276fe751b356~tplv-k3u1fbpfcp-watermark.image?)



##### 3.报警提示结果

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0d281092c34d40b590e0d01f262a18a0~tplv-k3u1fbpfcp-watermark.image?)