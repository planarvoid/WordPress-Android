package com.soundcloud.android.activity;

import android.app.NotificationManager;
import android.content.Context;
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

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected ScListView mListView;
    private ScTabView mTracklistView;
    private String mTrackingPath;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (redirectToMain()) return;

        CloudUtils.checkState(this);

        setContentView(R.layout.dashboard);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            if ("incoming".equalsIgnoreCase(tab)) {
                mTracklistView = createList(Request.to(Endpoints.MY_ACTIVITIES),
                        Event.class,
                        R.string.empty_incoming_text,
                        Consts.ListId.LIST_INCOMING, false);
                mTrackingPath = "/incoming";
            } else if ("news".equalsIgnoreCase(tab)) {
                //TODO replace with proper endpoint
                mTracklistView = createList(Request.to("/me/activities/all/own"),
                        Event.class,
                        R.string.empty_news_text,
                        Consts.ListId.LIST_NEWS, true);
                mTrackingPath = "/news";
            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
            setContentView(mTracklistView);
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        pageTrack(mTrackingPath);

        ((NotificationManager) getApp().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(Consts.Notifications.DASHBOARD_NOTIFY_ID);
    }

    protected ScTabView createList(Request endpoint, Class<?> model, int emptyText, int listId, boolean isNews) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), isNews, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, endpoint);

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this);
        mListView = view.setLazyListView(buildList(), adpWrap, listId, true);
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
}
