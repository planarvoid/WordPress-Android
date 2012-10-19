package com.soundcloud.android.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
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
    final int rowResourceId;
    private List<Search> data = new ArrayList<Search>();

    public SearchHistoryAdapter(Context context, int rowResourceId) {
        this.context = context;
        this.rowResourceId = rowResourceId;
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
            row = inflater.inflate(rowResourceId, parent, false);

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
                holder.iv_search_type.setImageResource(R.drawable.ic_search_user);
                break;
        }
        return row;
    }

    static class SearchHolder {
        ImageView iv_search_type;
        TextView tv_query;
    }

    @SuppressWarnings("unchecked")
    public static AsyncTask refreshHistory(final ContentResolver resolver,
                                           final SearchHistoryAdapter adapter,
                                           final Search... toInsert) {
        return new AsyncTask<Void, Void, List<Search>>() {
            @Override
            protected List<Search> doInBackground(Void... params) {
                if (toInsert != null) for (Search s : toInsert) s.insert(resolver);
                return Search.getHistory(resolver);
            }

            @Override
            protected void onPostExecute(List<Search> searches) {
                if (searches != null) {
                    for (Search searchDefault : SEARCH_DEFAULTS) {
                        if (!searches.contains(searchDefault)) searches.add(searchDefault);
                    }
                    adapter.setData(searches);
                }
            }
        }.execute();
    }

    public static final Search[] SEARCH_DEFAULTS = new Search[]{
            new Search("Comedy show", Search.SOUNDS),
            new Search("Bird calls", Search.SOUNDS),
            new Search("Ambient", Search.SOUNDS),
            new Search("Rap", Search.SOUNDS),
            new Search("Garage rock", Search.SOUNDS),
            new Search("Thunder storm", Search.SOUNDS),
            new Search("Snoring", Search.SOUNDS),
            new Search("Goa", Search.SOUNDS),
            new Search("Nature sounds", Search.SOUNDS),
            new Search("dubstep", Search.SOUNDS),
    };
}
