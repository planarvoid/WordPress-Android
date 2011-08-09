package com.soundcloud.android.activity;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected ScListView mListView;
    private String mTrackingPath;
    private boolean mNews;
    private final String EXCLUSIVE_ONLY_KEY = "incoming_exclusive_only";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (redirectToMain()) return;

        CloudUtils.checkState(this);

        setContentView(R.layout.dashboard);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            ScTabView trackListView;
            if ("incoming".equalsIgnoreCase(tab)) {
                trackListView = createList(getIncomingRequest(),
                        Event.class,
                        R.string.empty_incoming_text,
                        Consts.ListId.LIST_INCOMING, false);
                mTrackingPath = Consts.TrackingEvents.INCOMING;
            } else if ("activity".equalsIgnoreCase(tab)) {
                mNews = true;
                trackListView = createList(Request.to(AndroidCloudAPI.MY_NEWS),
                        Event.class,
                        R.string.empty_news_text,
                        Consts.ListId.LIST_NEWS, true);
                mTrackingPath = Consts.TrackingEvents.ACTIVITY;
            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
            setContentView(trackListView);
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }

    private Request getIncomingRequest() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(EXCLUSIVE_ONLY_KEY, false)
                ? Request.to(Endpoints.MY_EXCLUSIVE_TRACKS)
                : Request.to(Endpoints.MY_ACTIVITIES);
    }



    @Override
    public void onResume() {
        super.onResume();
        pageTrack(mTrackingPath);

        ((NotificationManager) getApp().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(mNews ?
                        Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID :
                        Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);
    }

    protected ScTabView createList(Request endpoint, Class<?> model, int emptyText, int listId, boolean isNews) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), isNews, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, endpoint);

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this);
        mListView = view.setLazyListView(buildList(!isNews), adpWrap, listId, true);
        return view;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null && mListView.getWrapper() != null){
            return mListView.getWrapper().saveState();
        }
        return null;
    }

    // legacy action, redirect to Main
    private boolean redirectToMain() {
        if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            Intent intent = new Intent(this, Main.class);
            startActivity(intent);
            finish();
            return true;
        } else {
            return false;
        }
    }

    public void refreshIncoming() {
        mListView.onRefresh();
        mListView.smoothScrollToPosition(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         if (!mNews) {
            menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.FILTER:
                new AlertDialog.Builder(this)
                   .setTitle(getString(R.string.dashboard_filter_title))
                   .setNegativeButton(R.string.dashboard_filter_cancel, null)
                        .setItems(new String[]{getString(R.string.dashboard_filter_all), getString(R.string.dashboard_filter_exclusive)},
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit()
                                                .putBoolean(EXCLUSIVE_ONLY_KEY, which == 1).commit();
                                        mListView.getWrapper().setRequest(getIncomingRequest());
                                        mListView.getWrapper().reset();
                                        mListView.setLastUpdated(0);
                                        mListView.invalidateViews();
                                        mListView.post(new Runnable() {
                                            @Override public void run() {
                                                mListView.onRefresh();
                                            }
                                        });

                                    }
                                })

                   .create()
                   .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
