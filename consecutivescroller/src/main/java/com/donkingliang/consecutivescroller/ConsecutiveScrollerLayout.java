package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import androidx.core.view.ScrollingView;

import java.lang.reflect.Method;

/**
 * @Author donkingliang QQ:1043214265 github:https://github.com/donkingliang
 * @Description
 * @Date 2020/3/13
 */
public class ConsecutiveScrollerLayout extends ViewGroup implements ScrollingView {

    /**
     * 手指滑动方向
     */
    private static int SCROLL_ORIENTATION_NONE = -1;
    /**
     * 手指滑动方向 -- 垂直
     */
    private static int SCROLL_ORIENTATION_VERTICAL = 0;
    /**
     * 手指滑动方向 -- 水平
     */
    private static int SCROLL_ORIENTATION_HORIZONTAL = 1;
    /**
     * 手指滑动方向
     */
    private int mScrollOrientation = SCROLL_ORIENTATION_NONE;

    /**
     * 手指触摸屏幕时的触摸点
     */
    private int mTouchY;

    /**
     * 记录布局垂直的偏移量，它是包括了自己的偏移量(mScrollY)和所有子View的偏移量的总和，
     * 取代View原有的mScrollY作为对外提供的偏移量值
     */
    private int mOwnScrollY;

    /**
     * 联动容器可滚动的范围
     */
    private int mScrollRange;

    /**
     * 联动容器滚动定位子view
     */
    private Scroller mScroller;
    /**
     * VelocityTracker
     */
    private VelocityTracker mVelocityTracker;
    /**
     * MaximumVelocity
     */
    private int mMaximumVelocity;
    /**
     * MinimumVelocity
     */
    private int mMinimumVelocity;

    /**
     * 滑动监听
     */
    protected OnScrollChangeListener mOnScrollChangeListener;


    public ConsecutiveScrollerLayout(Context context) {
        this(context, null);
    }

    public ConsecutiveScrollerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConsecutiveScrollerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mScroller = new Scroller(getContext());
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();

        // 确保联动容器调用onDraw()方法
        setWillNotDraw(false);
        // enable vertical scrollbar
        setVerticalScrollBarEnabled(true);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);

        // 去掉子View的滚动条。选择在这里做这个操作，而不是在onFinishInflate方法中完成，是为了兼顾用代码add子View的情况
        child.setVerticalScrollBarEnabled(false);
        child.setHorizontalScrollBarEnabled(false);
        child.setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mScrollRange = 0;
        int childTop = t;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int bottom = childTop + child.getMeasuredHeight();
            child.layout(l, childTop, r, bottom);
            childTop = bottom;
            // 联动容器可滚动最大距离
            mScrollRange += child.getHeight();
