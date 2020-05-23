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
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;
import androidx.core.widget.EdgeEffectCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author donkingliang QQ:1043214265 github:https://github.com/donkingliang
 * @Description
 * @Date 2020/3/13
 */
public class ConsecutiveScrollerLayout extends ViewGroup implements ScrollingView, NestedScrollingParent2, NestedScrollingChild2 {

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
     * 记录手指按下时的位置
     */
    private final int[] mDownLocation = new int[2];

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
    private NestedScrollingChildHelper mChildHelper;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

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

    /**
     * 吸顶view是否常驻，不被推出屏幕
     */
    private boolean isPermanent;

    /**
     * 吸顶view到顶部的偏移量
     */
    private int mStickyOffset = 0;

    /**
     * 保存当前吸顶的view(普通吸顶模式中，正在吸顶的view只有一个)
     */
    private View mCurrentStickyView;

    /**
     * 保存当前吸顶的view(常驻吸顶模式中，正在吸顶的view可能有多个)
     */
    private final List<View> mCurrentStickyViews = new ArrayList<>();
    // 临时保存吸顶的view，用于判断吸顶view是否改变了
    private final List<View> mTempStickyViews = new ArrayList<>();

    /**
     * 普通吸顶模式,监听吸顶变化
     */
    private OnStickyChangeListener mOnStickyChangeListener;

