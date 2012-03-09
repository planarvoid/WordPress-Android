package com.soundcloud.android.view;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.task.create.NewConnectionTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class ConnectionList extends LinearLayout {
    private Adapter listAdapter;
    private View footer;
    private View errorPlaceholder;

    public ConnectionList(Context context) {
        super(context);
        setLayoutParams();
    }

    /** @noinspection UnusedDeclaration*/
    public ConnectionList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutParams();
    }

    private void setLayoutParams() {
        setOrientation(LinearLayout.VERTICAL);
    }

    protected void handleDataChanged() {
        removeAllViews();

        if (!listAdapter.mFailed) {
            for (int i = 0; i < listAdapter.getCount(); i++) {
                if (listAdapter.getItem(i).service().enabled) {
                    View item = listAdapter.getView(i, null, this);
                    addView(item);
                    addView(getSeparator());
                }
            }
            addView(getFooter());
        } else {
            addView(getErrorPlaceholder());
        }
    }

    public void setAdapter(Adapter listAdapter) {
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

    public Adapter getAdapter() {
        return this.listAdapter;
    }

    public List<Integer> postToServiceIds() {
        List<Integer> ids = new ArrayList<Integer>();
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

        v.setBackgroundColor(getResources().getColor(R.color.recordUploadBorder));
        return v;
    }

    protected View getFooter() {
        if (footer == null) {
            footer = inflate(getContext(), R.layout.connection_list_footer, null);
        }
        return footer;
    }

    protected View getErrorPlaceholder() {
        if (errorPlaceholder == null) {
            errorPlaceholder = inflate(getContext(), R.layout.connection_list_error_placeholder, null);
        }
        return errorPlaceholder;
    }

    public static class Adapter extends BaseAdapter {
        private AndroidCloudAPI api;
        private List<Connection> mConnections;
        private boolean mFailed;

        public Adapter(AndroidCloudAPI api) {
            this.api = api;
        }

        @Override
        public int getCount() {
            return mConnections == null ? 0 : mConnections.size();
        }

        @Override
        public Connection getItem(int position) {
            return mConnections.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void setConnections(List<Connection> connections) {
            setConnections(connections, false);
        }

        public void setConnections(List<Connection> connections, boolean addUnused) {
            mConnections = addUnused ? Connection.addUnused(connections) : connections;
            notifyDataSetChanged();
            mFailed = false;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            return new ConnectionItem(parent.getContext(), getItem(position)) {

                @Override
                public void configureService(final Connection.Service service) {
                    new NewConnectionTask(api) {
                        @Override
                        protected void onPreExecute() {
                            progress(false);
                        }

                        @Override
                        protected void onPostExecute(Uri uri) {
                            progress(true);
                            if (uri != null) {
                                ((Activity) parent.getContext()).startActivityForResult(
                                        (new Intent(parent.getContext(), Connect.class))
                                                .putExtra("service", service.name())
                                                .setData(uri),
                                        Connect.MAKE_CONNECTION);
                            } else {
                                Toast toast = Toast.makeText(parent.getContext(),
                                        parent.getResources().getString(R.string.new_connection_error),
                                        Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        }
                    }.execute(service);
                }
            };
        }

        public Adapter loadIfNecessary() {
            if (mFailed || mConnections == null) load();
            return this;
        }

        public Adapter load() {
            // try to use cached connections first
            final List<Connection> connections = Connections.get().getObjectsOrNull();

            if (connections != null) setConnections(connections, true);
            // request update, but only use it if no cached connections were fetched
            Connections.get().requestUpdate(api, true,
                new ParcelCache.Listener<Connection>() {
                        @Override public void onChanged(List<Connection> objects, ParcelCache<Connection> cache) {
                            if (objects != null) {
                                setConnections(objects, true);
                            } else {
                                mFailed = true;
                                notifyDataSetChanged();
                            }
                        }
                    });
            return this;
        }
    }
}
