package com.donkingliang.consecutivescroller;

import android.view.View;

import java.util.List;

/**
 * @Author donkingliang
 * @Description ConsecutiveScrollerLayout默认只会处理它的直接子view的滑动事件，
 * 为了让ConsecutiveScrollerLayout能支持滑动子view的下级view，提供了IConsecutiveScroller接口。
 *
 * 子view实现IConsecutiveScroller接口，并通过实现接口方法告诉ConsecutiveScrollerLayout需要滑动的下级view,
 * ConsecutiveScrollerLayout就能正确地处理它的滑动事件。
 * @Date 2020/4/18
 */
public interface IConsecutiveScroller {

    /**
     * 返回当前需要滑动的下级view。在一个时间点里只能有一个view可以滑动。
     * @return
     */
    View getCurrentScrollerView();

    /**
     * 返回所有可以滑动的子view。由于ConsecutiveScrollerLayout允许它的子view包含多个可滑动的子view，所以返回一个view列表。
      * @return
     */
    List<View> getScrolledViews();
}
