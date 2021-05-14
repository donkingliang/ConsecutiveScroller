package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescroller.ConsecutiveViewPager;
import com.donkingliang.consecutivescrollerdemo.adapter.TabPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.scwang.smart.refresh.layout.simple.SimpleMultiListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ViewPagerActivity extends AppCompatActivity {

    private ConsecutiveScrollerLayout scrollerLayout;
    private ConsecutiveViewPager viewPager;
    private TabLayout tabLayout;
    private SmartRefreshLayout refreshLayout;

    private TabPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        TextView text = findViewById(R.id.text);
        text.setText("子view通过实现IConsecutiveScroller接口，可以使ConsecutiveScrollerLayout能正确地处理子view的下级view的滑动事件。\n" +
                "下面的例子中，通过自定义ViewPager，实现IConsecutiveScroller接口，ConsecutiveScrollerLayout能正确的处理ViewPager里" +
                "的子布局。如果ViewPager的内容是可以垂直滑动的，请使用ConsecutiveScrollerLayout或者RecyclerView等可滑动布局作为它内容的根布局。\n" +
                "下面的列子中使用ViewPager承载多个Fragment，Fragment的根布局为ConsecutiveScrollerLayout。");
        scrollerLayout = findViewById(R.id.scrollerLayout);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        refreshLayout = findViewById(R.id.refreshLayout);

        mAdapter = new TabPagerAdapter(getSupportFragmentManager(), getTabs(), getFragments());
        viewPager.setAdapter(mAdapter);
        tabLayout.setupWithViewPager(viewPager);

        refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout r) {
                // 把加载的动作传给当初的fragment
                MyFragment fragment = (MyFragment) mAdapter.getItem(viewPager.getCurrentItem());
                fragment.onLoadMore(refreshLayout);
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout r) {
                refreshLayout.finishRefresh(5000);
            }
        });

        refreshLayout.setOnMultiListener(new SimpleMultiListener() {
            @Override
            public void onFooterMoving(RefreshFooter footer, boolean isDragging, float percent, int offset, int footerHeight, int maxDragHeight) {
                // 上拉加载时，保证吸顶头部不被推出屏幕。
                Log.e("eee","****" + offset);
                scrollerLayout.setStickyOffset(offset);
            }
        });
    }

    // 提供给Fragment获取使用。
    public SmartRefreshLayout getRefreshLayout() {
        return refreshLayout;
    }

    private List<String> getTabs() {
        List<String> tabs = new ArrayList<>();
        tabs.add("Tab1");
        tabs.add("Tab2");
        tabs.add("Tab3");
        tabs.add("Tab4");
        tabs.add("Tab5");
        return tabs;
    }

    private List<Fragment> getFragments() {
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new MyFragment());
        fragmentList.add(new MyFragment());
        fragmentList.add(new MyFragment());
        fragmentList.add(new MyFragment());
        fragmentList.add(new MyFragment());
        return fragmentList;
    }
}
