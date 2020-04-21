package com.donkingliang.consecutivescroller;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/3/17
 */
public class ScrollUtils {

    static int computeVerticalScrollOffset(View view) {
        View scrolledView = getScrolledView(view);
        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollOffset");
            method.setAccessible(true);
            return (int) method.invoke(scrolledView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrolledView.getScrollY();
    }

    static int computeVerticalScrollRange(View view) {
        View scrolledView = getScrolledView(view);
        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollRange");
            method.setAccessible(true);
            return (int) method.invoke(scrolledView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrolledView.getHeight();
    }

    static int computeVerticalScrollExtent(View view) {
        View scrolledView = getScrolledView(view);
        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollExtent");
            method.setAccessible(true);
            return (int) method.invoke(scrolledView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrolledView.getHeight();
    }

    /**
     * 获取View滑动到自身顶部的偏移量
     *
     * @param view
     * @return
     */
    static int getScrollTopOffset(View view) {
        if (isConsecutiveScrollerChild(view) && canScrollVertically(view, -1)) {
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
        if (isConsecutiveScrollerChild(view) && canScrollVertically(view, 1)) {
            return computeVerticalScrollRange(view) - computeVerticalScrollOffset(view)
                    - computeVerticalScrollExtent(view);
        } else {
            return 0;
        }
    }

    /**
     * 是否是可以垂直滚动View。(内容可以滚动，或者本身就是个滚动布局)
     *
     * @param view
     * @return
     */
    static boolean canScrollVertically(View view) {
        return isConsecutiveScrollerChild(view) && (canScrollVertically(view, 1) || canScrollVertically(view, -1));
    }

    /**
     * 判断是否可以滑动
     *
     * @param view
     * @param direction
     * @return
     */
    static boolean canScrollVertically(View view, int direction) {
        View scrolledView = getScrolledView(view);
        if (scrolledView instanceof AbsListView) {
            AbsListView listView = (AbsListView) scrolledView;
            return listView.canScrollList(direction);
        } else {
            return scrolledView.canScrollVertically(direction);
        }
    }

    /**
     * 获取当前触摸点下的View
     *
     * @param rootView
     * @param touchX
     * @param touchY
     * @return
     */
    static List<View> getTouchViews(View rootView, int touchX, int touchY) {
        List views = new ArrayList();
        addTouchViews(views, rootView, touchX, touchY);
        return views;
    }

    private static void addTouchViews(List<View> views, View view, int touchX, int touchY) {
        if (isTouchPointInView(view, touchX, touchY)) {
            views.add(view);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                addTouchViews(views, viewGroup.getChildAt(i), touchX, touchY);
            }
        }
    }

    /**
     * 判断触摸点是否在View内
     *
     * @param view
     * @param x
     * @param y
     * @return
     */
    static boolean isTouchPointInView(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        int left = position[0];
        int top = position[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (x >= left && x <= right && y >= top && y <= bottom) {
            return true;
        }
        return false;
    }

    static int getRawX(View rootView, MotionEvent ev, int pointerIndex) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return (int) ev.getRawX(pointerIndex);
        } else {
            int[] position = new int[2];
            rootView.getLocationOnScreen(position);
            int left = position[0];
            return (int) (left + ev.getX(pointerIndex));
        }
    }

    static int getRawY(View rootView, MotionEvent ev, int pointerIndex) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return (int) ev.getRawY(pointerIndex);
        } else {
            int[] position = new int[2];
            rootView.getLocationOnScreen(position);
            int top = position[1];
            return (int) (top + ev.getY(pointerIndex));
        }
    }

    static List<Integer> getScrollOffsetForViews(List<View> views) {
        List<Integer> offsets = new ArrayList<>();
        for (View view : views) {
            offsets.add(computeVerticalScrollOffset(view));
        }
        return offsets;
    }

    static boolean equalsOffsets(List<Integer> offsets1, List<Integer> offsets2) {
        if (offsets1.size() != offsets2.size()) {
            return false;
        }

        int size = offsets1.size();

        for (int i = 0; i < size; i++) {
            if (!offsets1.get(i).equals(offsets2.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断View是否是支持连续滚动的
     *
     * @param view
     * @return
     */
    static boolean isConsecutiveScrollerChild(View view) {

        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();

            if (lp instanceof ConsecutiveScrollerLayout.LayoutParams) {
                return ((ConsecutiveScrollerLayout.LayoutParams) lp).isConsecutive;
            }
            return true;
        }
        return false;
    }

    /**
     * 判断是否是item复用的view(RecyclerView、AbsListView)
     * @param view
     * @return
     */
    static boolean isRecyclerLayout(View view) {
        return view instanceof RecyclerView || view instanceof AbsListView;
    }

    /**
     * 返回需要滑动的view，如果没有，就返回本身。
     * @param view
     * @return
     */
    static View getScrolledView(View view) {
        if (view instanceof IConsecutiveScroller) {
            View scrolledView = ((IConsecutiveScroller) view).getCurrentScrollerView();
            if (scrolledView != null) {
                return scrolledView;
            }
        }
        return view;
    }

}
