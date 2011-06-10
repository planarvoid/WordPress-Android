package com.soundcloud.android.activity.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.FriendFinderAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;

public class SuggestedUsers extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    private LazyListView mListView;
    private SectionedAdapter.Section mFriendsSection;
    private FriendFinderAdapter ffAdp;
    private SectionedEndlessAdapter ffAdpWrap;
    private Button facebookBtn;

    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);
        setContentView(R.layout.suggested_users);


        facebookBtn = (Button) findViewById(R.id.facebook_btn);
        facebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookBtn.setEnabled(false);
                configureFacebook();
            }
        });


        ffAdp = new FriendFinderAdapter(this);
        ffAdpWrap = new SectionedEndlessAdapter(this, ffAdp);
        ffAdpWrap.addListener(this);

        mListView = buildList();
        mListView.setAdapter(ffAdpWrap);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);
        // XXX make this sane - createListEmpty expects list with parent view
        ffAdpWrap.createListEmptyView(mListView);
        ffAdpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));

        if (getIntent().getBooleanExtra("facebook_connected", false)) {
            facebookBtn.setVisibility(View.GONE);
            addFriendsSection();
        }

        addSuggestedUsersSection();

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
            ((TextView) findViewById(R.id.suggested_users_msg)).setText(R.string.suggested_users_no_friends_msg);
            getSoundCloudApplication().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN, true);
        }
    }

    private void addFriendsSection() {
        mFriendsSection = new SectionedAdapter.Section(getString(R.string.list_header_fb_friends),
                Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS));
        ffAdp.sections.add(mFriendsSection);
    }

    private void addSuggestedUsersSection() {
        ffAdp.sections.add(
                new SectionedAdapter.Section(getString(R.string.list_header_suggested_users),
                        User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS)));
    }

    private void configureFacebook() {
        new NewConnectionTask(getSoundCloudApplication()) {
            @Override
            protected void onPostExecute(Uri uri) {
                if (uri != null) {
                    startActivityForResult(
                            (new Intent(SuggestedUsers.this, Connect.class))
                                    .putExtra("service", Connection.Service.Facebook.name())
                                    .setData(uri),
                            Connect.MAKE_CONNECTION);
                } else {
                    showToast(R.string.new_connection_error);
                }
            }
        }.execute(Connection.Service.Facebook);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Connect.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success) {
                        facebookBtn.setVisibility(View.GONE);
                        ffAdpWrap.clearData();
                        addFriendsSection();
                        addSuggestedUsersSection();
                        ffAdpWrap.refresh(false);
                    } else {
                        facebookBtn.setEnabled(true);
                    }
                }
        }
    }
}
