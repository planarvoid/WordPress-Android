package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.landing.ScLandingPage;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Toast;

import java.io.IOException;

public class Launch extends Activity implements FetchModelTask.FetchModelListener<ScResource> {

    private static final int DELAY_MILLIS = 500;
    private static final long MAX_DELAY_MILLIS = 1000;

    @Nullable
    private ResolveFetchTask mResolveTask;
    private Intent launchIntent;
    private boolean mLaunched;
    private FetchUserTask mFetchUserTask;
    private long mStartTIme;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launch);

        mStartTIme = System.currentTimeMillis();

        if (getApp().getAccount() == null) {
            getApp().addAccount(this, managerCallback);
            finish();
            return;
        }

        mResolveTask = (ResolveFetchTask) getLastNonConfigurationInstance();
        if (mResolveTask != null) {
            mResolveTask.setListener(this);
        }

        handleViewUrl(getIntent());
        final boolean resolving = !AndroidUtils.isTaskFinished(mResolveTask);
        findViewById(R.id.progress_resolve_layout).setVisibility(resolving ? View.VISIBLE : View.GONE);

        final SoundCloudApplication app = getApp();
        if (!resolving && IOUtils.isConnected(this) &&
                app.getAccount() != null &&
                app.getToken().valid() &&
                !app.getLoggedInUser().isPrimaryEmailConfirmed() &&
                !justAuthenticated(getIntent())) {
            checkEmailConfirmed(app);
        }

        launchIntent = ScLandingPage.LandingPage.fromString(app.getAccountData(User.DataKeys.LAST_LANDING_PAGE_IDX)).getIntent(this);
        launchIntent.putExtra(ScActivity.EXTRA_FIRST_LAUNCH, true);
    }

    private void checkCanLaunch(boolean force) {
        if (AndroidUtils.isTaskFinished(mResolveTask) && !mLaunched &&
                (force || mStartTIme - System.currentTimeMillis() >= DELAY_MILLIS)){
            mLaunched = true;
            startActivity(launchIntent);
            overridePendingTransition(R.anim.appear, R.anim.hold);
        }
    }

    protected boolean handleViewUrl(Intent intent) {
        if (!Intent.ACTION_VIEW.equals(intent.getAction()) && !FacebookSSO.handleFacebookView(this, intent))
            return false;
        Uri data = intent.getData();
        if (data == null) return false;

        mResolveTask = new ResolveFetchTask(getApp());
        mResolveTask.setListener(this);
        mResolveTask.execute(data);
        return true;
    }


    private final AccountManagerCallback<Bundle> managerCallback = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                // NB: important to call future.getResult() for side effects
                Bundle result = future.getResult();
                // restart main activity

                startActivity(new Intent(Launch.this, Launch.class)
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

    private void checkEmailConfirmed(final SoundCloudApplication app) {
        mFetchUserTask = new FetchUserTask(app) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    if (user != null) SoundCloudApplication.MODEL_MANAGER.cacheAndWrite(user, ScResource.CacheUpdateMode.FULL);
                    if (!mLaunched) checkCanLaunch(false);
                } else {
                    startActivityForResult(new Intent(Launch.this, EmailConfirm.class)
                            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                }
            }
        };
        mFetchUserTask.execute(Request.to(Endpoints.MY_DETAILS));
    }

    private boolean justAuthenticated(Intent intent) {
        return intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mResolveTask;
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCanLaunch(true);
            }
            // give some extra time for a email confirm check, but not too much
        }, mFetchUserTask != null ? MAX_DELAY_MILLIS : DELAY_MILLIS);
    }

    protected void onTrackLoaded(Track track, @Nullable String action) {
        mResolveTask = null;
        startService(track.getPlayIntent());
        launchIntent = new Intent(this, ScPlayer.class);
        checkCanLaunch(false);
    }

    protected void onUserLoaded(User u, @Nullable String action) {
        mResolveTask = null;
        launchIntent = new Intent(this, UserBrowser.class)
                .putExtra("user", u)
                .putExtra("updateInfo", false);
        checkCanLaunch(false);
    }

    @Override
    public void onError(long modelId) {
        mResolveTask = null;
        Toast.makeText(this, R.string.error_loading_url, Toast.LENGTH_LONG).show();
        checkCanLaunch(false);
    }

    @Override
    public void onSuccess(ScResource m, @Nullable String action) {
        mResolveTask = null;
        if (m instanceof Track) {
            onTrackLoaded((Track) m, null);
        } else if (m instanceof User) {
            onUserLoaded((User) m, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkCanLaunch(false);
    }
}


