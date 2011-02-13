package com.soundcloud.android.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ConnectActivity;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.task.LoadConnectionsTask;
import com.soundcloud.android.task.NewConnectionTask;

import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;


public class ConnectionList extends LinearLayout {
    private Adapter listAdapter;
    private AttributeSet attrs;

    public ConnectionList(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
    }

    public ConnectionList(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attrs = attrs;
        setOrientation(LinearLayout.VERTICAL);
    }


    protected void handleDataChanged() {
        Log.d(TAG, "handleDataChanged()");

        removeAllViews();

        for (int i = 0; i < listAdapter.getCount(); i++) {
            if (listAdapter.getItem(i).service().enabled) {
                View item = listAdapter.getView(i, null, this);
                addView(item);
                addView(getSeparator());
            }
        }
        addView(getFooter());
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
        TextView footer = new TextView(getContext(), attrs, R.style.connection_list_header);
        footer.setText(R.string.connection_list_footer);
        footer.setTextColor(getResources().getColor(R.color.background_light));
        return footer;
    }

    public static class Adapter extends BaseAdapter {
        private CloudAPI api;
        private List<Connection> mConnections;
        private boolean mFailed;

        public Adapter(CloudAPI api) {
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
            this.mConnections = connections;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            return new ConnectionItem(parent.getContext(), getItem(position)) {
                @Override
                public void configureService(Connection.Service service) {
                    Log.d(TAG, "configure service " + service);
                    new NewConnectionTask(api) {
                        @Override
                        protected void onPostExecute(Uri uri) {
                            if (uri != null) {
                                parent.getContext().startActivity(
                                        (new Intent(parent.getContext(), ConnectActivity.class)).setData(uri));
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

        public void clear() {
            mConnections = null;
        }

        public Adapter loadIfNecessary() {
            if (mFailed || mConnections == null) load();
            return this;
        }

        public Adapter load() {
            new LoadConnectionsTask(api) {
                @Override
                protected void onPreExecute() {
                    Log.v(TAG, "loading connections");
                }

                @Override
                protected void onPostExecute(List<Connection> connections) {
                    if (connections != null) {
                        mFailed = false;
                        setConnections(Connection.addUnused(connections));
                    } else {
                        mFailed = true;
                        setConnections(Connection.addUnused(new ArrayList<Connection>()));
                    }
                }
            }.execute();
            return this;
        }
    }
}
