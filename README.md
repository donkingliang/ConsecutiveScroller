ConsecutiveScrollerLayout是Android下支持多个滑动布局(RecyclerView、WebView、ScrollView等)和lView等)和普通控件(TextView、ImageView、LinearLayou、自定义View等)持续连贯滑动的容器,它使所有的子View像一个整体一样连续顺畅滑动。并且支持布局吸顶功能。

#### 效果图

![sample](https://upload-images.jianshu.io/upload_images/2365010-1d0ebf289428ce8c.gif?imageMogr2/auto-orient/strip) 
![sticky](https://upload-images.jianshu.io/upload_images/2365010-f2b64d20022d2566.gif?imageMogr2/auto-orient/strip)

### 引入依赖

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
// 使用了Androidx
implementation 'com.github.donkingliang:ConsecutiveScroller:4.4.1'

// 或者

// 使用Android support包
implementation 'com.github.donkingliang:ConsecutiveScroller:4.4.1-support'
```

**注意：** 如果你准备使用这个库，请务必认真阅读下面的文档。它能让你了解ConsecutiveScrollerLayout可以实现的功能，以及避免不必要的错误。

### 基本使用

ConsecutiveScrollerLayout的使用非常简单，把需要滑动的布局作为ConsecutiveScrollerLayout的直接子View即可。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:background="@android:color/holo_red_dark"
        android:gravity="center"
        android:orientation="vertical">

    </LinearLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </androidx.core.widget.NestedScrollView>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:scaleType="fitXY"
        android:src="@drawable/temp" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </ScrollView>

  <!--  可以嵌套ConsecutiveScrollerLayout  -->
  <com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/design_default_color_primary">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="10dp"
            android:text=""
            android:textColor="@android:color/black"
            android:textSize="18sp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView3"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

#### 关于margin

ConsecutiveScrollerLayout支持左右margin，但是不支持上下margin，子View间的间距可以通过Space设置。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <!--  使用Space设置上下边距   -->
    <Space
        android:layout_width="0dp"
        android:layout_height="20dp" />

    <!--  ConsecutiveScrollerLayout支持左右margin，但是不支持上下margin   -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_marginLeft="20dp"
        android:layout_marginRight="20dp"
        android:background="@android:color/holo_red_dark"
        android:gravity="center"
        android:orientation="vertical">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="LinearLayout"
            android:textColor="@android:color/black"
            android:textSize="18sp" />

    </LinearLayout>

    <!--  使用Space设置上下边距   -->
    <Space
        android:layout_width="0dp"
        android:layout_height="20dp" />

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>

```

#### 布局对齐方式

ConsecutiveScrollerLayout的布局方式类似于垂直的LinearLayout，但是它没有gravity和子view layout_gravity属性。ConsecutiveScrollerLayout为它的子view提供了layout_align属性，用于设置子view和父布局的对齐方式。layout_align有三个值：**左对齐(LEFT)** 、**右对齐(RIGHT)** 和**中间对齐(CENTER)**。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"                                                                    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">
  
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:padding="10dp"
        android:text="吸顶View - 1"
        android:textColor="@android:color/black"
        android:textSize="18sp"
        app:layout_isSticky="true"
        app:layout_align="LEFT"/> // 对齐方式
  
</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

#### 嵌套Fragment

要想把一个Fragment嵌套在ConsecutiveScrollerLayout里。通常我们需要一个布局容器来承载我们的Fragment，或者直接把Fragment写在activity的布局里。如果Fragment是垂直滑动的，那么承载Fragment的容器需要是ConsecutiveScrollerLayout，以及Fragment的根布局也需要是垂直滑动的。我们推荐使用ConsecutiveScrollerLayout或者其他可垂直滑动的控件(比如：RecyclerView、NestedScrollView)作为Fragment的根布局。如果你的Fragment不是垂直滑动的，则可以忽略这个限制。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <!--  承载Fragment的容器  -->
    <com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout
        android:id="@+id/fragment_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
  
<!--  MyFragment的根布局是垂直滑动控件  -->
   <fragment
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       android:name="com.donkingliang.consecutivescrollerdemo.MyFragment"/>

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

### 布局吸顶

要实现布局的吸顶效果，在以前，我们可能会写两个一摸一样的布局，一个隐藏在顶部，一个嵌套在ScrollView下，通过监听ScrollView的滑动来显示和隐藏顶部的布局。这种方式实现起来既麻烦也不优雅。ConsecutiveScrollerLayout内部实现了子View吸附顶部的功能，只要设置一个属性，就可以实现吸顶功能。而且支持设置多个子View吸顶，后面的View要吸顶时会把前面的吸顶View推出屏幕。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

  <!-- 设置app:layout_isSticky="true"就可以使View吸顶 -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:padding="10dp"
        android:text="吸顶View - 1"
        android:textColor="@android:color/black"
        android:textSize="18sp"
        app:layout_isSticky="true" />

    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:orientation="vertical"
        app:layout_isSticky="true">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="10dp"
            android:text="吸顶View - 2 我是个LinearLayout"
            android:textColor="@android:color/black"
            android:textSize="18sp" />

    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:padding="10dp"
        android:text="吸顶View - 3"
        android:textColor="@android:color/black"
        android:textSize="18sp"
        app:layout_isSticky="true" />

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </androidx.core.widget.NestedScrollView>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:padding="10dp"
        android:text="吸顶View - 4"
        android:textColor="@android:color/black"
        android:textSize="18sp"
        app:layout_isSticky="true" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

#### 常驻吸顶

如果你不希望吸顶的view被后面的吸顶view顶出屏幕，而且多个吸顶view排列吸附在顶部，你可以设置常驻吸顶模式：**app:isPermanent="true"**。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:isPermanent="true"  // 常驻吸顶
    android:scrollbars="vertical">

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

#### 关于吸顶功能的其他方法

```java
// 设置吸顶到顶部的距离，在距离顶部一定距离时开始悬停吸顶
scrollerLayout.setStickyOffset(50);

// 监听吸顶变化(普通模式)
scrollerLayout.setOnStickyChangeListener(OnStickyChangeListener);
// 监听吸顶变化(常驻模式)
scrollerLayout.setOnPermanentStickyChangeListener(OnPermanentStickyChangeListener);
// 获取当前吸顶view(普通模式)
scrollerLayout.getCurrentStickyView(); 
// 获取当前吸顶view(常驻模式)
scrollerLayout.getCurrentStickyViews();
// 设置吸顶常驻模式
public void setPermanent(boolean isPermanent);
// 判断子view是否处于吸顶状态
public boolean theChildIsStick(View child);
// 判断子view是否是吸顶view
public boolean isStickyView(View child);

/**
 * 在View吸顶的状态下，是否可以触摸view来滑动ConsecutiveScrollerLayout布局。
 * 默认为false，则View吸顶的状态下，不能触摸它来滑动布局
 */
app:layout_isTriggerScroll="true"

/**
 * 吸顶下沉模式
 * 默认情况下，吸顶view在吸顶状态下，会显示在布局上层，覆盖其他布局。
 * 如果设置了下沉模式，则会相反，view在吸顶时会显示在下层，被其他布局覆盖，隐藏在下面。
 * 实现的效果可参考demo中的例子
 */
app:layout_isSink="true";

```

### 局部滑动

ConsecutiveScrollerLayout将所有的子View视作一个整体，由它统一处理页面的滑动事件，所以它默认会拦截可滑动的子View的滑动事件，由自己来分发处理。并且会追踪用户的手指滑动事件，计算调整ConsecutiveScrollerLayout滑动偏移。如果你希望某个子View自己处理自己的滑动事件，可以通过设置layout_isConsecutive属性来告诉父View不要拦截它的滑动事件，这样就可以实现在这个View自己的高度内滑动自己的内容，而不会作为ConsecutiveScrollerLayout的一部分来处理。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">
  
<!--设置app:layout_isConsecutive="false"使父布局不拦截滑动事件-->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_isConsecutive="false">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="10dp"
            android:text="下面的红色区域是个RecyclerView，它在自己的范围内单独滑动。"
            android:textColor="@android:color/black"
            android:textSize="18sp"
            app:layout_isSticky="true" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView1"
            android:layout_width="match_parent"
            android:layout_height="300dp"
            android:layout_marginLeft="50dp"
            android:layout_marginRight="50dp"
            android:layout_marginBottom="30dp"
            android:background="@android:color/holo_red_dark"/>

    </LinearLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="10dp"
        android:text="下面的是个NestedScrollView，它在自己的范围内单独滑动。"
        android:textColor="@android:color/black"
        android:textSize="18sp" />

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="250dp"
        app:layout_isConsecutive="false">

    </androidx.core.widget.NestedScrollView>

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

在这个例子中NestedScrollView希望在自己的高度里滑动自己的内容，而不是跟随ConsecutiveScrollerLayout滑动，只要给它设置layout_isConsecutive="false"就可以了。而LinearLayout虽然不是滑动布局，但是在下面嵌套了个滑动布局RecyclerView，所以它也需要设置layout_isConsecutive="false"。

ConsecutiveScrollerLayout支持NestedScrolling机制，如果你的局部滑动的view实现了NestedScrollingChild接口(如：RecyclerView、NestedScrollView等)，它滑动完成后会把滑动事件交给父布局。如果你不想你的子view或它的下级view与父布局嵌套滑动，可以给子view设置app:layout_isNestedScroll="false"。它可以禁止子view与ConsecutiveScrollerLayout的嵌套滑动

### 滑动子view的下级view

ConsecutiveScrollerLayout默认情况下只会处理它的直接子view的滑动，但有时候需要滑动的布局可能不是ConsecutiveScrollerLayout的直接子view，而是子view所嵌套的下级view。比如ConsecutiveScrollerLayout嵌套FrameLayout,FrameLayout嵌套ScrollView，我们希望ConsecutiveScrollerLayout也能正常处理ScrollView的滑动。为了支持这种需求，ConsecutiveScroller提供了一个接口：IConsecutiveScroller。子view实现IConsecutiveScroller接口，并通过实现接口方法告诉ConsecutiveScrollerLayout需要滑动的下级view,ConsecutiveScrollerLayout就能正确地处理它的滑动事件。IConsecutiveScroller需要实现两个方法：

```java
    /**
     * 返回当前需要滑动的下级view。在一个时间点里只能有一个view可以滑动。
     */
    View getCurrentScrollerView();

    /**
     * 返回所有可以滑动的子view。由于ConsecutiveScrollerLayout允许它的子view包含多个可滑动的子view，所以返回一个view列表。
     */
    List<View> getScrolledViews();
```

在前面提到的例子中，我们可以这样实现：

```java
public class MyFrameLayout extends FrameLayout implements IConsecutiveScroller {

    @Override
    public View getCurrentScrollerView() {
        // 返回需要滑动的ScrollView
        return getChildAt(0);
    }

    @Override
    public List<View> getScrolledViews() {
        // 返回需要滑动的ScrollView
        List<View> views = new ArrayList<>();
        views.add(getChildAt(0));
        return views;
    }
}
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <com.donkingliang.consecutivescrollerdemo.widget.MyFrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent">

            </LinearLayout>
        </ScrollView>
    </com.donkingliang.consecutivescrollerdemo.widget.MyFrameLayout>
</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

这样ConsecutiveScrollerLayout就能正确地处理ScrollView的滑动。这是一个简单的例子，在实际的需求中，我们一般不需要这样做。

**注意：**

1、getCurrentScrollerView()和getScrolledViews()必须正确地返回需要滑动的view，这些view可以是经过多层嵌套的，不一定是直接子view。所以使用者应该按照自己的实际场景去实现者两个方法。

2、滑动的控件应该跟嵌套它的子view的高度保持一致，也就是说滑动的控件高度应该是match_parent，并且包裹它的子view和它本身都不要设置上下边距(margin和ppadding)。宽度没有这个限制。

#### 对ViewPager的支持

如果你的ViewPager承载的子布局(或Fragment)不是可以垂直滑动的，那么使用普通的ViewPager即可。如果是可以垂直滑动的，那么你的ViewPager需要实现IConsecutiveScroller接口，并返回需要滑动的view对象。框架里提供了一个实现了IConsecutiveScroller接口自定义控件：**ConsecutiveViewPager**。使用这个控件，然后你的ConsecutiveViewPager的子view(或Fragment的根布局)是可垂直滑动的view，如：RecyclerView、NestedScrollView、ConsecutiveScrollerLayout即可。这样你的ViewPager就能正确地跟ConsecutiveScrollerLayout一起滑动了。

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        app:tabGravity="fill"
        app:tabIndicatorColor="@color/colorPrimary"
        app:tabIndicatorHeight="3dp"
        app:tabMode="scrollable"
        app:tabSelectedTextColor="@color/colorPrimary" />

    <com.donkingliang.consecutivescroller.ConsecutiveViewPager
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

布局吸顶时会覆盖在下面的布局的上面，有时候我们希望TabLayout吸顶悬浮在顶部，但是不希望它覆盖遮挡ViewPager的内容。ConsecutiveViewPager提供了setAdjustHeight调整自己的布局高度，让自己不被TabLayout覆盖。注意：只有ConsecutiveScrollerLayout是ConsecutiveScrollerLayout的最低部时才能这样做。

```java
// 保证能获取到tabLayout的高度
tabLayout.post(new Runnable() {
    @Override
    public void run() {
        viewPager.setAdjustHeight(tabLayout.getHeight());
    }
});
```

#### 对ViewPager2的支持

从4.4.0版本开始，支持在ConsecutiveScrollerLayout中使用ViewPager2。跟ViewPager一样，框架里专门提供了一个ViewPage2的自定义控件：**ConsecutiveViewPager2**。你必须使用它，而不能直接使用Androidx里的ViewPager2。不过你要使用它，依然需要引入ViewPager2依赖。

```groovy
// xxx：viewpager2版本号
implementation 'androidx.viewpager2:viewpager2:xxx'
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/scrollerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        app:tabGravity="fill"
        app:tabIndicatorColor="@color/colorPrimary"
        app:tabIndicatorHeight="3dp"
        app:tabMode="scrollable"
        app:tabSelectedTextColor="@color/colorPrimary" />

    <com.donkingliang.consecutivescroller.ConsecutiveViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout>
```

ConsecutiveViewPager2提供了跟ConsecutiveViewPager一样的功能，同时也支持setAdjustHeight方法，使用的限制也与ConsecutiveViewPager一样，你可以阅读上一节：**对ViewPager的支持**了解。

不过ConsecutiveViewPager2并不是ViewPager2的子类(ViewPager2不允许继承)，而是一个包含ViewPager2的控件，所以你不能把ConsecutiveViewPager2当作ViewPager2。但是ConsecutiveViewPager2提供了跟ViewPager2一样的常用方法，而且提供了获取ViewPager2对象的方法：

```java
public ViewPager2 getViewPager2();
```

所以你完全可以像使用ViewPager2一样使用它。

**注意事项：**

1、ConsecutiveViewPager2只能作为ConsecutiveScrollerLayout的子view，中间不能嵌套其他层级。

2、不要给ConsecutiveViewPager2设置padding。

3、ConsecutiveViewPager2不支持垂直翻页。也就是说，它只能跟ViewPager一样水平翻页和支持item垂直滑动。

### 使用腾讯x5的WebView

由于腾讯x5的VebView是一个FrameLayout嵌套WebView的布局，而不是一个WebView的子类，所以要在ConsecutiveScrollerLayout里使用它，需要把它的滑动交给它里面的WebView。自定义MyWebView继承腾讯的WebView,重写它的scrollBy()方法即可。

```java
public class MyWebView extends com.tencent.smtt.sdk.WebView {

    @Override
    public void scrollBy(int x, int y) {
       // 把滑动交给它的子view
        getView().scrollBy(x, y);
    }
}
```

通过实现IConsecutiveScroller接口同样可以实现对x5的WebView支持。

```java
public class MyWebView extends com.tencent.smtt.sdk.WebView implements IConsecutiveScroller {

    @Override
    public View getCurrentScrollerView() {
        return getView();
    }

    @Override
    public List<View> getScrolledViews() {
        List<View> views = new ArrayList<>();
        views.add(getView());
        return views;
    }
   
}
```

另外需要隐藏它的子view的滚动条

```java
View view = webView.getView();
view.setVerticalScrollBarEnabled(false);
view.setHorizontalScrollBarEnabled(false);
view.setOverScrollMode(OVER_SCROLL_NEVER);
```

### 使用SmartRefreshLayout

SmartRefreshLayout和SwipeRefreshLayout等刷新控件可以嵌套ConsecutiveScrollerLayout实现下拉刷新功能，但是ConsecutiveScrollerLayout内部嵌套它们来刷新子view，因为子view是ConsecutiveScrollerLayout滑动内容等一部分。除非你给SmartRefreshLayout或者SwipeRefreshLayout设置app:layout_isConsecutive="false"。

如果你在页面ConsecutiveScrollerLayout嵌套Fragment，你的Fragment里不能使用SmartRefreshLayout，只能在外面的ConsecutiveScrollerLayout外嵌套SmartRefreshLayout，因为这里的Fragment是ConsecutiveScrollerLayout的一部分，不能在ConsecutiveScrollerLayout内部使用SmartRefreshLayout。这时如果你想在Fragment里使用上拉加载功能，可以将外部的SmartRefreshLayout的上拉加载回调通知给Fragment。我在demo中的ViewPagerActivity有提供一个示例，你也可以根据自己的具体业务实现。

如果你使用了吸顶功能，SmartRefreshLayout上拉布局时也会把吸顶View推上去。可以使用下面的方式，让布局上拉时，吸顶view也能固定在顶部。

```java
refreshLayout.setOnMultiPurposeListener(new SimpleMultiPurposeListener() {
    @Override
    public void onFooterMoving(RefreshFooter footer, boolean isDragging, float percent, int offset, int footerHeight, int maxDragHeight) {
        // 上拉加载时，保证吸顶头部不被推出屏幕。
        // 如果你本身就设置了吸顶偏移量，那么这里的offset计算你的偏移量加offset
        scrollerLayout.setStickyOffset(offset);
    }
});
```

### 其他常用方法

```java

// 修改子view的LayoutParams属性，LayoutParams属性对应xml中的app:layout_属性
ConsecutiveScrollerLayout.LayoutParams lp = (ConsecutiveScrollerLayout.LayoutParams)view.getLayoutParams();
// 修改各种属性
lp.isConsecutive = false;
lp.isSticky = false;
view.setLayoutParams(lp);

// 滑动到指定view的位置，可以设置一个位置偏移量
public void scrollToChild(View view);
public void scrollToChildWithOffset(View view, int offset);
public void smoothScrollToChild(View view);
public void smoothScrollToChildWithOffset(View view, int offset);

// 判断是否滑动到顶部
public boolean isScrollTop();
// 判断是否滑动到底部
public boolean isScrollBottom();

// 在fling的情况下停止布局滑动
public void stopScroll();

//监听滑动
public void setOnVerticalScrollChangeListener(OnScrollChangeListener l);
```

### 其他注意事项

1、WebView在加载的过程中如果滑动的布局，可能会导致WebView与其他View在显示上断层，使用下面的方法一定程度上可以避免这个问题。

```java
webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                scrollerLayout.checkLayoutChange();
            }
        });
