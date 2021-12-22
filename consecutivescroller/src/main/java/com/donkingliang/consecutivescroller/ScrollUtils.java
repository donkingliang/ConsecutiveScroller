package com.donkingliang.consecutivescroller;

import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;
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

    static Method computeVerticalScrollOffsetMethod;
    static Method computeVerticalScrollRangeMethod;
    static Method computeVerticalScrollExtentMethod;

    static int computeVerticalScrollOffset(View view) {
        View scrolledView = getScrolledView(view);

        if (scrolledView instanceof ScrollingView) {
            return ((ScrollingView) scrolledView).computeVerticalScrollOffset();
        }

        try {
            if (computeVerticalScrollOffsetMethod == null){
                computeVerticalScrollOffsetMethod = View.class.getDeclaredMethod("computeVerticalScrollOffset");
                computeVerticalScrollOffsetMethod.setAccessible(true);
            }
            Object o = computeVerticalScrollOffsetMethod.invoke(scrolledView);
            if (o != null){
                return (int) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrolledView.getScrollY();
    }

    static int computeVerticalScrollRange(View view) {
        View scrolledView = getScrolledView(view);

        if (scrolledView instanceof ScrollingView) {
            return ((ScrollingView) scrolledView).computeVerticalScrollRange();
        }

        try {
            if (computeVerticalScrollRangeMethod == null) {
                computeVerticalScrollRangeMethod = View.class.getDeclaredMethod("computeVerticalScrollRange");
                computeVerticalScrollRangeMethod.setAccessible(true);
            }
            Object o = computeVerticalScrollRangeMethod.invoke(scrolledView);
            if (o != null){
                return (int) o;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrolledView.getHeight();
    }

    static int computeVerticalScrollExtent(View view) {
        View scrolledView = getScrolledView(view);

        if (scrolledView instanceof ScrollingView) {
            return ((ScrollingView) scrolledView).computeVerticalScrollExtent();
        }

        try {
            if (computeVerticalScrollExtentMethod == null){
                computeVerticalScrollExtentMethod = View.class.getDeclaredMethod("computeVerticalScrollExtent");
                computeVerticalScrollExtentMethod.setAccessible(true);
            }

            Object o = computeVerticalScrollExtentMethod.invoke(scrolledView);
            if (o != null){
                return (int) o;
            }
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
            return Math.min(-computeVerticalScrollOffset(view), -1);
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
            return Math.max(computeVerticalScrollRange(view) - computeVerticalScrollOffset(view)
                    - computeVerticalScrollExtent(view), 1);
        } else {
            return 0;
        }
    }

    /**
     * 是否是可以水平滚动View。(内容可以滚动，或者本身就是个滚动布局)
     *
     * @param view
     * @return
     */
    static boolean canScrollHorizontally(View view) {
        return isConsecutiveScrollerChild(view) && (view.canScrollHorizontally(1) || view.canScrollHorizontally(-1));
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

    private static final Rect mBounds = new Rect();

    /**
     * 判断是否可以滑动
     *
     * @param view
     * @param direction
     * @return
     */
    static boolean canScrollVertically(View view, int direction) {
        View scrolledView = getScrolledView(view);

        if (scrolledView.getVisibility() == View.GONE) {
            // 隐藏状态，不能滑动
            return false;
        }

        if (scrolledView instanceof AbsListView) {
            AbsListView listView = (AbsListView) scrolledView;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return listView.canScrollList(direction);
            } else {
                // 低版本手机(android 19以下)，AbsListView不支持滑动
                return false;
            }
        } else {
            // RecyclerView通过canScrollVertically方法判断滑动到边界不准确，需要单独处理
            if (scrolledView instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) scrolledView;

                if (recyclerView.canScrollHorizontally(1) || recyclerView.canScrollHorizontally(-1)) {
                    // 如果recyclerView可以水平滑动，并且使用canScrollVertically判断不能垂直滑动，这认定是不能垂直滑动的。
                    // 这样做既兼顾了recyclerView同时水平、垂直滑动的情况，又保证了垂直滑动的判断是通过自定义的方式判断的。
                    if (!recyclerView.canScrollVertically(direction)) {
                        return false;
                    }
                }

                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                RecyclerView.Adapter adapter = recyclerView.getAdapter();

                if (layoutManager != null && adapter != null && adapter.getItemCount() > 0) {
                    View itemView = layoutManager.findViewByPosition(direction > 0 ? adapter.getItemCount() - 1 : 0);
                    if (itemView == null) {
                        return true;
                    }
                } else {
                    return false;
                }

                int count = recyclerView.getChildCount();
                if (direction > 0) {
                    for (int i = count - 1; i >= 0; i--) {
                        View child = recyclerView.getChildAt(i);
                        recyclerView.getDecoratedBoundsWithMargins(child, mBounds);
                        if (mBounds.bottom > recyclerView.getHeight() - recyclerView.getPaddingBottom()) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    for (int i = 0; i < count; i++) {
                        View child = recyclerView.getChildAt(i);
                        recyclerView.getDecoratedBoundsWithMargins(child, mBounds);
                        if (mBounds.top < recyclerView.getPaddingTop()) {
                            return true;
                        }
                    }
                    return false;
                }
            }

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
        List<View> views = new ArrayList<>();
        addTouchViews(views, rootView, touchX, touchY);
        return views;
    }

    private static void addTouchViews(List<View> views, View view, int touchX, int touchY) {
        if (isConsecutiveScrollerChild(view) && isTouchPointInView(view, touchX, touchY)) {
            views.add(view);

            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int count = viewGroup.getChildCount();
                for (int i = 0; i < count; i++) {
                    addTouchViews(views, viewGroup.getChildAt(i), touchX, touchY);
                }
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
     * 返回需要滑动的view，如果没有，就返回本身。
     *
     * @param view
     * @return
     */
    static View getScrolledView(View view) {
        View consecutiveView = null;

        // 先处理layout_scrollChild指定滑动view的情况
        View scrolledView = getScrollChild(view);

        while (scrolledView instanceof IConsecutiveScroller) {
            consecutiveView = scrolledView;
            scrolledView = ((IConsecutiveScroller) scrolledView).getCurrentScrollerView();

            if (consecutiveView == scrolledView) {
                break;
            }
        }

        return scrolledView;
    }

    /**
     * 如果通过layout_scrollChild指定的滑动子view，则返回子view，否则返回view
     *
     * @param view
     * @return
     */
    static View getScrollChild(View view) {
        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();

            if (lp instanceof ConsecutiveScrollerLayout.LayoutParams) {
                int childId = ((ConsecutiveScrollerLayout.LayoutParams) lp).scrollChild;
                if (childId != View.NO_ID){
                    View child = view.findViewById(childId);
                    if (child != null) {
                        return child;
                    }
                }
            }
        }
        return view;
    }

    static boolean startInterceptRequestLayout(RecyclerView view) {
        if ("InterceptRequestLayout".equals(view.getTag())) {
            try {
                Method method = RecyclerView.class.getDeclaredMethod("startInterceptRequestLayout");
                method.setAccessible(true);
                method.invoke(view);
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    static void stopInterceptRequestLayout(RecyclerView view) {
        if ("InterceptRequestLayout".equals(view.getTag())) {
            try {
                Method method = RecyclerView.class.getDeclaredMethod("stopInterceptRequestLayout", boolean.class);
                method.setAccessible(true);
                method.invoke(view, false);
            } catch (Exception e) {
            }
        }
    }

    /**
     * 判断父级容器是否是isConsecutive：true.判断到最近的ConsecutiveScrollerLayout容器
     *
     * @param view
     * @return
     */
    static boolean isConsecutiveScrollParent(View view) {
        View child = view;
        while (child.getParent() instanceof ViewGroup && !(child.getParent() instanceof ConsecutiveScrollerLayout)) {
            child = (View) child.getParent();
        }

        if (child.getParent() instanceof ConsecutiveScrollerLayout) {
            return isConsecutiveScrollerChild(child);
        } else {
            return false;
        }
    }

    /**
     * 判断当前触摸点下是否有view可以水平滑动
     *
     * @param rootView
     * @param touchX
     * @param touchY
     * @return
     */
    static boolean isHorizontalScroll(View rootView, int touchX, int touchY) {
        List<View> views = getTouchViews(rootView, touchX, touchY);
        for (View view : views) {
            if (view.canScrollHorizontally(1) || view.canScrollHorizontally(-1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否触摸吸顶view并且不能触发布局滑动
     *
     * @return
     */
    static boolean isTouchNotTriggerScrollStick(View rootView, int touchX, int touchY) {
        List<ConsecutiveScrollerLayout> csLayouts = getInTouchCSLayout(rootView, touchX, touchY);
        int size = csLayouts.size();
        for (int i = size - 1; i >= 0; i--) {
            ConsecutiveScrollerLayout csl = csLayouts.get(i);
            View topView = getTopViewInTouch(csl, touchX, touchY);
            if (topView != null && csl.isStickyView(topView) && csl.theChildIsStick(topView)) {
                ConsecutiveScrollerLayout.LayoutParams lp = (ConsecutiveScrollerLayout.LayoutParams) topView.getLayoutParams();
                if (!lp.isTriggerScroll) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取触摸点下的所有ConsecutiveScrollerLayout
     *
     * @param rootView
     * @param touchX
     * @param touchY
     * @return
     */
    static List<ConsecutiveScrollerLayout> getInTouchCSLayout(View rootView, int touchX, int touchY) {
        List<ConsecutiveScrollerLayout> csLayouts = new ArrayList<>();
        List<View> views = getTouchViews(rootView, touchX, touchY);
        for (View view : views) {
            if (view instanceof ConsecutiveScrollerLayout) {
                csLayouts.add((ConsecutiveScrollerLayout) view);
            }
        }
        return csLayouts;
    }

    static View getTopViewInTouch(ConsecutiveScrollerLayout csl, int touchX, int touchY) {
        int count = csl.getChildCount();
        View topTouchView = null;

        for (int i = 0; i < count; i++) {
            View child = csl.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && isTouchPointInView(child, touchX, touchY)) {
                if (topTouchView == null) {
                    topTouchView = child;
                    continue;
                }

                if (ViewCompat.getZ(child) > ViewCompat.getZ(topTouchView) // 判断View的Z高度
                        || (ViewCompat.getZ(child) == ViewCompat.getZ(topTouchView)
                        && csl.getDrawingPosition(child) > csl.getDrawingPosition(topTouchView))) { // 判断绘制顺序
                    topTouchView = child;
                }
            }
        }

        return topTouchView;
    }
}
