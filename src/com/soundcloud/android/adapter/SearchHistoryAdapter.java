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
import com.soundcloud.android.model.Search;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryAdapter extends BaseAdapter {
    final Context context;
    private List<Search> data = new ArrayList<Search>();

    public SearchHistoryAdapter(Context context) {
        this.context = context;
    }

    public List<Search> getData() {
        return data;
    }

    public void setData(List<Search> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Search getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SearchHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.search_history_row, parent, false);

            holder = new SearchHolder();
            holder.iv_search_type = (ImageView) row.findViewById(R.id.iv_search_type);
            holder.tv_query = (TextView) row.findViewById(R.id.tv_query);

            row.setTag(holder);
        } else {
            holder = (SearchHolder)row.getTag();
        }

        Search history = data.get(position);
        holder.tv_query.setText(history.query);
        switch (history.search_type) {
            case Search.SOUNDS:
                holder.iv_search_type.setImageResource(R.drawable.ic_search_sound);
                break;
            case Search.USERS:
                holder.iv_search_type.setImageResource(R.drawable.ic_search_people);
                break;
        }
        return row;
    }

    static class SearchHolder {
        ImageView iv_search_type;
        TextView tv_query;
    }
}
