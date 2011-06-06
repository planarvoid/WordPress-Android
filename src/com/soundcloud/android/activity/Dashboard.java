package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected LazyListView mListView;
    private ScTabView mTracklistView;
    private String mTrackingPath;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (redirectToMain()) return;

        CloudUtils.checkState(this);

        setContentView(R.layout.main_holder);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            if ("incoming".equalsIgnoreCase(tab)) {
                mTracklistView = createList(Request.to(Endpoints.MY_ACTIVITIES),
                        Event.class,
                        R.string.empty_incoming_text,
                        CloudUtils.ListId.LIST_INCOMING, false);
                mTrackingPath = "/incoming";
            } else if ("exclusive".equalsIgnoreCase(tab)) {
                mTracklistView = createList(Request.to(Endpoints.MY_EXCLUSIVE_TRACKS),
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
    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack(mTrackingPath);
    }

    protected ScTabView createList(Request endpoint, Class<?> model, int emptyText, int listId, boolean exclusive) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), exclusive, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, endpoint);

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this, adpWrap);
        mListView = CloudUtils.createTabList(this, view, adpWrap, listId, null);
        return view;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mTracklistView != null && mTracklistView.adapter instanceof EventsAdapterWrapper){
            return ((EventsAdapterWrapper) mTracklistView.adapter).saveState();
        }
        return null;
    }

    @Override
    public void onRefresh() {
        mTracklistView.onRefresh();
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
}
