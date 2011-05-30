package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.AuthenticatorService;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TabHost;
import android.widget.TabWidget;

import java.io.IOException;

public class Main extends TabActivity {
    private View mSplash;

    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY   = 400;
    private boolean mTabsInitialized;

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
    }

    private boolean showSplash(Bundle state) {
        // don't show splash on configChanges (screen rotate)
        return state == null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccountExists(getApp());
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

    private Runnable addAccount = new Runnable() {
        @Override
        public void run() {
            dismissSplash();
            getApp().addAccount(Main.this, managerCallback);
        }
    };

    private AccountManagerCallback<Bundle> managerCallback = new AccountManagerCallback<Bundle>() {
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
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                getTabHost().setCurrentTabByTag("search");
                ((ScSearch) getCurrentActivity()).doSearch(intent.getStringExtra(SearchManager.QUERY));
            } else if (intent.hasExtra("tabIndex")) {
                getTabHost().setCurrentTab(intent.getIntExtra("tabIndex", 0));
                intent.removeExtra("tabIndex");
            } else if (intent.hasExtra("userBrowserIndex")) {
                // XXX kill me now
                getTabHost().setCurrentTab(3);
                ((UserBrowser)getCurrentActivity()).setTab(intent.getIntExtra("userBrowserIndex", 0));
            } else if (intent.hasExtra("tabTag")) {
                if (intent.getStringExtra("tabTag").contentEquals("incoming") || intent.getStringExtra("tabTag").contentEquals("exclusive")){
                    getApp().scrollTop = true;
                }
                getTabHost().setCurrentTabByTag(intent.getStringExtra("tabTag"));
                intent.removeExtra("tabTag");
            } else if (justAuthenticated(intent)) {
                Log.d(TAG, "activity start after successful authentication");
            }
        }
    }

    private void buildTabHost(final SoundCloudApplication app, final TabHost host) {
        TabHost.TabSpec spec;

        spec = host.newTabSpec("incoming").setIndicator(
                getString(R.string.tab_incoming),
                getResources().getDrawable(R.drawable.ic_tab_incoming));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "incoming"));
        host.addTab(spec);

        spec = host.newTabSpec("exclusive").setIndicator(
                getString(R.string.tab_exclusive),
                getResources().getDrawable(R.drawable.ic_tab_incoming));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "exclusive"));
        host.addTab(spec);

        spec = host.newTabSpec("record").setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));
        spec.setContent(new Intent(this, ScCreate.class));
        host.addTab(spec);

        spec = host.newTabSpec("profile").setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));
        host.addTab(spec);

        spec = host.newTabSpec("search").setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));
        spec.setContent(new Intent(this, ScSearch.class));
        host.addTab(spec);

        host.setCurrentTabByTag(app.getAccountData(User.DataKeys.DASHBOARD_IDX));
        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                app.setAccountData(User.DataKeys.DASHBOARD_IDX, tabId);
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
                    return api().exchangeOAuth1Token(params[0]);
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
                             if (app.addUserAccount(user, token)) {
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
}
