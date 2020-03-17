package com.donkingliang.consecutivescrollerdemo.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.donkingliang.consecutivescrollerdemo.R;

import java.util.List;
import java.util.logging.Logger;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/3/17
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private Context context;
    private List<String> list;

    private int count;

    public RecyclerViewAdapter(Context context) {
        this.context = context;
    }

    public void setList(List<String> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setCount(int count) {
        this.count = count;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return count;
//        return list == null ? 0 : list.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
