package com.donkingliang.consecutivescrollerdemo;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

/**
 * @Author teach liang
 * @Description
 * @Date 2020/5/22
 */
public class FragmentActivity  extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();   // 开启一个事务
        transaction.replace(R.id.fragment_container, new MyFragment());
        transaction.commit();
    }
}