//            mEdgeList.put(child, new ViewEdge(child.getTop(), child.getBottom()));
        }
        // 联动容器可滚动range
        mScrollRange -= getMeasuredHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            stopScroll();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            // 拦截所有的滑动事件，自己处理
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchY = (int) ev.getY();
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchY == 0) {
                    mTouchY = (int) ev.getY();
                    return true;
                }
                int y = (int) ev.getY();
                int dy = y - mTouchY;
                mTouchY = y;
                scrollBy(0, -dy);

                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchY = 0;

                if (mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) mVelocityTracker.getYVelocity();
                    recycleVelocityTracker();
                    fling(-yVelocity);
                }
                break;
        }
        return true;
    }

    private void fling(int velocityY) {
        if (Math.abs(velocityY) > mMinimumVelocity) {
            mScroller.fling(0, mOwnScrollY,
                    1, velocityY,
                    0, 0,
                    Integer.MIN_VALUE, Integer.MAX_VALUE);
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int curY = mScroller.getCurrY();
            dispatchScroll(curY);
            invalidate();
        }
    }

    /**
     * 分发处理滑动
     *
     * @param y
     */
    private void dispatchScroll(int y) {
        int offset = y - mOwnScrollY;
        if (mOwnScrollY < y) {
            scrollUp(offset);
        } else if (mOwnScrollY > y) {
            scrollDown(offset);
        }
    }

    /**
     * 向上滑动
     *
     * @param offset
     */
    private void scrollUp(int offset) {
//        Log.e("eee","scrollUp" + offset);
        int scrollOffset = 0;
        int remainder = offset;
        int oldScrollY = mOwnScrollY;
        do {
            scrollOffset = 0;
            if (!isScrollBottom()) {
                // 找到当前显示的第一个View
                View firstVisibleView = findFirstVisibleView();
                if (firstVisibleView != null) {
                    int bottomOffset = getScrollBottomOffset(firstVisibleView);
                    if (bottomOffset > 0) {
                        scrollOffset = Math.min(remainder, bottomOffset);
                        firstVisibleView.scrollBy(0, scrollOffset);
                    } else {
                        scrollOffset = Math.min(remainder, firstVisibleView.getBottom() - getScrollY());
                        scrollOffset = Math.min(scrollOffset, mScrollRange - scrollOffset);
                        super.scrollTo(0, getScrollY() + scrollOffset);
                    }
//                Log.e("eee", bottomOffset + " " + scrollOffset);
                    mOwnScrollY += scrollOffset;
                    remainder = remainder - scrollOffset;
                    Log.e("eee", scrollOffset + " " + remainder);
                }
            }
        } while (scrollOffset != 0 && remainder != 0);

        if (oldScrollY != mOwnScrollY) {
            scrollChange(mOwnScrollY, oldScrollY);
        }
    }

    private void scrollDown(int offset) {
//        Log.e("eee","scrollDown" + offset);
        int scrollOffset = 0;
        int scrollY = getScrollY();
        int remainder = offset;
        int oldScrollY = mOwnScrollY;
        do {
            scrollOffset = 0;
            if (!isScrollTop()) {
                // 找到当前显示的最后一个View
                View lastVisibleView = findLastVisibleView();
                if (lastVisibleView != null) {
                    int childScrollOffset = getScrollTopOffset(lastVisibleView);
                    if (childScrollOffset < 0) {
                        scrollOffset = Math.max(offset, childScrollOffset);
                        lastVisibleView.scrollBy(0, scrollOffset);
                    } else {
                        scrollOffset = Math.max(offset, lastVisibleView.getTop() - scrollY - getHeight());
                        scrollOffset = Math.max(scrollOffset, -scrollY);
                        super.scrollTo(0, scrollY + scrollOffset);
                    }
                    mOwnScrollY += scrollOffset;
                    remainder = offset - scrollOffset;
                }
            }
        } while (scrollOffset != 0 && remainder != 0);

        if (oldScrollY != mOwnScrollY) {
            scrollChange(mOwnScrollY, oldScrollY);
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(0, mOwnScrollY + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        //所有的scroll操作都交由dispatchScroll()来分发处理
        dispatchScroll(y);
    }

    private void scrollChange(int scrollY, int oldScrollY) {
        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, scrollY, oldScrollY);
        }
    }

    /**
     * 初始化VelocityTracker
     */
    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    /**
     * 初始化VelocityTracker
     */
    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 回收VelocityTracker
     */
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 停止滑动
     */
    private void stopScroll() {
        mScroller.abortAnimation();
    }

    /**
     * 使用这个方法取代View的getScrollY
     *
     * @return
     */
    public int getOwnScrollY() {
        return mOwnScrollY;
    }

    /**
     * 找到当前显示的第一个View
     *
     * @return
     */
    private View findFirstVisibleView() {
        int offset = getScrollY();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (offset >= child.getTop() && offset < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    /**
     * 找到当前显示的第最后一个View
     *
     * @return
     */
    private View findLastVisibleView() {
        int offset = getScrollY() + getHeight();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (offset > child.getTop() && offset <= child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    /**
     * 是否滑动到顶部
     *
     * @return
     */
    private boolean isScrollTop() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            return getScrollY() <= 0 && !child.canScrollVertically(-1);
        }
        return true;
    }

    /**
     * 是否滑动到底部
     *
     * @return
     */
    private boolean isScrollBottom() {
        if (getChildCount() > 0) {
            View child = getChildAt(getChildCount() - 1);
            return getScrollY() >= mScrollRange && !child.canScrollVertically(1);
        }
        return true;
    }

    /**
     * 禁止设置滑动监听，因为这个监听器已无效
     * 若想监听容器的滑动，请使用
     *
     * @param l
     * @see #setOnVerticalScrollChangeListener(OnScrollChangeListener)
     */
    @Deprecated
    @Override
    public void setOnScrollChangeListener(View.OnScrollChangeListener l) {
    }

    /**
     * 设置滑动监听
     *
     * @param l
     */
    public void setOnVerticalScrollChangeListener(OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollRange() {
        int range = 0;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            range += Math.max(computeVerticalScrollRange(child), computeVerticalScrollExtent(child));
        }

        return range;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return mOwnScrollY;
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    private int computeVerticalScrollOffset(View view) {

        if (view instanceof ScrollingView) {
            ScrollingView scrollingView = (ScrollingView) view;
            return scrollingView.computeVerticalScrollOffset();
        }

        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollOffset");
            method.setAccessible(true);
            return (int) method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view.getScrollY();
    }

    private int computeVerticalScrollRange(View view) {

        if (view instanceof ScrollingView) {
            ScrollingView scrollingView = (ScrollingView) view;
            return scrollingView.computeVerticalScrollRange();
        }

        try {
            Method method = View.class.getDeclaredMethod("computeVerticalScrollRange");
            method.setAccessible(true);
            return (int) method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view.getHeight();
    }

    private int computeVerticalScrollExtent(View view) {

        if (view instanceof ScrollingView) {
            ScrollingView scrollingView = (ScrollingView) view;
            return scrollingView.computeVerticalScrollExtent();
        }

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
    private int getScrollTopOffset(View view) {
        if (view.canScrollVertically(-1)) {
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
    private int getScrollBottomOffset(View view) {
        if (view.canScrollVertically(1)) {
            return computeVerticalScrollRange(view) - computeVerticalScrollOffset(view)
                    - computeVerticalScrollExtent(view);
        } else {
            return 0;
        }
    }

    public interface OnScrollChangeListener {

        void onScrollChange(View v, int scrollY, int oldScrollY);
    }

}
