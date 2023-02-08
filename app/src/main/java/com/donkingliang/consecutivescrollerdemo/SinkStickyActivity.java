package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SinkStickyActivity extends AppCompatActivity {

    private ConsecutiveScrollerLayout scrollerLayout;
    private FrameLayout flSink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sink_sticky);

        scrollerLayout = findViewById(R.id.scrollerLayout);

        flSink = findViewById(R.id.fl_sink);

        RecyclerView recyclerView1 = findViewById(R.id.recyclerView1);
        recyclerView1.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewAdapter adapter1 = new RecyclerViewAdapter(this,"RecyclerView1-");
        recyclerView1.setAdapter(adapter1);

        RecyclerView recyclerView2 = findViewById(R.id.recyclerView2);
        recyclerView2.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewAdapter adapter2 = new RecyclerViewAdapter(this,"RecyclerView2-");
        recyclerView2.setAdapter(adapter2);

        // 监听滑动
        scrollerLayout.setOnVerticalScrollChangeListener(new ConsecutiveScrollerLayout.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollY, int oldScrollY, int scrollState) {
                if (scrollY > flSink.getHeight() || scrollY < 0){
                    scrollerLayout.setStickyOffset(0); // 恢复吸顶偏移量
                } else {
                    // 通过设置吸顶便宜量，实现flSink滑动隐藏时的向上移动效果
                    scrollerLayout.setStickyOffset(-scrollY / 2);
                }
            }
        });


        // 如果flSink不需要设置隐藏时的平移动画，就不用设置滑动监听和setStickyOffset

        // 如果要禁止滑动flSink来滑动整个布局，flSink不用设置app:layout_isTriggerScroll="true"即可。
    }
}
