# JIoT Client SDK Android 

### 简介

极光 IoT 是极光面向物联网开发者推出的 SaaS 服务平台，依托于极光在开发者服务领域的技术积累能力。专门为 IoT 设备优化协议，提供高并发，高覆盖，高可用的设备接入及消息通信能力。同时针对物联网使用场景提供安全连接，实时统计，设备管理 ，影子设备等一些列解决方案，当前开源项目针对的是Android平台客户端的集成。

### 接入方式

- 通过极光的官方网站注册开发者帐号；
- 登录进入管理控制台，创建应产品，得到 ProductKey（ProductKey 与服务器端通过 Appkey 互相识别）；
- 在产品设置中为产品完善属性设置，上报事件设置。
- 为产品添加设备：定义设备名，并获得分配的设备密钥。
- 下载 SDK 进行集成或者通过SDK中的demo进行调试。



### Android 系统版本支持

兼容 Android 4.0 及以上版本。



### 手动集成步骤

- 解压缩 jiot-android-1.x.x-release.zip 集成压缩包。
- 复制 libs/jiot-android-1.x.x.jar 到工程 libs/ 目录下。

**说明 1**：注意在 module 的 gradle 配置中添加一下配置：

implementation "org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0"

### 

### 配置 AndroidManifest.xml

添加权限

```
<uses-permission android:name="android.permission.INTERNET" />

<uses-permission android:name="android.permission.WAKE_LOCK" />
```



### 集成 JIoT Android SDK 的混淆

- 请下载 4.x 及以上版本的 proguard.jar， 并替换你 Android SDK "tools\proguard\lib\proguard.jar"
- 请在工程的混淆文件中添加以下配置：

```
-dontoptimize
-dontpreverify

-dontwarn cn.jiguang.**
-keep class cn.jiguang.** { *; }
```



### 相关文档

- [JIoT Android SDK 集成指南](https://docs.jiguang.cn/jiot/client/android_sdk_guide/)
- [JIoT Android SDK接口文档](https://docs.jiguang.cn/jiot/client/android_sdk_api/)