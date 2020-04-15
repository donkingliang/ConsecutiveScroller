package com.donkingliang.consecutivescroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

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
    private int mEventY;
    private int mScrollOffset = 0;
    private boolean isConsecutiveScrollerChild = false;

    /**
     * 是否处于拖拽状态
     */
    private boolean mIsDragging;
    private boolean isAdjust = true;

    /**
     * 滑动监听
     */
    protected OnScrollChangeListener mOnScrollChangeListener;

    private int mActivePointerId;

    private NestedScrollingParentHelper mParentHelper;

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
        child.setVerticalScrollBarEnabled(false);
        child.setHorizontalScrollBarEnabled(false);
        child.setOverScrollMode(OVER_SCROLL_NEVER);
        if (child instanceof ViewGroup) {
            ((ViewGroup) child).setClipToPadding(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

        // 布局发生变化，检测滑动位置
        checkLayoutChange();
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

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 停止滑动
                stopScroll();
                checkTargetsScroll(false);
            case MotionEvent.ACTION_POINTER_DOWN:
                mActivePointerId = ev.getPointerId(actionIndex);
                mEventY = (int) ev.getY(actionIndex);
                // 改变滑动的手指，重新询问事件拦截
                requestDisallowInterceptTouchEvent(false);
                isConsecutiveScrollerChild = ScrollUtils.isConsecutiveScrollerChild(getTouchTarget(
                        ScrollUtils.getRawX(this, ev, actionIndex), ScrollUtils.getRawY(this, ev, actionIndex)));
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                int offsetY = (int) ev.getY(pointerIndex) - mEventY;

                if (isIntercept(ev)) {
                    if (Math.abs(offsetY) >= mTouchSlop) {
                        mIsDragging = true;
                    }

                    if (!mIsDragging) {
                        return true;
                    }
                }

                mScrollOffset = offsetY;
                mEventY = (int) ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (mActivePointerId == ev.getPointerId(actionIndex)) { // 如果松开的是活动手指, 让还停留在屏幕上的最后一根手指作为活动手指
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    // pointerIndex都是像0, 1, 2这样连续的
                    final int newPointerIndex = actionIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    mEventY = (int) ev.getY(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mScrollOffset = 0;
                mIsDragging = false;
                mEventY = 0;
                break;
        }
        boolean dispatch;
        if (isConsecutiveScrollerChild) {
            dispatch = adjustScroll(ev);
        } else {
            dispatch = super.dispatchTouchEvent(ev);
        }
        return dispatch;
    }

    /**
     * 追踪手指的垂直滑动轨迹，通过计算ConsecutiveScrollerLayout及其后代View的滑动情况来确定
     * ConsecutiveScrollerLayout是否需要消费事件，确保滑动布局的滑动情况与用户的滑动操作保持一致。
     *
     * @param ev
     * @return
     */
    private boolean adjustScroll(MotionEvent ev) {
        List<View> views = ScrollUtils.getTouchViews(this, (int) ev.getRawX(), (int) ev.getRawY());
        List<Integer> offset = ScrollUtils.getScrollOffsetForViews(views);
        boolean dispatch = super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                initOrResetAdjustVelocityTracker();
                mAdjustVelocityTracker.addMovement(ev);
                isAdjust = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isAdjust && Math.abs(mScrollOffset) >= mTouchSlop) {
                    if (ScrollUtils.equalsOffsets(offset, ScrollUtils.getScrollOffsetForViews(views))) {
                        scrollBy(0, -mScrollOffset);
                    } else {
                        isAdjust = false;
                    }
                }
                initAdjustVelocityTrackerIfNotExists();
                mAdjustVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isConsecutiveScrollerChild = false;
                if (mAdjustVelocityTracker != null) {
                    mAdjustVelocityTracker.addMovement(ev);
                    if (isAdjust && mScroller.isFinished()) {
                        mAdjustVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        int yVelocity = (int) mAdjustVelocityTracker.getYVelocity();
                        recycleAdjustVelocityTracker();
                        fling(-yVelocity);
                    }
                }
                isAdjust = true;
                break;
        }
        return dispatch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            // 需要拦截事件
            if (isIntercept(ev)) {
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
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchY == 0) {
                    mTouchY = (int) ev.getY(pointerIndex);
                    return true;
                }
                int y = (int) ev.getY(pointerIndex);
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
                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) mVelocityTracker.getYVelocity();
                    yVelocity = Math.max(-mMaximumVelocity, Math.min(yVelocity, mMaximumVelocity));
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
                    Integer.MIN_VALUE, Integer.MIN_VALUE,
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

        if (mScroller.isFinished()) {
            // 滚动结束，校验子view内容的滚动位置
            checkTargetsScroll(false);
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
                        if(!ScrollUtils.isRecyclerLayout(firstVisibleView)){
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
                        if(!ScrollUtils.isRecyclerLayout(lastVisibleView)){
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
        if (child instanceof AbsListView) {
            AbsListView listView = (AbsListView) child;
            listView.scrollListBy(y);
        } else {
            child.scrollBy(0, y);
        }
    }

    /**
     * 布局发生变化，重新检查所有子View是否正确显示
     */
    public void checkLayoutChange() {
        scrollSelf(getScrollY());
        checkTargetsScroll(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            resetChildren();
        }
        resetSticky();
    }

    /**
     * 校验子view内容滚动位置是否正确
     */
    private void checkTargetsScroll(boolean isLayoutChange) {
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
                scrollTargetContentToBottom(child);
            }
        }
        for (int i = index + 1; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (ScrollUtils.isConsecutiveScrollerChild(child)) {
                scrollTargetContentToTop(child);
            }
        }

        computeOwnScrollOffset();

        if (oldScrollY != mOwnScrollY) {
            onScrollChange(mOwnScrollY, oldScrollY);
            resetSticky();
        }
    }

    /**
     * 滚动指定子view的内容到顶部
     *
     * @param target
     */
    private void scrollTargetContentToTop(View target) {
        int scrollY = 0;
        do {
            scrollY = 0;
            int offset = ScrollUtils.getScrollTopOffset(target);
            if (offset > 0) {
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
    private void scrollTargetContentToBottom(View target) {
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
            scrollY += ScrollUtils.computeVerticalScrollOffset(child);
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
            return getScrollY() <= 0 && !child.canScrollVertically(-1);
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
    public int computeVerticalScrollRange() {
        int range = 0;

        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            if (!ScrollUtils.isConsecutiveScrollerChild(child)) {
                range += child.getHeight();
            } else {
                range += Math.max(ScrollUtils.computeVerticalScrollRange(child) + child.getPaddingTop() + child.getPaddingBottom(),
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
            ViewGroup.LayoutParams lp = target.getLayoutParams();
            if (lp instanceof LayoutParams) {
                if (!((LayoutParams) lp).isConsecutive) {
                    return false;
                }
            }

            if (ScrollUtils.canScrollVertically(target)) {
                return true;
            }
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
        checkTargetsScroll(false);
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
}
