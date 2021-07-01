package com.donkingliang.consecutivescroller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
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

    private static final int TAG_KEY = -123;

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

    /**
     * 是否在ConsecutiveScrollerLayout的底部
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
        View scrollerView = null;
        int currentItem = getCurrentItem();
        Adapter adapter = mRecyclerView.getAdapter();
        LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (adapter != null && layoutManager != null) {
            if (currentItem >= 0 && currentItem < adapter.getItemCount()) {
                View itemView = layoutManager.findViewByPosition(currentItem);
                scrollerView = findScrolledItemView(itemView);
                if (scrollerView != null) {
                    setAttachListener(scrollerView);
                }
            }
        }

        if (scrollerView == null) {
            scrollerView = mRecyclerView;
        }
        return scrollerView;
    }

    /**
     * 给scrollerView添加OnAttachStateChange监听，在item添加到屏幕是检查scrollerView滑动位置，放在布局显示断层。
     *
     * @param scrollerView
     */
    private void setAttachListener(View scrollerView) {
        if (scrollerView.getTag(TAG_KEY) != null) {
            AttachListener listener = (AttachListener) scrollerView.getTag(TAG_KEY);
            if (listener.reference.get() == null) {
                // 情况原来的监听器
                scrollerView.removeOnAttachStateChangeListener(listener);
                scrollerView.setTag(TAG_KEY, null);
            }
        }

        if (scrollerView.getTag(TAG_KEY) == null) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp instanceof ConsecutiveScrollerLayout.LayoutParams) {
                if (((ConsecutiveScrollerLayout.LayoutParams) lp).isConsecutive) {
                    AttachListener l = new AttachListener(this, scrollerView);
                    scrollerView.addOnAttachStateChangeListener(l);
                    scrollerView.setTag(TAG_KEY, l);
                }
            }
        }
    }

    private void scrollChildContent(View v) {
        if (v != null && getParent() instanceof ConsecutiveScrollerLayout) {
            ConsecutiveScrollerLayout parent = (ConsecutiveScrollerLayout) getParent();
            int thisIndex = parent.indexOfChild(this);

            // 判断是否需要滑动内容到底部或顶部

            if (thisIndex == parent.getChildCount() - 1
                    && getHeight() < parent.getHeight()
                    && parent.getScrollY() >= parent.mScrollRange) {
                return;
            }

            View firstVisibleView = parent.findFirstVisibleView();
            if (firstVisibleView == null) {
                return;
            }
            int firstIndex = parent.indexOfChild(firstVisibleView);

            if (thisIndex < firstIndex) {
                parent.scrollChildContentToBottom(v);
            } else if (thisIndex > firstIndex) {
                parent.scrollChildContentToTop(v);
            }
        }
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

    protected View findScrolledItemView(View view) {
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

    @Override
    public boolean canScrollHorizontally(int direction) {
        return mViewPager2.canScrollHorizontally(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mViewPager2.canScrollVertically(direction);
    }

    private static class AttachListener implements OnAttachStateChangeListener {

        WeakReference<ConsecutiveViewPager2> reference;
        View view;

        public AttachListener(ConsecutiveViewPager2 parent, View view) {
            reference = new WeakReference<ConsecutiveViewPager2>(parent);
            this.view = view;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            if (reference.get() != null) {
                reference.get().scrollChildContent(view);
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    }
}
