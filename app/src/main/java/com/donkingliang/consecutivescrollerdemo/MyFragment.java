package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;
import com.donkingliang.consecutivescrollerdemo.widget.MyRecyclerView;

/**
 * @Author teach-梁任彦
 * @Description
 * @Date 2020/4/18
 */
public class MyFragment extends Fragment {

    private ConsecutiveScrollerLayout mScrollerLayout;

    public MyFragment(ConsecutiveScrollerLayout scrollerLayout) {
        this.mScrollerLayout = scrollerLayout;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_item_list,container,false);

        MyRecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(getContext(),"ViewPager1-");
        list.setAdapter(adapter);
        list.setScrollerLayout(mScrollerLayout);
        return view;

    }
}
