package com.donkingliang.consecutivescrollerdemo.adapter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/11/5
 */
public class MyFragmentPagerAdapter extends FragmentStateAdapter {

    private List<? extends Fragment> mFragments;

    public MyFragmentPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<? extends Fragment> fragments) {
        super(fragmentActivity);
        this.mFragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragments.size();
    }
}
