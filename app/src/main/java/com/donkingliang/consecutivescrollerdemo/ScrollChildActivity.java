package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;

import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ScrollChildActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_child);

        RecyclerView recyclerView1 = findViewById(R.id.recyclerView1);
        recyclerView1.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewAdapter adapter1 = new RecyclerViewAdapter(this,"RecyclerView1-");
        recyclerView1.setAdapter(adapter1);

        RecyclerView recyclerView2 = findViewById(R.id.recyclerView2);
        recyclerView2.setLayoutManager(new LinearLayoutManager(this));
        RecyclerViewAdapter adapter2 = new RecyclerViewAdapter(this,"RecyclerView2-");
        recyclerView2.setAdapter(adapter2);
    }
}
