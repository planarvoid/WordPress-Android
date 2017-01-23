package com.soundcloud.android.profile;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class FollowersActivity extends PlayerActivity {
    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_SEARCH_QUERY_SOURCE_INFO = "searchQuerySourceInfo";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject AccountOperations accountOperations;

    public FollowersActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Urn userUrn = getIntent().getParcelableExtra(EXTRA_USER_URN);
        final SearchQuerySourceInfo searchQuerySourceInfo = getIntent().getParcelableExtra(EXTRA_SEARCH_QUERY_SOURCE_INFO);

        if (savedInstanceState == null) {
            final Fragment fragment = isLoggedInUser() ?
                                      UserFollowersFragment.createForCurrentUser(userUrn, getScreen(), searchQuerySourceInfo) :
                                      UserFollowersFragment.create(userUrn, getScreen(), searchQuerySourceInfo);

            getSupportFragmentManager().beginTransaction().add(getContentHolderViewId(), fragment).commit();
        }
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithMargins(this);
    }

    @Override
    public Screen getScreen() {
        return isLoggedInUser() ? Screen.YOUR_FOLLOWERS : Screen.USER_FOLLOWERS;
    }

    private boolean isLoggedInUser() {
        return accountOperations.isLoggedInUser(getIntent().getParcelableExtra(EXTRA_USER_URN));
    }
}
