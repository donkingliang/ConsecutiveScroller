package com.donkingliang.consecutivescrollerdemo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;

/**
 * @Author teach-梁任彦
 * @Description
 * @Date 2020/4/18
 */
public class MyRecyclerView extends RecyclerView {

    private ConsecutiveScrollerLayout mScrollerLayout;

    private int mTouchY;

    public MyRecyclerView(@NonNull Context context) {
        super(context);
    }

    public MyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScrollerLayout(ConsecutiveScrollerLayout layout){
        mScrollerLayout = layout;
    }


}
