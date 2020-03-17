package com.donkingliang.consecutivescrollerdemo.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.donkingliang.consecutivescrollerdemo.R;

/**
 * @Author donkingliang
 * @Description
 * @Date 2020/3/17
 */
public class ListViewAdapter extends BaseAdapter {

    private Context context;

    private int count;

    public ListViewAdapter(Context context) {
        this.context = context;
    }

    public void setCount(int count) {
        this.count = count;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ListViewAdapter.ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_recycler_item, parent, false);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ListViewAdapter.ViewHolder) convertView.getTag();
        }

        return convertView;
    }

    private final class ViewHolder {

    }

}