```

2、如果你的RecyclerView Item大小不是固定，而是在滑动时动态变化的，可能会引起滑动时页面跳动，你可以给RecyclerView设置tag: android:tag="InterceptRequestLayout"，ConsecutiveScrollerLayout内部会对具有这个tag的RecyclerView做一些处理。如果你的RecyclerView不存在这种问题，不要加这个tag，因为它会影响性能。

3、继承AbsListView的布局(ListView、GridView等)，在滑动上可能会与用户的手指滑动不同步，推荐使用RecyclerView代替。

4、ConsecutiveScroller的minSdkVersion是19，如果你的项目支持19以下，可以设置：

```xml
<uses-sdk tools:overrideLibrary="com.donkingliang.consecutivescroller"/>
```

但是不要在minSdkVersion小于19的项目使用AbsListView的子类，因为ConsecutiveScrollerLayout使用了只有19以上才有的AbsListView API。

5、使用ConsecutiveScrollerLayout提供的setOnVerticalScrollChangeListener()方法监听布局的滑动事件。View所提供的setOnScrollChangeListener()方法已无效。

6、通过getOwnScrollY()方法获取ConsecutiveScrollerLayout的整体垂直滑动距离，这个滑动距离包含了ConsecutiveScrollerLayout本身和所有子view的滑动距离之和。View的getScrollY()方法获取的是ConsecutiveScrollerLayout本身的滑动距离。

7、如果嵌套WebView，请确保WebView加载的网页是符合移动端的，则网页的body高度就是显示的内容高度。否则显示不全。[#109](https://github.com/donkingliang/ConsecutiveScroller/issues/109)

8、4.4.0以下版本，如果使用水平滑动的RecyclerView，需要使用FrameLayout包裹一下，不用让它作为ConsecutiveScrollerLayout的子view，否则可能会有滑动冲突。这是个bug，在4.4.0时已修复。
