package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

/**
 * @Author donkingliang
 * @Description Viewpager2的包装类，它不是ViewPager2的子类，而且一个包含ViewPager2的容器，提供了ViewPager2的所有功能，
 * 同时实现IConsecutiveScroller接口，使它能配合ConsecutiveScrollerLayout的滑动处理。
 * 因为Viewpager2不能被继承，所有使用包装类的方式来解决Viewpager2和ConsecutiveScrollerLayout的滑动冲突。
 * @Date 2020/11/6
 */
public class ConsecutiveViewPager2 extends FrameLayout implements IConsecutiveScroller {

    protected ViewPager2 mViewPager2;
    protected RecyclerView mRecyclerView;

    private int mAdjustHeight;

    public ConsecutiveViewPager2(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public ConsecutiveViewPager2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ConsecutiveViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        mViewPager2 = new ViewPager2(context);
        addView(mViewPager2, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mRecyclerView = (RecyclerView) mViewPager2.getChildAt(0);
        mViewPager2.setOffscreenPageLimit(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isConsecutiveParent() && mAdjustHeight > 0) {
            int height = getDefaultSize(0, heightMeasureSpec) - mAdjustHeight;
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean isConsecutiveParent() {
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
        int currentItem = getCurrentItem();
        Adapter adapter = mRecyclerView.getAdapter();
        LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (adapter != null && layoutManager != null) {
            if (currentItem >= 0 && currentItem < adapter.getItemCount()) {
                View itemView = layoutManager.findViewByPosition(currentItem);
                return findScrolledItemView(itemView);
            }
        }
        return mRecyclerView;
    }

    /**
     * 返回全部需要滑动的下级view
     *
     * @return
     */
    @Override
    public List<View> getScrolledViews() {
        List<View> views = new ArrayList<>();
        int count = mRecyclerView.getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                views.add(findScrolledItemView(mRecyclerView.getChildAt(i)));
            }
        }
        return views;
    }

    private View findScrolledItemView(View view) {
        if (mRecyclerView.getAdapter() instanceof FragmentStateAdapter
                && view instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) view;
            if (frameLayout.getChildCount() > 0) {
                return frameLayout.getChildAt(0);
            }
        }
        return view;
    }

    public ViewPager2 getViewPager2() {
        return mViewPager2;
    }

    public void setAdapter(@Nullable @SuppressWarnings("rawtypes") RecyclerView.Adapter adapter) {
        mViewPager2.setAdapter(adapter);
    }

    public @Nullable
    RecyclerView.Adapter getAdapter() {
        return mViewPager2.getAdapter();
    }

    public void setOrientation(@ViewPager2.Orientation int orientation) {
        mViewPager2.setOrientation(orientation);
    }

    public @ViewPager2.Orientation
    int getOrientation() {
        return mViewPager2.getOrientation();
    }

    public void setCurrentItem(int item) {
        mViewPager2.setCurrentItem(item);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        mViewPager2.setCurrentItem(item, smoothScroll);
    }

    public int getCurrentItem() {
        return mViewPager2.getCurrentItem();
    }

    public void setOffscreenPageLimit(@ViewPager2.OffscreenPageLimit int limit) {
        mViewPager2.setOffscreenPageLimit(limit);
    }

    @ViewPager2.OffscreenPageLimit
    public int getOffscreenPageLimit() {
        return mViewPager2.getOffscreenPageLimit();
    }

    public void registerOnPageChangeCallback(@NonNull ViewPager2.OnPageChangeCallback callback) {
        mViewPager2.registerOnPageChangeCallback(callback);
    }

    public void unregisterOnPageChangeCallback(@NonNull ViewPager2.OnPageChangeCallback callback) {
        mViewPager2.unregisterOnPageChangeCallback(callback);
    }
}
