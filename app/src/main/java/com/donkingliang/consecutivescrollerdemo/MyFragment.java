package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/4/18
 */
public class MyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_item_list,container,false);

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(getContext(),"ViewPager1-");
        list.setAdapter(adapter);
        return view;

    }
}
