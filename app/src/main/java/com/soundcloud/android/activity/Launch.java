package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.settings.AccountSettings;
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
import android.widget.Toast;

import java.io.IOException;

public class Launch extends Activity implements FetchModelTask.FetchModelListener<ScResource> {

    @Nullable
    private ResolveFetchTask mResolveTask;
    private FetchUserTask mFetchUserTask;
    private long mStartTime;
    private static final long SPLASH_DELAY = 800;

    private Intent launchIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launch);

        mStartTime = System.currentTimeMillis();

        mResolveTask = (ResolveFetchTask) getLastNonConfigurationInstance();
        if (mResolveTask != null) {
            mResolveTask.setListener(this);
        }

        final boolean resolving = !AndroidUtils.isTaskFinished(mResolveTask);

        findViewById(R.id.progress_resolve_layout).setVisibility(resolving ? View.VISIBLE : View.GONE);

        final SoundCloudApplication app = getApp();
        if (IOUtils.isConnected(this) &&
                app.getAccount() != null &&
                app.getToken().valid() &&
                !app.getLoggedInUser().isPrimaryEmailConfirmed() &&
                !justAuthenticated(getIntent())) {
            checkEmailConfirmed(app);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkCanLaunch();
                }
            }, SPLASH_DELAY);
        }

        handleViewUrl(getIntent());
        launchIntent = new Intent(Launch.this, Stream.class);

    }

    private void checkCanLaunch() {
        if (AndroidUtils.isTaskFinished(mResolveTask)
                && AndroidUtils.isTaskFinished(mFetchUserTask)
                && System.currentTimeMillis() - mStartTime > SPLASH_DELAY - 200){
            startActivity(launchIntent);
            overridePendingTransition(R.anim.appear, R.anim.hold);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getApp().getAccount() == null) {
            getApp().addAccount(this, managerCallback);
            finish();
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
        (new FetchUserTask(app) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    checkCanLaunch();
                } else {
                    startActivityForResult(new Intent(Launch.this, EmailConfirm.class)
                            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                }
            }
        }).execute(Request.to(Endpoints.MY_DETAILS));
    }

    private boolean justAuthenticated(Intent intent) {
        return intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    private boolean showSplash(Bundle state) {
        // don't show splash on configChanges (screen rotate)
        return state == null;
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mResolveTask;
    }

    protected void onTrackLoaded(Track track, @Nullable String action) {
        startService(track.getPlayIntent());
        launchIntent = new Intent(this, ScPlayer.class);
        checkCanLaunch();
    }

    protected void onUserLoaded(User u, @Nullable String action) {
        launchIntent = new Intent(this, UserBrowser.class)
                .putExtra("user", u)
                .putExtra("updateInfo", false);
        checkCanLaunch();
    }

    @Override
    public void onError(long modelId) {
        mResolveTask = null;
        Toast.makeText(this, R.string.error_loading_url, Toast.LENGTH_LONG).show();
        checkCanLaunch();
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
        checkCanLaunch();
    }
}


