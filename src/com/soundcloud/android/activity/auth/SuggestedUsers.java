package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SuggestedUsers extends ScActivity {
    private LazyListView mListView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        CloudUtils.checkState(this);

        setContentView(R.layout.suggested_users);

        View facebookBtn = findViewById(R.id.facebook_btn);
        if (getIntent().getBooleanExtra("facebook_connected", false)) {
            ((TextView) findViewById(R.id.listTitle)).setText(R.string.friends_from_facebook);
            facebookBtn.setVisibility(View.GONE);
            createList(Request.to(Endpoints.MY_FRIENDS), Friend.class, R.string.empty_list);
        } else {
            ((TextView) findViewById(R.id.listTitle)).setText(R.string.suggested_users);
            createList(Request.to(Endpoints.SUGGESTED_USERS), User.class, R.string.empty_list);
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            ((LazyEndlessAdapter) mListView.getAdapter()).restoreState(mPreviousState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack("/suggested_users");
    }

    protected void createList(Request request, Class<?> model, int emptyText) {
        FriendFinderAdapter adp = new FriendFinderAdapter(this, new ArrayList<Parcelable>(), model);
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, request);

        mListView = buildList();
        mListView.setAdapter(adpWrap);
        ((FrameLayout) findViewById(R.id.listHolder)).addView(mListView);
        adpWrap.createListEmptyView(mListView);
        if (emptyText != -1) adpWrap.setEmptyViewText(getResources().getString(emptyText));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null && mListView.getAdapter() instanceof LazyEndlessAdapter){
            return ((LazyEndlessAdapter) mListView.getAdapter()).saveState();
        }
        return null;
    }

    @Override
    public void onRefresh() {
        if (mListView != null && mListView.getAdapter() instanceof LazyEndlessAdapter){
            ((LazyEndlessAdapter) mListView.getAdapter()).refresh(true);
        }
    }
}
