package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.SyncAdapterService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.api.Endpoints;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected LazyListView mListView;
    private ScTabView mTracklistView;
    private String mTrackingPath;
    private IntentFilter mSyncCheckFilter;
    private boolean mExclusive;

    public static final String SYNC_CHECK_ACTION = "com.soundcloud.android.eventforeground";


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (redirectToMain()) return;

        CloudUtils.checkState(this);

        setContentView(R.layout.main_holder);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            if ("incoming".equalsIgnoreCase(tab)) {
                mTracklistView = createList(Endpoints.MY_ACTIVITIES,
                        Event.class,
                        R.string.empty_incoming_text,
                        CloudUtils.ListId.LIST_INCOMING, false);
                mTrackingPath = "/incoming";
            } else if ("exclusive".equalsIgnoreCase(tab)) {
                mExclusive = true;
                mTracklistView = createList(Endpoints.MY_EXCLUSIVE_TRACKS,
                        Event.class,
                        R.string.empty_exclusive_text,
                        CloudUtils.ListId.LIST_EXCLUSIVE, true);
                mTrackingPath = "/exclusive";
            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
            setContentView(mTracklistView);
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            ((EventsAdapterWrapper) mTracklistView.adapter).restoreState(mPreviousState);
        }

        mSyncCheckFilter = new IntentFilter();
        mSyncCheckFilter.addAction(SYNC_CHECK_ACTION);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack(mTrackingPath);

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) getSoundCloudApplication().getSystemService(ns);

        nm.cancel(SyncAdapterService.DASHBOARD_NOTIFICATION_ID);

        if (mExclusive){
            getSoundCloudApplication().setAccountData(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN,"0");
        } else {
            getSoundCloudApplication().setAccountData(User.DataKeys.CURRENT_INCOMING_UNSEEN,"0");
        }

        registerReceiver(mIntentReceiver, mSyncCheckFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
    }

    protected ScTabView createList(String endpoint, Class<?> model, int emptyText, int listId, boolean exclusive) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), exclusive, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, endpoint, "collection");

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this, adpWrap);
        mListView = CloudUtils.createTabList(this, view, adpWrap, listId, null);
        return view;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return ((EventsAdapterWrapper) mTracklistView.adapter).saveState();
    }

    @Override
    public void onRefresh() {
        mTracklistView.onRefresh();
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(SYNC_CHECK_ACTION)) {
                this.setResultCode(Activity.RESULT_OK);
            }
        }
    };

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
}
