package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/5/22
 */
public class ConsecutiveViewPager extends ViewPager implements IConsecutiveScroller {

    private int mAdjustHeight;

    public ConsecutiveViewPager(@NonNull Context context) {
        super(context);
    }

    public ConsecutiveViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isConsecutiveParentAndBottom() && mAdjustHeight > 0) {
            int height = getDefaultSize(0, heightMeasureSpec) - mAdjustHeight;
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        // 去掉子View的滚动条。选择在这里做这个操作，而不是在onFinishInflate方法中完成，是为了兼顾用代码add子View的情况

        if (ScrollUtils.isConsecutiveScrollerChild(this)) {
            disableChildScroll(child);

            if (child instanceof ViewGroup) {
                ((ViewGroup) child).setClipToPadding(false);
            }
        }
    }

    /**
     * 禁用子view的一下滑动相关的属性
     *
     * @param child
     */
    private void disableChildScroll(View child) {
        child.setVerticalScrollBarEnabled(false);
        child.setHorizontalScrollBarEnabled(false);
        child.setOverScrollMode(OVER_SCROLL_NEVER);
        ViewCompat.setNestedScrollingEnabled(child, false);
    }

    /**
     * 是否在ConsecutiveScrollerLayout的底部
     *
     * @return
     */
    private boolean isConsecutiveParentAndBottom() {
        ViewParent parent = getParent();
        if (parent instanceof ConsecutiveScrollerLayout) {
            ConsecutiveScrollerLayout layout = (ConsecutiveScrollerLayout) parent;
            return layout.indexOfChild(this) == layout.getChildCount() - 1;
        }
        return false;
    }

    /**
     * @deprecated
     */
    public int getAdjustHeight() {
        return mAdjustHeight;
    }

    /**
     * @deprecated 如果你想调整ViewPager的高度，使它不被顶部吸顶view或者其他布局覆盖，
     * 请使用ConsecutiveScrollerLayout的autoAdjustHeightAtBottomView和adjustHeightOffset属性。
     */
    public void setAdjustHeight(int adjustHeight) {
        if (mAdjustHeight != adjustHeight) {
            mAdjustHeight = adjustHeight;
            requestLayout();
        }
    }

    /**
     * 返回当前需要滑动的view。
     *
     * @return
     */
    @Override
    public View getCurrentScrollerView() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view.getX() == getScrollX() + getPaddingLeft()) {
                return view;
            }
        }
        return this;
    }

    /**
     * 返回全部需要滑动的下级view
     *
     * @return
     */
    @Override
    public List<View> getScrolledViews() {
        List<View> views = new ArrayList<>();
        int count = getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                views.add(getChildAt(i));
            }
        }
        return views;
    }
}
