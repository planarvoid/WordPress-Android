package com.soundcloud.android.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.task.LoadConnectionsTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.soundcloud.android.SoundCloudApplication.TAG;


public class ConnectionList extends LinearLayout {
    private ListAdapter listAdapter;

    public ConnectionList(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
    }

    public ConnectionList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
    }

    protected void handleDataChanged() {
        Log.d(TAG, "handleDataChanged()");

        removeAllViews();
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View item = listAdapter.getView(i, null, this);
            addView(item);
            addView(getSeparator());
        }
    }

    public void setAdapter(ListAdapter listAdapter) {
        this.listAdapter = listAdapter;
        listAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                handleDataChanged();
            }

            @Override
            public void onInvalidated() {
                invalidate();
            }
        });
    }

    public Set<Integer> postToServiceIds() {
        Set<Integer> ids = new HashSet<Integer>();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof ConnectionItem && v.isEnabled()) {
                ids.add(((ConnectionItem) v).connection.id);
            }
        }
        return ids;
    }


    private View getSeparator() {
        final View v = new View(this.getContext());
        v.setLayoutParams(new LayoutParams(
                LayoutParams.FILL_PARENT,
                1));

        v.setBackgroundColor(getResources().getColor(R.color.background_light));
        return v;
    }

    public static class Adapter extends BaseAdapter {
        private List<Connection> connections;


        @Override
        public int getCount() {
            return connections == null ? 0 : connections.size();
        }

        @Override
        public Connection getItem(int position) {
            return connections.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return new ConnectionItem(parent.getContext(), getItem(position));
        }

        public Adapter load(CloudAPI api) {
            new LoadConnectionsTask(api) {
                @Override
                protected void onPostExecute(List<Connection> connections) {
                    Adapter.this.connections = Connection.addUnused(connections);
                    notifyDataSetChanged();
                }
            }.execute();
            return this;
        }
    }
}
