package com.donkingliang.consecutivescrollerdemo.view;

import android.content.Context;
import android.util.AttributeSet;

import com.donkingliang.consecutivescroller.IConsecutiveScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * @Author teach liang
 * @Description
 * @Date 2020/3/19
 */
public class MyScrollView extends NestedScrollView implements IConsecutiveScroller {

    public MyScrollView(@NonNull Context context) {
        super(context);
    }

    public MyScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isConsecutiveScroller() {
        return false;
    }
}
