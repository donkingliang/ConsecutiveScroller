package com.donkingliang.consecutivescrollerdemo.adapter;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Depiction: TabLayout 和 Fragment，viewpager结合使用的viewpager adapter。
 */
public class TabPagerAdapter extends FragmentStatePagerAdapter {

    private List<String> mTitles;
    private List<? extends Fragment> mFragments;

    public TabPagerAdapter(FragmentManager fm, List<String> titleList, List<? extends Fragment> fragments) {
        super(fm);
        this.mTitles = titleList;
        this.mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments == null ? 0 : mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles == null ? "" : mTitles.get(position);
    }
}
