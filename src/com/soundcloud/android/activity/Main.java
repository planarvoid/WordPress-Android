package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.AuthenticatorService;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.task.LoadUserInfoTask;
import com.soundcloud.android.task.ResolveTask;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class Main extends TabActivity implements LoadTrackInfoTask.LoadTrackInfoListener, LoadUserInfoTask.LoadUserInfoListener, ResolveTask.ResolveListener {
    private View mSplash;

    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY   = 400;

    private ResolveTask mResolveTask;
    private LoadTrackInfoTask mLoadTrackTask;
    private LoadUserInfoTask mLoadUserTask;
    private ChangeLog mChangeLog;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        mChangeLog = new ChangeLog(this);

        final SoundCloudApplication app = getApp();

        if (mChangeLog.isFirstRun()){
            app.onFirstRun(mChangeLog.getOldVersionCode(), mChangeLog.getCurrentVersionCode());
        }

        final boolean showSplash = showSplash(state);
        mSplash = findViewById(R.id.splash);
        mSplash.setVisibility(showSplash ? View.VISIBLE : View.GONE);
        if (isConnected() &&
                app.getAccount() != null &&
                app.getToken().valid() &&
                !app.isEmailConfirmed() &&
                !justAuthenticated(getIntent())) {
            checkEmailConfirmed(app);
        } else if (showSplash) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissSplash();
                }
            }, SPLASH_DELAY);
        }

        buildTabHost(getApp(), getTabHost());
        handleIntent(getIntent());

        Object[] previousState = (Object[]) getLastNonConfigurationInstance();
        if (previousState != null) {
            mResolveTask = (ResolveTask) previousState[0];
            if (mResolveTask != null) mResolveTask.setListener(this);

            mLoadTrackTask = (LoadTrackInfoTask) previousState[1];
            if (mLoadTrackTask != null) mLoadTrackTask.addListener(this);

            mLoadUserTask = (LoadUserInfoTask) previousState[2];
            if (mLoadUserTask != null) mLoadUserTask.setListener(this);
        }
    }

    private boolean showSplash(Bundle state) {
        // don't show splash on configChanges (screen rotate)
        return state == null;
    }

    @Override
    protected void onResume() {
        if (SoundCloudApplication.BETA_MODE && mChangeLog.isFirstRun()) {
            SoundCloudApplication.handleSilentException("Install",
                    new InstallNotification(CloudUtils.getAppVersionCode(Main.this, -1), CloudUtils.getAppVersion(Main.this, "")));
        }

        if (getApp().getAccount() == null) {
            dismissSplash();
            getApp().addAccount(Main.this, managerCallback);
            finish();
        }
        super.onResume();
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private void checkEmailConfirmed(final SoundCloudApplication app) {
        (new LoadTask<User>((SoundCloudApplication) getApplication(), User.class) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null) {
                    dismissSplash();
                } else if (user.primary_email_confirmed) {
                    app.confirmEmail();
                    dismissSplash();
                } else {
                    startActivityForResult(new Intent(Main.this, EmailConfirm.class)
                            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                }
            }
        }).execute(Request.to(Endpoints.MY_DETAILS));
    }

    private final AccountManagerCallback<Bundle> managerCallback = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                // NB: important to call future.getResult() for side effects
                Bundle result = future.getResult();
                // restart main activity

                startActivity(new Intent(Main.this, Main.class)
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

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (state.containsKey("tabTag")) {
            getTabHost().setCurrentTabByTag(state.getString("tabTag"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("tabTag", getTabHost().getCurrentTabTag());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        dismissSplash();
        if (resultCode == RESULT_OK) {
            if (data != null && EmailConfirm.RESEND.equals(data.getAction())) {
                // user pressed resend button
            }
        } else {
            // back button or no thanks
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            final String tab = Dashboard.Tabs.fromAction(intent.getAction(), null);

            if (Intent.ACTION_VIEW.equals(intent.getAction()) && handleViewUrl(intent)) {
                // already handled
            } else if (Actions.MESSAGE.equals(intent.getAction())) {
                final long recipient = intent.getLongExtra("recipient", -1);
                if (recipient != -1) {
                    startActivity(new Intent(this, UserBrowser.class)
                        .putExtra("userId",recipient)
                        .putExtra("userBrowserTag", UserBrowser.TabTags.privateMessage));
                }
            } else if (tab != null) {
                getTabHost().setCurrentTabByTag(tab);
                if (getCurrentActivity() instanceof Dashboard) {
                     ((Dashboard) getCurrentActivity()).refreshIncoming();
                }
            } else if (Actions.PLAYER.equals(intent.getAction())) {
                // start another activity to control history (back from player moves back to main)
                startActivity(
                    new Intent(this, ScPlayer.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                );
            } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                getTabHost().setCurrentTabByTag(Dashboard.Tabs.SEARCH);
                if (getCurrentActivity() instanceof ScSearch) {
                    ((ScSearch) getCurrentActivity()).doSearch(intent.getStringExtra(SearchManager.QUERY));
                }
            } else if (Actions.USER_BROWSER.equals(intent.getAction()) && intent.hasExtra("userBrowserTag")) {
                getTabHost().setCurrentTabByTag(Dashboard.Tabs.PROFILE);
                if (getCurrentActivity() instanceof UserBrowser) {
                    ((UserBrowser) getCurrentActivity()).setTab(intent.getStringExtra("userBrowserTag"));
                }
            } else if (Actions.ACCOUNT_PREF.equals(intent.getAction())) {
                startActivity(
                    new Intent(this, AccountPreferences.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                );
            } else if (justAuthenticated(intent)) {
                Log.d(TAG, "activity start after successful authentication");
                getTabHost().setCurrentTabByTag(Dashboard.Tabs.RECORD);
            }
            intent.setAction("");
            intent.setData(null);
            intent.removeExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        }
    }


    private boolean handleViewUrl(Intent intent) {
        Uri data = intent.getData();
        if (data != null && !data.getPathSegments().isEmpty()) {
            // only handle the first 2 path segments (resource only for now, actions to be implemented later)
            int cutoff = 0;
            if (data.getPathSegments().size() > 1 && (data.getPathSegments().get(1).contentEquals("follow")
                    || data.getPathSegments().get(1).contentEquals("favorite"))){
                cutoff = 1;
            } else if (data.getPathSegments().size() > 2){
                cutoff = 2;
            }
            if (cutoff > 0) {
                data = data.buildUpon().path(TextUtils.join("/", data.getPathSegments().subList(0, cutoff))).build();
            }

            mResolveTask = new ResolveTask(getApp()) ;
            mResolveTask.setListener(this);
            mResolveTask.execute(data);
            return true;

        } else {
            return false;
        }
    }

    private void buildTabHost(final SoundCloudApplication app, final TabHost host) {
        TabHost.TabSpec spec;

        spec = host.newTabSpec(Dashboard.Tabs.STREAM).setIndicator(
                getString(R.string.tab_stream),
                getResources().getDrawable(R.drawable.ic_tab_incoming));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", Dashboard.Tabs.STREAM));
        host.addTab(spec);

        spec = host.newTabSpec(Dashboard.Tabs.ACTIVITY).setIndicator(
                getString(R.string.tab_activity),
                getResources().getDrawable(R.drawable.ic_tab_news));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", Dashboard.Tabs.ACTIVITY));
        host.addTab(spec);

        spec = host.newTabSpec(Dashboard.Tabs.RECORD).setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));
        spec.setContent(new Intent(this, ScCreate.class));
        host.addTab(spec);

        spec = host.newTabSpec(Dashboard.Tabs.PROFILE).setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));
        host.addTab(spec);

        spec = host.newTabSpec(Dashboard.Tabs.SEARCH).setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));
        spec.setContent(new Intent(this, ScSearch.class));
        host.addTab(spec);

        host.setCurrentTabByTag(app.getAccountData(User.DataKeys.DASHBOARD_IDX));
        if (CloudUtils.isScreenXL(this)){
            CloudUtils.configureTabs(this, (TabWidget) findViewById(android.R.id.tabs),90, -1, false);
        }
        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        TabWidget tw = ((TabWidget) findViewById(android.R.id.tabs));

        if (Build.VERSION.SDK_INT > 7) {
            for (int i = 0; i < tw.getChildCount(); i++) {
                tw.getChildAt(i).setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_indicator));
            }
            tw.setLeftStripDrawable(R.drawable.tab_bottom_left);
            tw.setRightStripDrawable(R.drawable.tab_bottom_right);
        }

        // set record tab to just image, if tab order is changed, change the index of the following line
        RelativeLayout relativeLayout = (RelativeLayout) tw.getChildAt(2);
        for (int j = 0; j < relativeLayout.getChildCount(); j++) {
            if (relativeLayout.getChildAt(j) instanceof TextView) {
                relativeLayout.getChildAt(j).setVisibility(View.GONE);
            }
        }

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(final String tabId) {
                new Thread() {
                    @Override
                    public void run() {
                        app.setAccountData(User.DataKeys.DASHBOARD_IDX, tabId);
                    }
                }.start();
            }
        });
    }

    private void dismissSplash() {
        if (mSplash.getVisibility() == View.VISIBLE) {
            mSplash.startAnimation(new AlphaAnimation(1, 0) {
                {
                    setDuration(FADE_DELAY);
                    setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mSplash.setVisibility(View.GONE);

                            if (mChangeLog.isFirstRun()) {
                                mChangeLog.getDialog(true).show();
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                }
            });
        }
    }

    private boolean justAuthenticated(Intent intent) {
        return intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    private boolean isConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info =  manager.getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }

   @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                mResolveTask,
                mLoadTrackTask,
                mLoadUserTask
        };
    }

    @Override
    public void onUrlResolved(Uri uri, String action) {
        List<String> params = uri.getPathSegments();
        if (params.size() >= 2) {
            if (params.get(0).equalsIgnoreCase("tracks")) {
                mLoadTrackTask = new LoadTrackInfoTask(getApp(), 0, true, true);
                mLoadTrackTask.addListener(this);
                mLoadTrackTask.action = action;
                mLoadTrackTask.execute(Request.to(uri.getPath()));
            } else if (params.get(0).equalsIgnoreCase("users")) {
                mLoadUserTask = new LoadUserInfoTask(getApp(), 0, true, true);
                mLoadUserTask.setListener(this);
                mLoadUserTask.execute(Request.to(uri.getPath()));
            }
        }
    }

    @Override
    public void onUrlError() {
        Toast.makeText(this,getString(R.string.error_resolving_url),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrackInfoLoaded(Track track, String action) {
        startService(new Intent(this, CloudPlaybackService.class)
                .setAction(CloudPlaybackService.PLAY)
                .putExtra("track", track));

        startActivity(new Intent(this, ScPlayer.class));
    }

    @Override
    public void onTrackInfoError(long trackId) {
        Toast.makeText(this,getString(R.string.error_loading_sound),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUserInfoLoaded(User user) {
        Intent i = new Intent(this, UserBrowser.class);
        i.putExtra("user", user);
        i.putExtra("updateInfo", false);
        startActivity(i);
    }

    @Override
    public void onUserInfoError(long trackId) {
        Toast.makeText(this,getString(R.string.error_loading_user),Toast.LENGTH_LONG).show();
    }

    static class InstallNotification extends Exception {
        int versionCode;
        String versionName;
        InstallNotification(int versionCode, String versionName) {
            super();
            this.versionCode = versionCode;
            this.versionName = versionName;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("versionCode: ").append(versionCode).append(" ").append("versionName: ").append(versionName);
            return sb.toString();
        }
    }
}
