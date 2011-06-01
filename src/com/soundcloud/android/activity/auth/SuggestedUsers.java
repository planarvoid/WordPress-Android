package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Endpoints;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SuggestedUsers extends ScActivity {

    private Button mFacebookBtn;
    private LazyListView mListView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        CloudUtils.checkState(this);

        setContentView(R.layout.suggested_users);

        mFacebookBtn = (Button)findViewById(R.id.facebook_btn);

        if (!getIntent().getBooleanExtra("facebook_connected", false)) {
            ((TextView) findViewById(R.id.listTitle)).setText(R.string.suggested_users);
            createList(Endpoints.SUGGESTED_USERS, R.string.empty_list);
        } else {
            ((TextView) findViewById(R.id.listTitle)).setText(R.string.friends_from_facebook);
            mFacebookBtn.setVisibility(View.GONE);
            createList(Endpoints.MY_FRIENDS, R.string.empty_list);
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

    protected void createList(String endpoint, int emptyText) {
        UserlistAdapter adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, endpoint, "collection");

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
            ((LazyEndlessAdapter) mListView.getAdapter()).refresh();
        }

    }
}
