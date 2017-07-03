package com.soundcloud.android.profile;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class FollowingsActivity extends PlayerActivity implements FollowingsPresenter.FollowingsView {
    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_SEARCH_QUERY_SOURCE_INFO = "searchQuerySourceInfo";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject AccountOperations accountOperations;
    @Inject FollowingsPresenter followingsPresenter;

    public FollowingsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            followingsPresenter.visitFollowingsScreen(this);
        }

        followingsPresenter.attachView(this);
    }

    @Override
    protected void onDestroy() {
        followingsPresenter.detachView();
        super.onDestroy();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithMargins(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    public Urn getUserUrn() {
        return Urns.urnFromIntent(getIntent(), EXTRA_USER_URN);
    }

    @Override
    public void visitFollowingsScreenForCurrentUser(Screen trackingScreen) {
        createFragment(MyFollowingsFragment.create(trackingScreen, getSearchQuerySourceInfo()));
    }

    @Override
    public void visitFollowingsScreenForOtherUser(Screen trackingScreen) {
        createFragment(UserFollowingsFragment.create(getUserUrn(), trackingScreen, getSearchQuerySourceInfo()));
    }

    private void createFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().add(getContentHolderViewId(), fragment).commit();
    }

    private SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return getIntent().getParcelableExtra(EXTRA_SEARCH_QUERY_SOURCE_INFO);
    }
}