    /**
     * 常驻吸顶模式,监听吸顶变化
     */
    private OnPermanentStickyChangeListener mOnPermanentStickyChangeListener;

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
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.ConsecutiveScrollerLayout);
            isPermanent = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_isPermanent, false);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
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
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {

        if (params instanceof LayoutParams) {
            LayoutParamsUtils.invalidTopAndBottomMargin((LayoutParams) params);
        }

        super.addView(child, index, params);

        // 去掉子View的滚动条。选择在这里做这个操作，而不是在onFinishInflate方法中完成，是为了兼顾用代码add子View的情况

        if (ScrollUtils.isConsecutiveScrollerChild(child)) {
            disableChildScroll(child);
            if (child instanceof IConsecutiveScroller) {
                List<View> views = ((IConsecutiveScroller) child).getScrolledViews();
                if (views != null && !views.isEmpty()) {
                    int size = views.size();
                    for (int i = 0; i < size; i++) {
                        disableChildScroll(views.get(i));
                    }
                }
            }
        }

        if (child instanceof ViewGroup) {
            ((ViewGroup) child).setClipToPadding(false);
        }
    }

    /**
     * 禁用子view的一下滑动🇭🇰的属性
     *
     * @param child
     */
    private void disableChildScroll(View child) {
        child.setVerticalScrollBarEnabled(false);
        child.setHorizontalScrollBarEnabled(false);
        child.setOverScrollMode(OVER_SCROLL_NEVER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            child.setNestedScrollingEnabled(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        resetScrollToTopView();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 测量子view
        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {

        LayoutParamsUtils.invalidTopAndBottomMargin((LayoutParams) child.getLayoutParams());

        super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        mScrollRange = 0;
        int childTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int parentWidth = getMeasuredWidth();

        List<View> children = getNonGoneChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            View child = children.get(i);
            int bottom = childTop + child.getMeasuredHeight();
            int left = getChildLeft(child, parentWidth, paddingLeft, paddingRight);
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

    /**
     * 获取子view的left
     */
    private int getChildLeft(View child, int parentWidth, int paddingLeft, int paddingRight) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        switch (lp.align) {

            case RIGHT:
                return parentWidth - child.getMeasuredWidth() - paddingRight - lp.rightMargin;

            case CENTER:
                return paddingLeft + lp.leftMargin + ((parentWidth - child.getMeasuredWidth()
                        - paddingLeft - lp.leftMargin - paddingRight - lp.rightMargin) / 2);
            case LEFT:
            default:
                return paddingLeft + lp.leftMargin;
        }
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

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollVertically()) {
                    nestedScrollAxis = ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, ViewCompat.TYPE_TOUCH);

                mDownLocation[0] = ScrollUtils.getRawX(this, ev, mActivePointerId);
                mDownLocation[1] = ScrollUtils.getRawY(this, ev, mActivePointerId);
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
                mDownLocation[0] = 0;
                mDownLocation[1] = 0;
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
            if (SCROLL_ORIENTATION != SCROLL_HORIZONTAL
                    && (isIntercept(ev) || isIntercept(mDownLocation[0], mDownLocation[1]))) {
                return true;
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_UP
                || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            stopNestedScroll(ViewCompat.TYPE_TOUCH);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollVertically()) {
                    nestedScrollAxis = ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, ViewCompat.TYPE_TOUCH);

            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                mTouchY = (int) ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchY == 0) {
                    mTouchY = (int) ev.getY(pointerIndex);
                    return true;
                }

                mScrollConsumed[1] = 0;
                int y = (int) ev.getY(pointerIndex);
                int deltaY = mTouchY - y;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset, ViewCompat.TYPE_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                    ev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                }

                mTouchY = y - mScrollOffset[1];
                int oldScrollY = mOwnScrollY;
                scrollBy(0, deltaY);

                final int scrolledDeltaY = mOwnScrollY - oldScrollY;
                deltaY = deltaY - scrolledDeltaY;

                dispatchNestedScroll(0, scrolledDeltaY, 0, deltaY, mScrollOffset,
                        ViewCompat.TYPE_TOUCH);
                deltaY += mScrollOffset[1];
                mTouchY -= mScrollOffset[1];

                // 判断是否显示边界阴影
                final int range = getScrollRange();
                final int overscrollMode = getOverScrollMode();
                boolean canOverscroll = overscrollMode == View.OVER_SCROLL_ALWAYS
                        || (overscrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
                if (canOverscroll) {
                    ensureGlows();
                    final int pulledToY = oldScrollY + deltaY;
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
                endDrag();
                mTouchY = 0;
                break;
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

    private boolean canScrollVertically() {
        return !isScrollTop() || !isScrollBottom();
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

    private int getScrollRange() {
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
            if (canScrollVertically()) {
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
            } else {
                stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
            }
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
                int y = mScroller.getCurrY();
                int unconsumed = y - mLastScrollerY;
                mLastScrollerY = y;

                mScrollConsumed[1] = 0;

                dispatchNestedPreScroll(0, unconsumed, mScrollConsumed, null,
                        ViewCompat.TYPE_NON_TOUCH);
                unconsumed -= mScrollConsumed[1];
                y -= mScrollConsumed[1];

                final int oldScrollY = mOwnScrollY;

                dispatchScroll(y);

                final int scrolledByMe = mOwnScrollY - oldScrollY;
                unconsumed -= scrolledByMe;

                if ((unconsumed < 0 && isScrollTop()) || (unconsumed > 0 && isScrollBottom())) {
                    dispatchNestedScroll(0, scrolledByMe, 0, unconsumed, mScrollOffset,
                            ViewCompat.TYPE_NON_TOUCH);
                    unconsumed += mScrollOffset[1];
                }

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
            scrollChange(mOwnScrollY, oldScrollY);
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
            scrollChange(mOwnScrollY, oldScrollY);
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

    private void scrollChange(int scrollY, int oldScrollY) {
        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, scrollY, oldScrollY);
        }
    }

    private void stickyChange(View oldStickyView, View newStickyView) {
        if (mOnStickyChangeListener != null) {
            mOnStickyChangeListener.OnStickyChange(oldStickyView, newStickyView);
        }
    }

    private void permanentStickyChange(List<View> mCurrentStickyViews) {
        if (mOnPermanentStickyChangeListener != null) {
            mOnPermanentStickyChangeListener.OnStickyChange(mCurrentStickyViews);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                listView.scrollListBy(y);
            }
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
                scrollChange(mOwnScrollY, oldScrollY);
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
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
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
                if (isPermanent) {//常驻
                    clearCurrentStickyView();
                    permanentStickyChild(children);
                } else {

                    clearCurrentStickyViews();

                    // 需要吸顶的View
                    View stickyView = null;
                    // 下一个需要吸顶的View
                    View nextStickyView = null;

                    // 找到需要吸顶的View
                    for (int i = count - 1; i >= 0; i--) {
                        View child = children.get(i);
                        if (getScrollY() > 0 && child.getTop() <= getStickyY()) {
                            stickyView = child;
                            if (i != count - 1) {
                                nextStickyView = children.get(i + 1);
                            }
                            break;
                        }
                    }

                    View oldStickyView = mCurrentStickyView;
                    View newStickyView = stickyView;

                    if (stickyView != null) {
                        int offset = 0;
                        if (nextStickyView != null) {
                            offset = Math.max(0, stickyView.getHeight() - (nextStickyView.getTop() - getStickyY()));
                        }
                        stickyChild(stickyView, offset);
                    }

                    if (oldStickyView != newStickyView) {
                        mCurrentStickyView = newStickyView;
                        stickyChange(oldStickyView, newStickyView);
                    }
                }
            } else {
                // 没有吸顶view
                clearCurrentStickyView();
                clearCurrentStickyViews();
            }
        }
    }

    private void clearCurrentStickyView() {
        if (mCurrentStickyView != null) {
            View oldStickyView = mCurrentStickyView;
            mCurrentStickyView = null;
            stickyChange(oldStickyView, null);
        }
    }

    private void clearCurrentStickyViews() {
        if (!mCurrentStickyViews.isEmpty()) {
            mCurrentStickyViews.clear();
            permanentStickyChange(mCurrentStickyViews);
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
        child.setY(getStickyY() - offset);
        child.setTranslationZ(1);

        // 把View设置为可点击的，避免吸顶View与其他子View重叠是，触摸事件透过吸顶View传递给下面的View，
        // 导致ConsecutiveScrollerLayout追踪布局的滑动出现偏差
        child.setClickable(true);
    }

    /**
     * 获取吸顶的位置。
     *
     * @return
     */
    private int getStickyY() {
        return getScrollY() + getPaddingTop() + mStickyOffset;
    }

    /**
     * 子View吸顶常驻
     *
     * @param children
     */
    @SuppressLint("NewApi")
    private void permanentStickyChild(List<View> children) {
        mTempStickyViews.clear();
        for (int i = 0; i < children.size(); i++) {
            View child = children.get(i);
            int permanentHeight = getPermanentHeight(children, i);
            if (getScrollY() > 0 && child.getTop() <= getStickyY() + permanentHeight) {
                child.setY(getStickyY() + permanentHeight);
                child.setTranslationZ(1);
                child.setClickable(true);
                mTempStickyViews.add(child);
            }
        }

        if (!isListEqual()) {
            mCurrentStickyViews.clear();
            mCurrentStickyViews.addAll(mTempStickyViews);
            mTempStickyViews.clear();
            permanentStickyChange(mCurrentStickyViews);
        }
    }

    private int getPermanentHeight(List<View> children, int currentPosition) {
        int height = 0;
        for (int i = 0; i < currentPosition; i++) {
            View child = children.get(i);
            height += child.getMeasuredHeight();
        }
        return height;
    }

    private boolean isListEqual() {
        if (mTempStickyViews.size() == mCurrentStickyViews.size()) {
            int size = mTempStickyViews.size();
            for (int i = 0; i < size; i++) {
                if (mTempStickyViews.get(i) != mCurrentStickyViews.get(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
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

    public OnScrollChangeListener setOnVerticalScrollChangeListener() {
        return mOnScrollChangeListener;
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
        return isIntercept(ScrollUtils.getRawX(this, ev, pointerIndex),
                ScrollUtils.getRawY(this, ev, pointerIndex));
    }

    /**
     * 判断是否需要拦截事件
     *
     * @return
     */
    private boolean isIntercept(int touchX, int touchY) {
        View target = getTouchTarget(touchX, touchY);
        if (target != null) {
            return ScrollUtils.isConsecutiveScrollerChild(target);
        }

        return false;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

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

        /**
         * 子view与父布局的对齐方式
         */
        public Align align = Align.LEFT;

        /**
         * 子view与父布局的对齐方式
         */
        public enum Align {
            //左对齐。（默认）
            LEFT(1),
            //右对齐。
            RIGHT(2),
            //中间对齐。
            CENTER(3);

            int value;

            Align(int value) {
                this.value = value;
            }

            static Align get(int value) {
                switch (value) {
                    case 1:
                        return LEFT;
                    case 2:
                        return RIGHT;
                    case 3:
                        return CENTER;
                }
                return LEFT;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = null;
            try {
                a = c.obtainStyledAttributes(attrs, R.styleable.ConsecutiveScrollerLayout_Layout);

                isConsecutive = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isConsecutive, true);
                isSticky = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isSticky, false);
                isNestedScroll = a.getBoolean(R.styleable.ConsecutiveScrollerLayout_Layout_layout_isNestedScroll, true);
                int type = a.getInt(R.styleable.ConsecutiveScrollerLayout_Layout_layout_align, 1);
                align = Align.get(type);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
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

    /**
     * 设置吸顶常驻
     *
     * @param isPermanent
     */
    public void setPermanent(boolean isPermanent) {
        if (this.isPermanent != isPermanent) {
            this.isPermanent = isPermanent;
            resetSticky();
        }
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    /**
     * 设置吸顶view到顶部的偏移量，允许吸顶view在距离顶部offset偏移量的地方吸顶停留。
     *
     * @param offset
     */
    public void setStickyOffset(int offset) {
        if (mStickyOffset != offset) {
            mStickyOffset = offset;
            resetSticky();
        }
    }

    public int getStickyOffset() {
        return mStickyOffset;
    }

    /**
     * 获取正在吸顶的view
     *
     * @return
     */
    public View getCurrentStickyView() {
        return mCurrentStickyView;
    }

    /**
     * 常驻模式下，获取正在吸顶的view
     *
     * @return
     */
    public List<View> getCurrentStickyViews() {
        return mCurrentStickyViews;
    }

    public OnStickyChangeListener getOnStickyChangeListener() {
        return mOnStickyChangeListener;
    }

    /**
     * 普通吸顶模式,监听吸顶变化
     *
     * @param l
     */
    public void setOnStickyChangeListener(OnStickyChangeListener l) {
        this.mOnStickyChangeListener = l;
    }

    public OnPermanentStickyChangeListener getOnPermanentStickyChangeListener() {
        return mOnPermanentStickyChangeListener;
    }

    /**
     * 常驻吸顶模式,监听吸顶变化
     *
     * @param l
     */
    public void setOnPermanentStickyChangeListener(OnPermanentStickyChangeListener l) {
        this.mOnPermanentStickyChangeListener = l;
    }

    /**
     * 滑动监听
     */
    public interface OnScrollChangeListener {
        void onScrollChange(View v, int scrollY, int oldScrollY);
    }

    /**
     * 监听吸顶变化
     */
    public interface OnStickyChangeListener {
        /**
         * @param oldStickyView 旧的吸顶view，可能为空
         * @param newStickyView 新的吸顶view，可能为空
         */
        void OnStickyChange(@Nullable View oldStickyView, @Nullable View newStickyView);
    }

    /**
     * 监听常驻吸顶变化
     */
    public interface OnPermanentStickyChangeListener {

        /**
         * @param mCurrentStickyViews 正在吸顶的view
         */
        void OnStickyChange(@NonNull List<View> mCurrentStickyViews);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
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
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
        checkTargetsScroll(false, false);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mParentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScrollInternal(dyUnconsumed, type);
    }

    private void onNestedScrollInternal(int dyUnconsumed, int type) {
        final int oldScrollY = mOwnScrollY;
        scrollBy(0, dyUnconsumed);
        final int myConsumed = mOwnScrollY - oldScrollY;
        final int myUnconsumed = dyUnconsumed - myConsumed;
        mChildHelper.dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            dispatchNestedFling(0, velocityY, true);
            fling((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        } else {
            return false;
        }
    }

}