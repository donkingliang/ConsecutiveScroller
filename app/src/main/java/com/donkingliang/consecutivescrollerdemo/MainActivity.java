package com.donkingliang.consecutivescrollerdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_sample).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SampleActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_sticky).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,StickyActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_sink_sticky).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SinkStickyActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_consecutive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ConsecutiveActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_scroll_child).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ScrollChildActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_viewpager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ViewPagerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_viewpager2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ViewPager2Activity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_sticky_permanent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PermanentStickyActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_fragment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FragmentActivity.class);
                startActivity(intent);
            }
        });
    }

}
