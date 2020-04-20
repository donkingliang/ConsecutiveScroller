package com.donkingliang.consecutivescroller;

import android.view.View;

import java.util.List;

/**
 * @Author teach-梁任彦
 * @Description
 * @Date 2020/4/18
 */
public interface IConsecutiveScroller {

    View getCurrentScrollerView();

    List<View> getScrolledViews();
}
