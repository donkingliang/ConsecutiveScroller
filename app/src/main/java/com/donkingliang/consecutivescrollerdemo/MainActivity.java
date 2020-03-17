package com.donkingliang.consecutivescrollerdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescrollerdemo.adapter.ListViewAdapter;
import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;

public class MainActivity extends AppCompatActivity {

    RecyclerViewAdapter adapter;

    ListViewAdapter listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listViewAdapter.getCount() > 5) {
                    listViewAdapter.setCount(5);
                } else {
                    listViewAdapter.setCount(40);
                }
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = findViewById(R.id.text);
                if (view.getVisibility() == View.GONE) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        });

//        ConsecutiveScrollerLayout scrollView = findViewById(R.id.scrollView);
//        scrollView.setOnVerticalScrollChangeListener(new ConsecutiveScrollerLayout.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(View v, int scrollY, int oldScrollY) {
//                Log.e("eee", scrollY + " " + oldScrollY);
//            }
//        });
//
//        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                v.setVisibility(View.GONE);
//            }
//        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter(this);
        adapter.setCount(40);
        recyclerView.setAdapter(adapter);

        ListView listView = findViewById(R.id.listView);
        listViewAdapter = new ListViewAdapter(this);
        listViewAdapter.setCount(10);
        listView.setAdapter(listViewAdapter);


        WebView webView = findViewById(R.id.webView);
        webView.loadUrl("https://github.com/donkingliang");

    }

}
