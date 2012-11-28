package com.soundcloud.android.activity.landing;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import net.hockeyapp.android.UpdateManager;

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class Home extends ScActivity implements ScLandingPage {
    private FetchUserTask mFetchUserTask;
    private ChangeLog mChangeLog;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_stream));
        mChangeLog = new ChangeLog(this);

        final SoundCloudApplication app = getApp();
        if (app.getAccount() != null) {
            if (state == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.ME_SOUND_STREAM)).commit();
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
            getApp().addAccount(this, managerCallback);
            finish();
        } else if (mChangeLog.isFirstRun()) {
            mChangeLog.getDialog(true).show();
        }
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
                        if (user != null) SoundCloudApplication.MODEL_MANAGER.cacheAndWrite(user, ScResource.CacheUpdateMode.FULL);
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


    private final AccountManagerCallback<Bundle> managerCallback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    // NB: important to call future.getResult() for side effects
                    Bundle result = future.getResult();
                    // restart main activity

                    startActivity(new Intent(Home.this, Home.class)
                            .putExtra(AuthenticatorService.KEY_ACCOUNT_RESULT, result)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                } catch (OperationCanceledException ignored) {
                    finish();
                } catch (IOException e) {
                    Log.w(TAG, e);
                } catch (AuthenticatorException e) {
                    Log.w(TAG, e);
                }
            }
        };


}
