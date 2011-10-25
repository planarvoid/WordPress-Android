package com.soundcloud.android.activity;

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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.*;
import com.soundcloud.android.*;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.AuthenticatorService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.*;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.CreateController;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Main extends TabActivity implements LoadTrackInfoTask.LoadTrackInfoListener, LoadUserInfoTask.LoadUserInfoListener, ResolveTask.ResolveListener {
    private View mSplash;

    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY   = 400;

    private ResolveTask mResolveTask;
    private LoadTrackInfoTask mLoadTrackTask;
    private LoadUserInfoTask mLoadUserTask;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        final SoundCloudApplication app = getApp();
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
            if (mLoadTrackTask != null) mLoadTrackTask.setListener(this);

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
        if (!checkAccountExists(getApp())) {
            finish();
        }
        super.onResume();
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private boolean checkAccountExists(SoundCloudApplication app) {
        if (app.getAccount() == null) {
            String oauth1Token = PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(User.DataKeys.OAUTH1_ACCESS_TOKEN, null);

            if (oauth1Token != null) {
                attemptTokenExchange(app, oauth1Token, addAccount);
            } else {
                addAccount.run();
            }
            return false;
        } else {
            return true;
        }
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

    private final Runnable addAccount = new Runnable() {
        @Override
        public void run() {
            dismissSplash();
            getApp().addAccount(Main.this, managerCallback);
        }
    };

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
            } else if (Actions.MESSAGE.equals(intent.getAction())){
                final long recipient = intent.getLongExtra("recipient", -1);
                if (recipient != -1){
                startActivity(
                    new Intent(this, UserBrowser.class).putExtra("userId",recipient)
                            .putExtra("userBrowserTag",UserBrowser.TabTags.privateMessage)
                );
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
        final Uri data = intent.getData();
        if (data != null && !data.getPathSegments().isEmpty()) {
            // only handle the first 2 path segments (resource only for now, actions to be implemented later)
            Uri u = Uri.parse("http://soundcloud.com/");
            Uri resolveUri = (data.getPathSegments().size() > 2) ?
                    (u.buildUpon().path(TextUtils.join("/", data.getPathSegments().subList(0, 2))).build()) : data;

            mResolveTask = new ResolveTask(getApp()) ;
            mResolveTask.setListener(this);
            mResolveTask.execute(resolveUri);
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
            } else if (relativeLayout.getChildAt(j) instanceof ImageView) {
                relativeLayout.getChildAt(j).getLayoutParams().height = RelativeLayout.LayoutParams.FILL_PARENT;
                ((RelativeLayout.LayoutParams) relativeLayout.getChildAt(j).getLayoutParams()).bottomMargin =
                        (int) (5*getResources().getDisplayMetrics().density);
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
            final ChangeLog cl = new ChangeLog(this);
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
                            if (cl.isFirstRun()) {
                                cl.getDialog(true).show();
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

    private void attemptTokenExchange(final SoundCloudApplication app,
                                      String oldAccessToken, final Runnable fallback) {
        new AsyncApiTask<String, Void, Token>(app) {
            @Override protected Token doInBackground(String... params) {
                try {
                    return mApi.exchangeOAuth1Token(params[0]);
                } catch (IOException e) {
                    Log.w(TAG, "error exchanging tokens", e);
                    return null;
                }
            }
            @Override
            protected void onPostExecute(final Token token) {
                if (token != null) {
                     new LoadTask.LoadUserTask(app) {
                         @Override
                         protected void onPostExecute(User user) {
                             if (user != null && app.addUserAccount(user, token)) {
                                 Log.v(TAG, "successful token exchange");
                                 SoundCloudDB.writeUser(getContentResolver(), user, WriteState.all, user.id);
                                 // remove old tokens after successful exchange
                                 PreferenceManager.getDefaultSharedPreferences(Main.this)
                                         .edit()
                                         .remove(User.DataKeys.OAUTH1_ACCESS_TOKEN)
                                         .remove(User.DataKeys.OAUTH1_ACCESS_TOKEN_SECRET)
                                         .commit();
                                 // restart main activity
                                 Intent intent = getIntent();
                                 overridePendingTransition(0, 0);
                                 intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                 finish();
                                 overridePendingTransition(0, 0);

                                 startActivity(intent);
                             } else {
                                 fallback.run();
                             }
                         }
                     }.execute(Request.to(Endpoints.MY_DETAILS));
                } else {
                    fallback.run();
                }
            }
        }.execute(oldAccessToken);
    }

   @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                super.onRetainNonConfigurationInstance(),
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
                mLoadTrackTask.setListener(this);
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
                .setAction(CloudPlaybackService.ONE_SHOT_PLAY)
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
}
