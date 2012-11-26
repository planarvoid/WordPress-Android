package com.soundcloud.android.view.create;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Connect;
import com.soundcloud.android.cache.ConnectionsCache;
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
import java.util.Set;


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

    public List<Long> postToServiceIds() {
        List<Long> ids = new ArrayList<Long>();
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

        public void setConnections(Set<Connection> connections) {
            setConnections(connections, false);
        }

        public void setConnections(Set<Connection> connections, boolean addUnused) {
            mConnections = addUnused ? Connection.addUnused(connections) : (List<Connection>) connections;
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
                                        Consts.RequestCodes.MAKE_CONNECTION);
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

        public Adapter loadIfNecessary(Context context) {
            if (mFailed || mConnections == null) load(context);
            return this;
        }

        public Adapter load(Context context) {
            // try to use cached connections first
            final Set<Connection> connections = ConnectionsCache.get(context).getConnections();

            if (connections != null) setConnections(connections, true);
            // request update, but only use it if no cached connections were fetched
            ConnectionsCache.get(context).requestConnections(
                    new ConnectionsCache.Listener() {
                        @Override
                        public void onChange(boolean success, ConnectionsCache status) {
                            final Set<Connection> connections1 = status.getConnections();
                            if (connections1 != null) {
                                setConnections(connections1, true);
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
