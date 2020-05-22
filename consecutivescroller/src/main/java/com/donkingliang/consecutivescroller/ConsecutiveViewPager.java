package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

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

    public ConsecutiveViewPager(@NonNull Context context) {
        super(context);
    }

    public ConsecutiveViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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
        } else {
            views.add(this);
        }
        return views;
    }
}
