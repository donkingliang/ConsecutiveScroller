package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * @Author teach liang
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
        if (isAdjust() && mAdjustHeight > 0) {
            ConsecutiveScrollerLayout layout = (ConsecutiveScrollerLayout) getParent();
            int parentHeight = layout.getMeasuredHeight();
            int height = Math.min(parentHeight - mAdjustHeight, getDefaultSize(0, heightMeasureSpec));
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean isAdjust() {
        ViewParent parent = getParent();
        if (parent instanceof ConsecutiveScrollerLayout) {
            ConsecutiveScrollerLayout layout = (ConsecutiveScrollerLayout) parent;
            return layout.indexOfChild(this) == layout.getChildCount() - 1;
        }
        return false;
    }

    public int getAdjustHeight() {
        return mAdjustHeight;
    }

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
