package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SuggestedUsers extends ScActivity {
    private LazyListView mListView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.suggested_users);

        final View facebookBtn = findViewById(R.id.facebook_btn);
        final ViewGroup parent = ((ViewGroup) findViewById(R.id.listHolder));
        final TextView listTitle = ((TextView) findViewById(R.id.listTitle));

        if (getIntent().getBooleanExtra("facebook_connected", false)) {
            facebookBtn.setVisibility(View.GONE);
            listTitle.setText(R.string.friends_from_facebook);
            mListView = createList(Request.to(Endpoints.MY_FRIENDS), Friend.class, parent, R.string.empty_list);
        } else {
            listTitle.setText(R.string.suggested_users);
            mListView = createList(Request.to(Endpoints.SUGGESTED_USERS), User.class, parent, R.string.empty_list);
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

    protected LazyListView createList(Request request, Class<?> model, ViewGroup parent, int emptyText) {
        FriendFinderAdapter adp = new FriendFinderAdapter(this, new ArrayList<Parcelable>(), model);
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, request);
        LazyListView list = buildList();
        list.setAdapter(adpWrap);
        parent.addView(list);
        // XXX make this sane - createListEmpty expects list with parent view
        adpWrap.createListEmptyView(list);
        if (emptyText != -1) adpWrap.setEmptyViewText(getResources().getString(emptyText));
        return list;
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
