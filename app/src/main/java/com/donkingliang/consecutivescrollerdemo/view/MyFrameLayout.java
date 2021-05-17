package com.donkingliang.consecutivescrollerdemo.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.donkingliang.consecutivescroller.IConsecutiveScroller;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * @Author teach liang
 * @Description
 * @Date 2021/5/17
 */
public class MyFrameLayout extends FrameLayout implements IConsecutiveScroller {

    public MyFrameLayout(@NonNull Context context) {
        super(context);
    }

    public MyFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MyFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public View getCurrentScrollerView() {
        // 返回需要滑动的ScrollView
        return getChildAt(0);
    }

    @Override
    public List<View> getScrolledViews() {
        // 返回需要滑动的ScrollView
        List<View> views = new ArrayList<>();
        views.add(getChildAt(0));
        return views;
    }
}
