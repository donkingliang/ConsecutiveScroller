package com.donkingliang.consecutivescroller;

import android.view.View;
import android.widget.AbsListView;

import java.lang.reflect.Method;

/**
 * @Author teach liang
 * @Description
 * @Date 2020/3/17
 */
public class ScrollUtils {

    static int computeVerticalScrollOffset(View view) {

        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollOffset");
            method.setAccessible(true);
            return (int) method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view.getScrollY();
    }

    static int computeVerticalScrollRange(View view) {

        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollRange");
            method.setAccessible(true);
            return (int) method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view.getHeight();
    }

    static int computeVerticalScrollExtent(View view) {

        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollExtent");
            method.setAccessible(true);
            return (int) method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view.getHeight();
    }

    /**
     * 获取View滑动到自身顶部的偏移量
     *
     * @param view
     * @return
     */
    static int getScrollTopOffset(View view) {
        if (canScrollVertically(view, -1)) {
            return -computeVerticalScrollOffset(view);
        } else {
            return 0;
        }
    }

    /**
     * 获取View滑动到自身底部的偏移量
     *
     * @param view
     * @return
     */
    static int getScrollBottomOffset(View view) {
        if (canScrollVertically(view, 1)) {
            return computeVerticalScrollRange(view) - computeVerticalScrollOffset(view)
                    - computeVerticalScrollExtent(view);
        } else {
            return 0;
        }
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    static boolean canScrollVertically(View view, int direction) {
        if (view instanceof AbsListView) {
            AbsListView listView = (AbsListView) view;
            return listView.canScrollList(direction);
        } else {
            return view.canScrollVertically(direction);
        }
    }
}
