package com.donkingliang.consecutivescroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.EdgeEffectCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author donkingliang QQ:1043214265 github:https://github.com/donkingliang
 * @Description
 * @Date 2020/3/13
 */
public class ConsecutiveScrollerLayout extends ViewGroup implements NestedScrollingParent {

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
    private OverScroller mScroller;

    /**
     * VelocityTracker
     */
    private VelocityTracker mVelocityTracker;
    private VelocityTracker mAdjustVelocityTracker;

    /**
     * MaximumVelocity
     */
    private int mMaximumVelocity;

    /**
     * MinimumVelocity
     */
    private int mMinimumVelocity;

    private int mTouchSlop;

    /**
     * 手指触摸屏幕时的触摸点
     */
    private int mTouchY;
    private int mEventX;
    private int mEventY;
    private float mFixedY;

    /**
     * 是否处于状态
     */
    private boolean mTouching = false;

    private static final int SCROLL_NONE = 0;
    private static final int SCROLL_VERTICAL = 1;
    private static final int SCROLL_HORIZONTAL = 2;
    private int SCROLL_ORIENTATION = SCROLL_NONE;

    /**
     * 滑动监听
     */
    protected OnScrollChangeListener mOnScrollChangeListener;

    private int mActivePointerId;

    private NestedScrollingParentHelper mParentHelper;

    private View mScrollToTopView;
    private int mAdjust;

    /**
     * 滑动到指定view，目标view的index
     */
    private int mScrollToIndex = -1;

    /**
     * 滑动到指定view，平滑滑动时，每次滑动的距离
     */
    private int mSmoothScrollOffset = 0;

    /**
     * 上边界阴影
     */
    private EdgeEffect mEdgeGlowTop;
    /**
     * 下边界阴影
     */
    private EdgeEffect mEdgeGlowBottom;

    /**
     * fling时，保存最后的滑动位置，在下一帧时通过对比新的滑动位置，判断滑动的方向。
     */
    private int mLastScrollerY;

    // 这是RecyclerView的代码，让ConsecutiveScrollerLayout的fling效果更接近于RecyclerView。
    static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public ConsecutiveScrollerLayout(Context context) {
        this(context, null);
    }

    public ConsecutiveScrollerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConsecutiveScrollerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mScroller = new OverScroller(getContext(), sQuinticInterpolator);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mTouchSlop = ViewConfiguration.getTouchSlop();
        // 确保联动容器调用onDraw()方法
        setWillNotDraw(false);
        // enable vertical scrollbar
        setVerticalScrollBarEnabled(true);

        mParentHelper = new NestedScrollingParentHelper(this);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        // 去掉子View的滚动条。选择在这里做这个操作，而不是在onFinishInflate方法中完成，是为了兼顾用代码add子View的情况

        if (ScrollUtils.isConsecutiveScrollerChild(child)) {
            if (child instanceof IConsecutiveScroller) {
                List<View> views = ((IConsecutiveScroller) child).getScrolledViews();
                if (views != null && !views.isEmpty()) {
                    int size = views.size();
                    for (int i = 0; i < size; i++) {
                        child.setVerticalScrollBarEnabled(false);
                        child.setHorizontalScrollBarEnabled(false);
                        child.setOverScrollMode(OVER_SCROLL_NEVER);
                    }
                }

            } else {
                child.setVerticalScrollBarEnabled(false);
                child.setHorizontalScrollBarEnabled(false);
                child.setOverScrollMode(OVER_SCROLL_NEVER);
            }
        }

        if (child instanceof ViewGroup) {
            ((ViewGroup) child).setClipToPadding(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        resetScrollToTopView();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 测量子view
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        mScrollRange = 0;
        int childTop = getPaddingTop();
        int left = getPaddingLeft();

        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            int bottom = childTop + child.getMeasuredHeight();
            child.layout(left, childTop, left + child.getMeasuredWidth(), bottom);
            childTop = bottom;
            // 联动容器可滚动最大距离
            mScrollRange += child.getHeight();
        }
        // 联动容器可滚动range
        mScrollRange -= getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        // mScrollRange不能少于0
        if (mScrollRange < 0) {
            mScrollRange = 0;
        }

        // 布局发生变化，检测滑动位置
        checkLayoutChange(changed, false);
    }

