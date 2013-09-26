package com.soundcloud.android.activity.landing;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.showcases.Showcase;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import net.hockeyapp.android.UpdateManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class Home extends ScActivity implements ScLandingPage {
    public static final String EXTRA_ONBOARDING_USERS_RESULT  = "onboarding_users_result";
    private FetchUserTask mFetchUserTask;
    private AccountOperations mAccountOperations;

    private AndroidCloudAPI oldCloudAPI;
    private ApplicationProperties mApplicationProperties;
    private ShowcaseView mCurrentMenuShowcase;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        oldCloudAPI = new OldCloudAPI(this);
        mAccountOperations = new AccountOperations(this);
        mApplicationProperties = new ApplicationProperties(getResources());
        setTitle(getString(R.string.side_menu_stream));
        final SoundCloudApplication app = getApp();
        if (mAccountOperations.soundCloudAccountExists()) {
            if (state == null) {
                final Uri build = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();

                getSupportFragmentManager().beginTransaction()
                        .add(mRootView.getContentHolderId(), ScListFragment.newInstance(build))
                        .commit();
            }

            if (IOUtils.isConnected(this) &&
                    mAccountOperations.soundCloudAccountExists() &&
                    mAccountOperations.getSoundCloudToken().valid() &&
                    !app.getLoggedInUser().isPrimaryEmailConfirmed() &&
                    !justAuthenticated(getIntent())) {
                checkEmailConfirmed();
            }

            if (mApplicationProperties.isBetaBuildRunningOnDalvik()) {
                Log.d(TAG, "checking for beta updates");
                UpdateManager.register(this, getString(R.string.hockey_app_id));
            }
        }
    }

    private boolean startedFromOnboardingError() {
        return !getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mAccountOperations.soundCloudAccountExists()) {
            mAccountOperations.addSoundCloudAccountManually(this);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_stream;
    }

    private void checkEmailConfirmed() {
        mFetchUserTask = new FetchUserTask(oldCloudAPI) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    if (user != null) {
                        new UserStorage().createOrUpdate(user);
                    }
                } else {
                    startActivityForResult(new Intent(Home.this, EmailConfirm.class)
                            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                }
                mFetchUserTask = null;
            }
        };
        mFetchUserTask.execute(Request.to(Endpoints.MY_DETAILS));
    }

    private boolean justAuthenticated(Intent intent) {
        return intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    @Override
    public void onMenuOpenLeft() {
        super.onMenuOpenLeft();

        final View viewById = mRootView.getMenuItemViewById(R.id.nav_discover);
        if (viewById != null){
            mCurrentMenuShowcase = Showcase.EXPLORE.insertShowcase(this, viewById);
        }
    }

    @Override
    public void onMenuClosed() {
        super.onMenuClosed();
        if (mCurrentMenuShowcase != null && mCurrentMenuShowcase.isShown()){
            mCurrentMenuShowcase.hide();
        }
    }
}
