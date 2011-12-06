package com.soundcloud.android.activity.auth;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Connect;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;

public class SuggestedUsers extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    private ScListView mListView;
    private SectionedAdapter.Section mFriendsSection;
    private SectionedUserlistAdapter ffAdp;
    private SectionedEndlessAdapter ffAdpWrap;
    private Button facebookBtn;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.suggested_users);

        ffAdp = new SectionedUserlistAdapter(this);
        ffAdpWrap = new SectionedEndlessAdapter(this, ffAdp, true);
        ffAdpWrap.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.suggested_users_header, mListView, false);
        mListView.addHeaderView(header, null, false);

        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        ffAdpWrap.configureViews(mListView);
        ffAdpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));
        mListView.setAdapter(ffAdpWrap,true);

        facebookBtn = (Button) header.findViewById(R.id.facebook_btn);
        facebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                configureFacebook();
            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        if (getIntent() != null && getIntent().getBooleanExtra(Start.FB_CONNECTED_EXTRA, false)) {
            facebookBtn.setVisibility(View.GONE);
            addFriendsSection();
        }

        addSuggestedUsersSection();

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }

        // result ok no matter what
        setResult(RESULT_OK);

        startActivity(new Intent(this, com.soundcloud.android.view.tour.Start.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        trackPage(Consts.Tracking.PEOPLE_FINDER);
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null) {
            return  mListView.getWrapper().saveState();
        }
        return null;
    }

    public void onSectionLoaded(SectionedAdapter.Section section) {
        if ((mFriendsSection != null &&
             mFriendsSection == section && mFriendsSection.data.size() == 0 &&
            !getApp().getAccountDataBoolean(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN))) {

            ((TextView) findViewById(R.id.suggested_users_msg_txt)).setText(R.string.suggested_users_no_friends_msg);
            getApp().setAccountData(User.DataKeys.FRIEND_FINDER_NO_FRIENDS_SHOWN, true);

        }
    }

    private void addFriendsSection() {
        mFriendsSection = new SectionedAdapter.Section(getString(R.string.list_header_fb_friends),
                Friend.class, new ArrayList<Parcelable>(), Request.to(Endpoints.MY_FRIENDS), ScContentProvider.Content.ME_FRIENDS);
        ffAdp.sections.add(mFriendsSection);
    }

    private void addSuggestedUsersSection() {
        ffAdp.sections.add(
                new SectionedAdapter.Section(getString(R.string.list_header_suggested_users),
                        User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.SUGGESTED_USERS), ScContentProvider.Content.SUGGESTED_USERS));
    }

    /* package */ void configureFacebook() {
        facebookBtn.setEnabled(false);
        final Drawable background = facebookBtn.getBackground();
        if (background != null) background.setAlpha(150);

        new NewConnectionTask(getApp()) {
            @Override
            protected void onPostExecute(Uri uri) {
                facebookBtn.setEnabled(true);
                if (background != null) background.setAlpha(255);
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
                        ffAdpWrap.clearSections();
                        addFriendsSection();
                        addSuggestedUsersSection();
                        ffAdpWrap.refresh(false);
                    }
                }
        }
    }
}