    private void resetScrollToTopView() {
        mScrollToTopView = findFirstVisibleView();
        if (mScrollToTopView != null) {
            mAdjust = getScrollY() - mScrollToTopView.getTop();
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int actionIndex = ev.getActionIndex();

        if (SCROLL_ORIENTATION == SCROLL_HORIZONTAL) {
            // 如果是横向滑动，设置ev的y坐标始终为开始的坐标，避免子view自己消费了垂直滑动事件。
            ev.setLocation(ev.getX(), mFixedY);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 停止滑动
                stopScroll();
                checkTargetsScroll(false, false);
                mTouching = true;
                SCROLL_ORIENTATION = SCROLL_NONE;
                mFixedY = ev.getY();
                mActivePointerId = ev.getPointerId(actionIndex);
                mEventY = (int) ev.getY(actionIndex);
                mEventX = (int) ev.getX(actionIndex);

                initOrResetAdjustVelocityTracker();
                mAdjustVelocityTracker.addMovement(ev);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mActivePointerId = ev.getPointerId(actionIndex);
                mEventY = (int) ev.getY(actionIndex);
                mEventX = (int) ev.getX(actionIndex);
                // 改变滑动的手指，重新询问事件拦截
                requestDisallowInterceptTouchEvent(false);

                initAdjustVelocityTrackerIfNotExists();
                mAdjustVelocityTracker.addMovement(ev);

                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:

                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);

                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                int offsetY = (int) ev.getY(pointerIndex) - mEventY;
                int offsetX = (int) ev.getX(pointerIndex) - mEventX;
                if (isIntercept(ev)) {

                    if (SCROLL_ORIENTATION == SCROLL_NONE) {
                        if (Math.abs(offsetX) > Math.abs(offsetY)) {
                            if (Math.abs(offsetX) >= mTouchSlop) {
                                SCROLL_ORIENTATION = SCROLL_HORIZONTAL;
                                // 如果是横向滑动，设置ev的y坐标始终为开始的坐标，避免子view自己消费了垂直滑动事件。
                                ev.setLocation(ev.getX(), mFixedY);
                            }
                        } else {
                            if (Math.abs(offsetY) >= mTouchSlop) {
                                SCROLL_ORIENTATION = SCROLL_VERTICAL;
                            }
                        }

                        if (SCROLL_ORIENTATION == SCROLL_NONE) {
                            return true;
                        }
                    }
                }

                mEventY = (int) ev.getY(pointerIndex);
                mEventX = (int) ev.getX(pointerIndex);

                initAdjustVelocityTrackerIfNotExists();
                mAdjustVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (mActivePointerId == ev.getPointerId(actionIndex)) { // 如果松开的是活动手指, 让还停留在屏幕上的最后一根手指作为活动手指
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    // pointerIndex都是像0, 1, 2这样连续的
                    final int newPointerIndex = actionIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    mEventY = (int) ev.getY(newPointerIndex);
                    mEventX = (int) ev.getX(newPointerIndex);
                }
                initAdjustVelocityTrackerIfNotExists();
                mAdjustVelocityTracker.addMovement(ev);

                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                if (mAdjustVelocityTracker != null) {
                    mAdjustVelocityTracker.addMovement(ev);
                    mAdjustVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) mAdjustVelocityTracker.getYVelocity();
                    recycleAdjustVelocityTracker();
                    boolean canScrollVerticallyChild = ScrollUtils.canScrollVertically(getTouchTarget(
                            ScrollUtils.getRawX(this, ev, actionIndex), ScrollUtils.getRawY(this, ev, actionIndex)));
                    if (SCROLL_ORIENTATION == SCROLL_HORIZONTAL && canScrollVerticallyChild && Math.abs(yVelocity) > mMinimumVelocity) {
                        //如果当前是横向滑动，但是触摸的控件可以垂直滑动，并且产生垂直滑动的fling事件，
                        // 为了不让这个控件垂直fling，把事件设置为MotionEvent.ACTION_CANCEL。
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                    }
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(ev);
                }

                mEventY = 0;
                mEventX = 0;
                mTouching = false;
                SCROLL_ORIENTATION = SCROLL_NONE;
                break;
        }

