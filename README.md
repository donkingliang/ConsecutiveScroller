## 简介

ConsecutiveScrollerLayout是Android下支持多个滑动布局(RecyclerView、ViewPager、WebView、ScrollView等)和普通控件(TextView、ImageView、LinearLayou、自定义View等)持续连贯滑动的容器,它使所有的子View像一个整体一样连续顺畅滑动。

ConsecutiveScrollerLayout支持多种模式的布局吸顶功能，能动态控制吸顶View的显示位置和状态，能适用于大部分的业务场景。

ConsecutiveScrollerLayout能通过实现接口，支持复杂的、多层嵌套下的滑动布局的滑动处理。

ConsecutiveScrollerLayout支持NestedScrolling机制。

## 效果图

![sample](https://upload-images.jianshu.io/upload_images/2365010-1d0ebf289428ce8c.gif?imageMogr2/auto-orient/strip) 
![sticky](https://upload-images.jianshu.io/upload_images/2365010-f2b64d20022d2566.gif?imageMogr2/auto-orient/strip)
![permanent_sticky](https://github.com/donkingliang/ConsecutiveScroller/blob/master/image/permanent_sticky.gif?raw=true)
![sink_sticky](https://github.com/donkingliang/ConsecutiveScroller/blob/master/image/sink_sticky.gif?raw=true)
![viewpager](https://github.com/donkingliang/ConsecutiveScroller/blob/master/image/viewpager.gif?raw=true)

## 引入依赖

在Project的build.gradle在添加以下代码

```groovy
allprojects {
      repositories {
         ...
         maven { url 'https://jitpack.io' }
      }
   }
```
在Module的build.gradle在添加以下代码
```groovy

implementation 'com.github.donkingliang:ConsecutiveScroller:4.6.4'
```

## 使用文档

查看文档请移步 [wiki](https://github.com/donkingliang/ConsecutiveScroller/wiki)

## LICENSE

<img alt="Apache-2.0 license" src="https://www.apache.org/img/ASF20thAnniversary.jpg" width="128">

ConsecutiveScroller 基于 Apache-2.0 协议进行分发和使用，更多信息参见 [协议文件](LICENSE)。
