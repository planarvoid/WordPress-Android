package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
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

public class SuggestedUsers extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    private LazyListView mListView;
    private SectionedAdapter.Section mFriendsSection;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.suggested_users);

        final View facebookBtn = findViewById(R.id.facebook_btn);

        FriendFinderAdapter ffAdp = new FriendFinderAdapter(this);
        SectionedEndlessAdapter ffAdpWrap = new SectionedEndlessAdapter(this, ffAdp);

        ffAdpWrap.addListener(this);

        mListView = buildList();
        mListView.setAdapter(ffAdpWrap);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);
        // XXX make this sane - createListEmpty expects list with parent view
        ffAdpWrap.createListEmptyView(mListView);
        ffAdpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));

        if (getIntent().getBooleanExtra("facebook_connected", false)) {
            facebookBtn.setVisibility(View.GONE);
            ffAdp.sections.add(
                mFriendsSection = new SectionedAdapter.Section("Facebook Friends", Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS)));
        }

        ffAdp.sections.add(
                new SectionedAdapter.Section("Suggested Users", User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS)));

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

    public void onSectionLoaded(SectionedAdapter.Section section) {
        if ((mFriendsSection != null && mFriendsSection == section && mFriendsSection.data.size() == 0 &&
                !getSoundCloudApplication().getAccountDataBoolean(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN))){
            showToast(R.string.suggested_users_no_friends_msg);
            getSoundCloudApplication().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN, true);
        }
    }
}