        boolean dispatch = super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                recycleVelocityTracker();
                break;
        }

        return dispatch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // 需要拦截事件
            if (isIntercept(ev) && SCROLL_ORIENTATION == SCROLL_VERTICAL) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                mTouchY = (int) ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchY == 0) {
                    mTouchY = (int) ev.getY(pointerIndex);
                    return true;
                }
                int y = (int) ev.getY(pointerIndex);
                int dy = y - mTouchY;
                mTouchY = y;
                int oldY = mOwnScrollY;
                scrollBy(0, -dy);
                int deltaY = -dy;

                // 判断是否显示边界阴影
                final int range = getScrollRange();
                final int overscrollMode = getOverScrollMode();
                boolean canOverscroll = overscrollMode == View.OVER_SCROLL_ALWAYS
                        || (overscrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
                if (canOverscroll) {
                    ensureGlows();
                    final int pulledToY = oldY + deltaY;
                    if (pulledToY < 0) {
                        // 滑动距离超出顶部边界，设置阴影
                        EdgeEffectCompat.onPull(mEdgeGlowTop, (float) deltaY / getHeight(),
                                ev.getX(pointerIndex) / getWidth());
                        if (!mEdgeGlowBottom.isFinished()) {
                            mEdgeGlowBottom.onRelease();
                        }
                    } else if (pulledToY > range) {
                        // 滑动距离超出底部边界，设置阴影
                        EdgeEffectCompat.onPull(mEdgeGlowBottom, (float) deltaY / getHeight(),
                                1.f - ev.getX(pointerIndex)
                                        / getWidth());
                        if (!mEdgeGlowTop.isFinished()) {
                            mEdgeGlowTop.onRelease();
                        }
                    }
                    if (mEdgeGlowTop != null
                            && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                endDrag();
                mTouchY = 0;

                if (mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) mVelocityTracker.getYVelocity();
                    yVelocity = Math.max(-mMaximumVelocity, Math.min(yVelocity, mMaximumVelocity));
                    fling(-yVelocity);
                }
                break;
        }
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // 绘制边界阴影
        if (mEdgeGlowTop != null) {
            final int scrollY = getScrollY();
            if (!mEdgeGlowTop.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = 0;
                int yTranslation = scrollY;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation += getPaddingTop();
                }
                canvas.translate(xTranslation, yTranslation);
                mEdgeGlowTop.setSize(width, height);
                if (mEdgeGlowTop.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished()) {
                final int restoreCount = canvas.save();
                int width = getWidth();
                int height = getHeight();
                int xTranslation = 0;
                int yTranslation = scrollY + height;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || getClipToPadding()) {
                    width -= getPaddingLeft() + getPaddingRight();
                    xTranslation += getPaddingLeft();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getClipToPadding()) {
                    height -= getPaddingTop() + getPaddingBottom();
                    yTranslation -= getPaddingBottom();
                }
                canvas.translate(xTranslation - width, yTranslation);
                canvas.rotate(180, width, 0);
                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            int childSize = computeVerticalScrollRange();
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            scrollRange = Math.max(0, childSize - parentSpace);
        }
        return scrollRange;
    }

    private void fling(int velocityY) {
        if (Math.abs(velocityY) > mMinimumVelocity) {
            mScroller.fling(0, mOwnScrollY,
                    1, velocityY,
                    Integer.MIN_VALUE, Integer.MIN_VALUE,
                    Integer.MIN_VALUE, Integer.MAX_VALUE);
            mLastScrollerY = mOwnScrollY;
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (mScrollToIndex != -1 && mSmoothScrollOffset != 0) {
            // 正在平滑滑动到某个子view
            scrollBy(0, mSmoothScrollOffset);
            invalidate();
        } else {

            // fling
            if (mScroller.computeScrollOffset()) {
                final int y = mScroller.getCurrY();
                int unconsumed = y - mLastScrollerY;
                mLastScrollerY = y;
                dispatchScroll(y);
                // 判断滑动方向和是否滑动到边界
                if ((unconsumed < 0 && isScrollTop()) || (unconsumed > 0 && isScrollBottom())) {
                    final int mode = getOverScrollMode();
                    final boolean canOverscroll = mode == OVER_SCROLL_ALWAYS
                            || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && getScrollRange() > 0);
                    if (canOverscroll) {
                        ensureGlows();
                        if (unconsumed < 0) {
                            // 设置上边界阴影
                            if (mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                            }
                        } else {
                            // 设置下边界阴影
                            if (mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                            }
                        }
                    }
                    stopScroll();
                }

                invalidate();
            }


            if (mScroller.isFinished()) {
                // 滚动结束，校验子view内容的滚动位置
                checkTargetsScroll(false, false);
            }
        }
    }

    private void endDrag() {
        if (mEdgeGlowTop != null) {
            mEdgeGlowTop.onRelease();
            mEdgeGlowBottom.onRelease();
        }
    }

    private void ensureGlows() {
        if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
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
        int scrollOffset = 0;
        int remainder = offset;
        int oldScrollY = mOwnScrollY;
        do {

            // 如果是要滑动到指定的View，判断滑动到目标位置，就停止滑动
            if (mScrollToIndex != -1) {
                View view = getChildAt(mScrollToIndex);
                if (getScrollY() + getPaddingTop() >= view.getTop() || isScrollBottom()) {
                    mScrollToIndex = -1;
                    mSmoothScrollOffset = 0;
                    break;
                }
            }

            scrollOffset = 0;
            if (!isScrollBottom()) {
                // 找到当前显示的第一个View
                View firstVisibleView = findFirstVisibleView();
                if (firstVisibleView != null) {
                    awakenScrollBars();
                    int bottomOffset = ScrollUtils.getScrollBottomOffset(firstVisibleView);
                    if (bottomOffset > 0) {
                        int childOldScrollY = ScrollUtils.computeVerticalScrollOffset(firstVisibleView);
                        scrollOffset = Math.min(remainder, bottomOffset);
                        scrollChild(firstVisibleView, scrollOffset);
                        int childNewScrollY = ScrollUtils.computeVerticalScrollOffset(firstVisibleView);
                        if (!ScrollUtils.isRecyclerLayout(firstVisibleView)) {
                            scrollOffset = childNewScrollY - childOldScrollY;
                        }
                    } else {
                        int selfOldScrollY = getScrollY();
                        scrollOffset = Math.min(remainder,
                                firstVisibleView.getBottom() - getPaddingTop() - getScrollY());
                        scrollSelf(getScrollY() + scrollOffset);
                        scrollOffset = getScrollY() - selfOldScrollY;
                    }
                    mOwnScrollY += scrollOffset;
                    remainder = remainder - scrollOffset;
                }
            }

        } while (scrollOffset > 0 && remainder > 0);

        if (oldScrollY != mOwnScrollY) {
            onScrollChange(mOwnScrollY, oldScrollY);
            resetSticky();
        }
    }

    private void scrollDown(int offset) {
        int scrollOffset = 0;
        int remainder = offset;
        int oldScrollY = mOwnScrollY;
        do {

            // 如果是要滑动到指定的View，判断滑动到目标位置，就停止滑动
            if (mScrollToIndex != -1) {
                View view = getChildAt(mScrollToIndex);
                if ((getScrollY() + getPaddingTop() <= view.getTop()
                        && ScrollUtils.getScrollTopOffset(view) >= 0) || isScrollTop()) {
                    mScrollToIndex = -1;
                    mSmoothScrollOffset = 0;
                    break;
                }
            }

            scrollOffset = 0;
            if (!isScrollTop()) {
                // 找到当前显示的最后一个View
                View lastVisibleView = findLastVisibleView();
                if (lastVisibleView != null) {
                    awakenScrollBars();
                    int childScrollOffset = ScrollUtils.getScrollTopOffset(lastVisibleView);
                    if (childScrollOffset < 0) {
                        int childOldScrollY = ScrollUtils.computeVerticalScrollOffset(lastVisibleView);
                        scrollOffset = Math.max(remainder, childScrollOffset);
                        scrollChild(lastVisibleView, scrollOffset);
                        int childNewScrollY = ScrollUtils.computeVerticalScrollOffset(lastVisibleView);
                        if (!ScrollUtils.isRecyclerLayout(lastVisibleView)) {
                            scrollOffset = childNewScrollY - childOldScrollY;
                        }
                    } else {
                        int scrollY = getScrollY();
                        int selfOldScrollY = getScrollY();
                        scrollOffset = Math.max(remainder,
                                lastVisibleView.getTop() + getPaddingBottom() - scrollY - getHeight());
                        scrollSelf(scrollY + scrollOffset);
                        scrollOffset = getScrollY() - selfOldScrollY;
                    }
                    mOwnScrollY += scrollOffset;
                    remainder = remainder - scrollOffset;
                }
            }

        } while (scrollOffset < 0 && remainder < 0);

        if (oldScrollY != mOwnScrollY) {
            onScrollChange(mOwnScrollY, oldScrollY);
            resetSticky();
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

    private void onScrollChange(int scrollY, int oldScrollY) {
        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, scrollY, oldScrollY);
        }
    }

    /**
     * 滑动自己
     *
     * @param y
     */
    private void scrollSelf(int y) {
        int scrollY = y;

        // 边界检测
        if (scrollY < 0) {
            scrollY = 0;
        } else if (scrollY > mScrollRange) {
            scrollY = mScrollRange;
        }
        super.scrollTo(0, scrollY);
    }

    private void scrollChild(View child, int y) {
        View scrolledView = ScrollUtils.getScrolledView(child);
        if (scrolledView instanceof AbsListView) {
            AbsListView listView = (AbsListView) scrolledView;
            listView.scrollListBy(y);
        } else {
            scrolledView.scrollBy(0, y);
        }
    }


    public void checkLayoutChange() {
        checkLayoutChange(false, true);
    }

    /**
     * 布局发生变化，重新检查所有子View是否正确显示
     */
    public void checkLayoutChange(boolean changed, boolean isForce) {
        if (mScrollToTopView != null && changed) {
            if (indexOfChild(mScrollToTopView) != -1) {
                scrollSelf(mScrollToTopView.getTop() + mAdjust);
            }
        } else {
            scrollSelf(getScrollY());
        }
        mScrollToTopView = null;
        mAdjust = 0;
        checkTargetsScroll(true, isForce);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            resetChildren();
            resetSticky();
        }
    }

    /**
     * 校验子view内容滚动位置是否正确
     */
    private void checkTargetsScroll(boolean isLayoutChange, boolean isForce) {

        if (!isForce && (mTouching || !mScroller.isFinished() || mScrollToIndex != -1)) {
            return;
        }

        int oldScrollY = mOwnScrollY;
        View target = findFirstVisibleView();
        if (target == null) {
            return;
        }
        int index = indexOfChild(target);

        if (isLayoutChange) {
            int bottomOffset = ScrollUtils.getScrollBottomOffset(target);
            int scrollTopOffset = target.getTop() - getScrollY();
            if (bottomOffset > 0 && scrollTopOffset < 0) {
                int offset = Math.min(bottomOffset, -scrollTopOffset);
                scrollSelf(getScrollY() - offset);
                scrollChild(target, offset);
            }
        }

        for (int i = 0; i < index; i++) {
            final View child = getChildAt(i);
            if (ScrollUtils.isConsecutiveScrollerChild(child)) {
                if (child instanceof IConsecutiveScroller) {
                    List<View> views = ((IConsecutiveScroller) child).getScrolledViews();
                    if (views != null && !views.isEmpty()) {
                        int size = views.size();
                        for (int c = 0; c < size; c++) {
                            scrollChildContentToBottom(views.get(c));
                        }
                    }

                } else {
                    scrollChildContentToBottom(child);
                }
            }
        }

        for (int i = index + 1; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (ScrollUtils.isConsecutiveScrollerChild(child)) {
                if (child instanceof IConsecutiveScroller) {
                    List<View> views = ((IConsecutiveScroller) child).getScrolledViews();
                    if (views != null && !views.isEmpty()) {
                        int size = views.size();
                        for (int c = 0; c < size; c++) {
                            scrollChildContentToTop(views.get(c));
                        }
                    }

                } else {
                    scrollChildContentToTop(child);
                }
            }
        }

        computeOwnScrollOffset();
        if (isLayoutChange) {
            if (oldScrollY != mOwnScrollY) {
                onScrollChange(mOwnScrollY, oldScrollY);
            }
        }

        resetSticky();
    }

    /**
     * 滚动指定子view的内容到顶部
     *
     * @param target
     */
    private void scrollChildContentToTop(View target) {
        int scrollY = 0;
        do {
            scrollY = 0;
            int offset = ScrollUtils.getScrollTopOffset(target);
            if (offset < 0) {
                int childOldScrollY = ScrollUtils.computeVerticalScrollOffset(target);
                scrollChild(target, offset);
                scrollY = childOldScrollY - ScrollUtils.computeVerticalScrollOffset(target);
            }
        } while (scrollY != 0);
    }

    /**
     * 滚动指定子view的内容到底部
     *
     * @param target
     */
    private void scrollChildContentToBottom(View target) {
        int scrollY = 0;
        do {
            scrollY = 0;
            int offset = ScrollUtils.getScrollBottomOffset(target);
            if (offset > 0) {
                int childOldScrollY = ScrollUtils.computeVerticalScrollOffset(target);
                scrollChild(target, offset);
                scrollY = childOldScrollY - ScrollUtils.computeVerticalScrollOffset(target);
            }
        } while (scrollY != 0);
    }

    /**
     * 重新计算mOwnScrollY
     *
     * @return
     */
    private void computeOwnScrollOffset() {
        int scrollY = getScrollY();
        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            if (ScrollUtils.isConsecutiveScrollerChild(child)) {
                scrollY += ScrollUtils.computeVerticalScrollOffset(child);
            }
        }

        mOwnScrollY = scrollY;
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
     * 初始化VelocityTracker
     */
    private void initOrResetAdjustVelocityTracker() {
        if (mAdjustVelocityTracker == null) {
            mAdjustVelocityTracker = VelocityTracker.obtain();
        } else {
            mAdjustVelocityTracker.clear();
        }
    }

    /**
     * 初始化VelocityTracker
     */
    private void initAdjustVelocityTrackerIfNotExists() {
        if (mAdjustVelocityTracker == null) {
            mAdjustVelocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 回收VelocityTracker
     */
    private void recycleAdjustVelocityTracker() {
        if (mAdjustVelocityTracker != null) {
            mAdjustVelocityTracker.recycle();
            mAdjustVelocityTracker = null;
        }
    }

    /**
     * 停止滑动
     */
    private void stopScroll() {
        mScroller.abortAnimation();
    }

    /**
     * 返回所有的非GONE子View
     *
     * @return
     */
    private List<View> getNonGoneChildren() {
        List<View> children = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * 返回所有高度不为0的view
     */
    private List<View> getEffectiveChildren() {
        List<View> children = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE && child.getHeight() > 0) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * 返回所有的吸顶子View(非GONE)
     *
     * @return
     */
    private List<View> getStickyChildren() {
        List<View> children = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE && isStickyChild(child)) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * 是否是需要吸顶的View
     *
     * @param child
     * @return
     */
    private boolean isStickyChild(View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof LayoutParams) {
            return ((LayoutParams) lp).isSticky;
        }
        return false;
    }

    /**
     * 布局发生变化，可能是某个吸顶布局的isSticky发生改变，需要重新重置一下所有子View的translationY、translationZ
     */
    @SuppressLint("NewApi")
    private void resetChildren() {
        List<View> children = getNonGoneChildren();
        for (View child : children) {
            child.setTranslationY(0);
            child.setTranslationZ(0);
        }
    }

    /**
     * 重置吸顶
     */
    private void resetSticky() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<View> children = getStickyChildren();
            if (!children.isEmpty()) {
                int count = children.size();

                // 让所有的View恢复原来的状态
                for (int i = 0; i < count; i++) {
                    View child = children.get(i);
                    child.setTranslationY(0);
                    child.setTranslationZ(0);
                }

                // 需要吸顶的View
                View stickyView = null;
                // 下一个需要吸顶的View
                View nextStickyView = null;

                // 找到需要吸顶的View
                for (int i = count - 1; i >= 0; i--) {
                    View child = children.get(i);
                    if (child.getTop() <= getScrollY()) {
                        stickyView = child;
                        if (i != count - 1) {
                            nextStickyView = children.get(i + 1);
                        }
                        break;
                    }
                }

                if (stickyView != null) {
                    int offset = 0;
                    if (nextStickyView != null) {
                        offset = Math.max(0, stickyView.getHeight() - (nextStickyView.getTop() - getScrollY()));
                    }
                    stickyChild(stickyView, offset);
                }
            }
        }
    }

    /**
     * 子View吸顶
     *
     * @param child
     * @param offset
     */
    @SuppressLint("NewApi")
    private void stickyChild(View child, int offset) {
        child.setY(getScrollY() - offset);
        child.setTranslationZ(1);

        // 把View设置为可点击的，避免吸顶View与其他子View重叠是，触摸事件透过吸顶View传递给下面的View，
        // 导致ConsecutiveScrollerLayout追踪布局的滑动出现偏差
        child.setClickable(true);
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
    public View findFirstVisibleView() {
        int offset = getScrollY() + getPaddingTop();
        List<View> children = getEffectiveChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            if (child.getTop() <= offset && child.getBottom() > offset) {
                return child;
            }
        }
        return null;
    }

    /**
     * 找到当前显示的最后一个View
     *
     * @return
     */
    public View findLastVisibleView() {
        int offset = getHeight() - getPaddingBottom() + getScrollY();
        List<View> children = getEffectiveChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            if (child.getTop() < offset && child.getBottom() >= offset) {
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
    public boolean isScrollTop() {
        List<View> children = getEffectiveChildren();
        if (children.size() > 0) {
            View child = children.get(0);
            View scrolledView = ScrollUtils.getScrolledView(child);
            return getScrollY() <= 0 && !scrolledView.canScrollVertically(-1);
        }
        return true;
    }

    /**
     * 是否滑动到底部
     *
     * @return
     */
    public boolean isScrollBottom() {
        List<View> children = getEffectiveChildren();
        if (children.size() > 0) {
            View child = children.get(children.size() - 1);
            View scrolledView = ScrollUtils.getScrolledView(child);
            return getScrollY() >= mScrollRange && !scrolledView.canScrollVertically(1);
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
    public int computeVerticalScrollRange() {
        int range = 0;

        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            if (!ScrollUtils.isConsecutiveScrollerChild(child)) {
                range += child.getHeight();
            } else {
                range += Math.max(ScrollUtils.computeVerticalScrollRange(child),
                        child.getHeight());
            }
        }

        return range;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return mOwnScrollY;
    }

    @Override
    public int computeVerticalScrollExtent() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    //根据坐标返回触摸到的View
    private View getTouchTarget(int touchX, int touchY) {
        View targetView = null;
        // 获取可触摸的View
        List<View> touchableViews = getNonGoneChildren();
        for (View touchableView : touchableViews) {
            if (ScrollUtils.isTouchPointInView(touchableView, touchX, touchY)) {
                targetView = touchableView;
                break;
            }
        }
        return targetView;
    }

    /**
     * 判断是否需要拦截事件
     *
     * @param ev
     * @return
     */
    private boolean isIntercept(MotionEvent ev) {
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        View target = getTouchTarget(ScrollUtils.getRawX(this, ev, pointerIndex),
                ScrollUtils.getRawY(this, ev, pointerIndex));

        if (target != null) {
            return ScrollUtils.isConsecutiveScrollerChild(target);
        }

        return false;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * 是否与父布局整体滑动，设置为false时，父布局不会拦截它的事件，滑动事件将由子view处理。
         * 可以实现子view内部的垂直滑动。
         */
        public boolean isConsecutive = true;

        /**
         * 是否支持嵌套滑动，默认支持，如果子view或它内部的下级view实现了NestedScrollingChild接口，
         * 它可以与ConsecutiveScrollerLayout嵌套滑动，把isNestedScroll设置为false可以禁止它与ConsecutiveScrollerLayout嵌套滑动。
         */
        public boolean isNestedScroll = true;

        /**
         * 设置子view吸顶悬浮
         */
        public boolean isSticky = false;


        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConsecutiveScrollerLayout_Layout);

            isConsecutive = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isConsecutive, true);
            isSticky = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isSticky, false);
            isNestedScroll = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isNestedScroll, true);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public interface OnScrollChangeListener {

        void onScrollChange(View v, int scrollY, int oldScrollY);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
        boolean isNestedScroll = false;
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof LayoutParams) {
            isNestedScroll = ((LayoutParams) lp).isNestedScroll;
        }
        if (isNestedScroll) {
            return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        } else {
            return false;
        }
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        mParentHelper.onNestedScrollAccepted(child, target, axes);
        checkTargetsScroll(false, false);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        mParentHelper.onStopNestedScroll(target);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        scrollBy(0, dyUnconsumed);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (velocityY > 0) {
            // 向上滑动
            if (isScrollBottom()) {
                return false;
            }

            if (!target.canScrollVertically(1)) {
                fling((int) velocityY);
                return true;
            }

        } else {
            // 向下滑动
            if (isScrollTop()) {
                return false;
            }

            if (!target.canScrollVertically(-1)) {
                fling((int) velocityY);
                return true;
            }
        }

        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return super.onNestedPreFling(target, velocityX, velocityY);
    }

    /**
     * 滑动到指定的view
     *
     * @param view
     */
    public void scrollToChild(View view) {
        int scrollToIndex = indexOfChild(view);
        if (scrollToIndex != -1) {
            mScrollToIndex = scrollToIndex;
            // 停止fling
            stopScroll();
            do {
                if (getScrollY() + getPaddingTop() >= view.getTop()) {
                    scrollBy(0, -200);
                } else {
                    scrollBy(0, 200);
                }

            } while (mScrollToIndex != -1);
        }
    }

    /**
     * 平滑滑动到指定的view
     *
     * @param view
     */
    public void smoothScrollToChild(View view) {
        int scrollToIndex = indexOfChild(view);
        if (scrollToIndex != -1) {
            mScrollToIndex = scrollToIndex;
            // 停止fling
            stopScroll();
            if (getScrollY() + getPaddingTop() >= view.getTop()) {
                mSmoothScrollOffset = -200;
            } else {
                mSmoothScrollOffset = 200;
            }
            invalidate();
        }
    }
}