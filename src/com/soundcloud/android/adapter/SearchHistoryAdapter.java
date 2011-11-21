package com.soundcloud.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SearchHistoryItem;

import java.util.ArrayList;

public class SearchHistoryAdapter extends BaseAdapter {


    final Context context;
    int layoutResourceId;
    final ArrayList<SearchHistoryItem> data = new ArrayList<SearchHistoryItem>();

    public SearchHistoryAdapter(Context context) {
        this.context = context;
    }

    public ArrayList<SearchHistoryItem> getData() {
        return data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        WeatherHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.search_history_row, parent, false);

            holder = new WeatherHolder();
            holder.iv_search_type = (ImageView) row.findViewById(R.id.iv_search_type);
            holder.tv_query = (TextView) row.findViewById(R.id.tv_query);

            row.setTag(holder);
        } else {
            holder = (WeatherHolder) row.getTag();
        }

        SearchHistoryItem history = data.get(position);
        holder.tv_query.setText(history.query);
        switch (history.search_type) {
            case 0:
                holder.iv_search_type.setImageResource(R.drawable.ic_search_sound);
                break;
            case 1:
                holder.iv_search_type.setImageResource(R.drawable.ic_search_people);
                break;
        }

        return row;
    }

    static class WeatherHolder {
        ImageView iv_search_type;
        TextView tv_query;
        //TextView tv_created_at;
    }
}
