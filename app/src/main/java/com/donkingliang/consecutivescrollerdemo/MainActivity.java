package com.donkingliang.consecutivescrollerdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.donkingliang.consecutivescroller.ConsecutiveScrollerLayout;
import com.donkingliang.consecutivescrollerdemo.adapter.ListViewAdapter;
import com.donkingliang.consecutivescrollerdemo.adapter.RecyclerViewAdapter;

public class MainActivity extends AppCompatActivity {

    RecyclerViewAdapter adapter;

    ListViewAdapter listViewAdapter;
    ConsecutiveScrollerLayout scrollerLayout;
    TextView text2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getItemCount() > 5) {
                    adapter.setCount(5);
                } else {
                    adapter.setCount(40);
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

        scrollerLayout = findViewById(R.id.scrollerLayout);
//        scrollView.setOnVerticalScrollChangeListener(new ConsecutiveScrollerLayout.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(View v, int scrollY, int oldScrollY) {
//                Log.e("eee", scrollY + " " + oldScrollY);
//            }
//        });
//
        text2 = findViewById(R.id.text2);
        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "TextView被点击", Toast.LENGTH_SHORT).show();
                ConsecutiveScrollerLayout.LayoutParams lp = (ConsecutiveScrollerLayout.LayoutParams)text2.getLayoutParams();
                lp.isSticky = !lp.isSticky;
                text2.setLayoutParams(lp);
            }
        });
        text2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConsecutiveScrollerLayout.LayoutParams lp = (ConsecutiveScrollerLayout.LayoutParams)text2.getLayoutParams();
                lp.isSticky = !lp.isSticky;
                text2.setLayoutParams(lp);
            }
        });
        findViewById(R.id.linearLayout).setClickable(true);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new RecyclerViewAdapter(this);
        adapter.setCount(30);
        recyclerView.setAdapter(adapter);

        ListView listView = findViewById(R.id.listView);
        listViewAdapter = new ListViewAdapter(this);
        listViewAdapter.setCount(10);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "item被点击", Toast.LENGTH_SHORT).show();
            }
        });

//        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(MainActivity.this,"item被长按",Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        });
        WebView webView = findViewById(R.id.webView);
        webView.loadUrl("https://github.com/donkingliang");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 在webView加载的过程中，用户滚动了webView内容，可能会使webView的显示与scrollerLayout断层，
                // 需要让scrollerLayout重新检查一下所有View的显示位置
                scrollerLayout.checkLayoutChange();
            }
        });

    }

}
