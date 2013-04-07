package com.soundcloud.android.activity.landing;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.fragment.ActivitiesFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import net.hockeyapp.android.UpdateManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class Home extends ScActivity implements ScLandingPage {
    private FetchUserTask mFetchUserTask;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_stream));
        final SoundCloudApplication app = getApp();
        if (app.getAccount() != null) {
            if (state == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(mRootView.getContentHolderId(), ActivitiesFragment.create(Content.ME_SOUND_STREAM))
                        .commit();

                if (SoundCloudApplication.BETA_MODE){
                    ChangeLog changeLog = new ChangeLog(this);
                    if (changeLog.isFirstRun()) {
                        changeLog.getDialog(true).show();
                    }
                }
            }

            if (IOUtils.isConnected(this) &&
                    app.getAccount() != null &&
                    app.getToken().valid() &&
                    !app.getLoggedInUser().isPrimaryEmailConfirmed() &&
                    !justAuthenticated(getIntent())) {
                checkEmailConfirmed(app);
            }

            if (SoundCloudApplication.BETA_MODE) {
                Log.d(TAG, "checking for beta updates");
                UpdateManager.register(this, getString(R.string.hockey_app_id));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getApp().getAccount() == null) {
            getApp().addAccount(this);
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

    private void checkEmailConfirmed(final SoundCloudApplication app) {
        mFetchUserTask = new FetchUserTask(app) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    if (user != null) {
                        new UserStorage(Home.this).createOrUpdate(user);
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
}
