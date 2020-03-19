package com.donkingliang.consecutivescrollerdemo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.donkingliang.consecutivescroller.IConsecutiveScroller;

import androidx.annotation.Nullable;

/**
 * @Author teach liang
 * @Description
 * @Date 2020/3/19
 */
public class MyLinearLayout extends LinearLayout implements IConsecutiveScroller {

    public MyLinearLayout(Context context) {
        super(context);
    }

    public MyLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean isConsecutiveScroller() {
        return false;
    }
}
