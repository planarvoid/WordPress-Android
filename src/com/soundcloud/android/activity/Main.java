package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.AuthenticatorService;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.CloudAPI;

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
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TabHost;
import android.widget.TabWidget;

import java.io.IOException;

public class Main extends TabActivity {
    private TabHost mTabHost;
    private ViewGroup mSplash;

    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY   = 400;
    private static final long SPLASH_PAUSE = 5 * 60 * 1000;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, "onCreate("+state+")");

        setContentView(R.layout.main);

        long lastDestroyed = state == null ? 0 : state.getLong("lastDestroyed");
        final boolean visible = lastDestroyed == 0 || System.currentTimeMillis() - lastDestroyed > SPLASH_PAUSE;
        mSplash = (ViewGroup) findViewById(R.id.splash);
        mSplash.setVisibility(visible ? View.VISIBLE : View.GONE);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        if (app.getAccount() == null) {
            String legacyAccessToken = PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(User.DataKeys.OAUTH1_ACCESS_TOKEN, null);

            if (legacyAccessToken != null) {
                attemptTokenExchange(app, legacyAccessToken, addAccount);
            } else {
                addAccount.run();
            }
            finish();
            return;
        }

        if (isConnected() && app.getToken() != null && !app.isEmailConfirmed()) {
            (new LoadTask<User>((SoundCloudApplication) getApplication(), User.class) {
                @Override
                protected void onPostExecute(User user) {
                    if (user == null ) {
                        Log.w(TAG, "could not get user information");
                        dismissSplash();
                    } else if (user.primary_email_confirmed) {
                        Log.v(TAG, "email confirmed");
                        app.confirmEmail();
                        dismissSplash();
                    } else {
                        startActivityForResult(new Intent(Main.this, EmailConfirm.class)
                                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                    }
                }
            }).execute(CloudAPI.Enddpoints.MY_DETAILS);
        } else if (visible) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissSplash();
                }
            }, SPLASH_DELAY);
        }

        mTabHost = buildTabHost();
        mTabHost.setCurrentTab(((SoundCloudApplication) this.getApplication())
                .getAccountDataInt(User.DataKeys.DASHBOARD_IDX));

        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                ((SoundCloudApplication) Main.this.getApplication())
                .setAccountData(User.DataKeys.DASHBOARD_IDX,Integer.toString(mTabHost.getCurrentTab()));
            }
        });

        handleIntent(getIntent());
    }

    private Runnable addAccount = new Runnable() {
        @Override
        public void run() {
            SoundCloudApplication app = (SoundCloudApplication) getApplication();
            app.addAccount(Main.this, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        // restart main activity
                        // NB: important to call future.getResult() for side effects
                        startActivity(new Intent(Main.this, Main.class)
                                .putExtra(AuthenticatorService.KEY_ACCOUNT_RESULT, future.getResult())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    } catch (OperationCanceledException ignored) {
                        Log.d(TAG, "operation canceled");
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    } catch (AuthenticatorException e) {
                        Log.w(TAG, e);
                    }
                }
            });
        }
    };

    private void attemptTokenExchange(final SoundCloudApplication app, String legacyAccessToken, final Runnable fallback) {
        new AsyncApiTask<String, Void, AndroidCloudAPI>(app) {
            @Override
            protected AndroidCloudAPI doInBackground(String... params) {
                try {
                    return (AndroidCloudAPI) api().exchangeToken(params[0]);
                } catch (IOException e) {
                    return null;
                }
            }
            @Override
            protected void onPostExecute(final AndroidCloudAPI api) {
                if (api != null) {
                     new LoadTask.LoadUserTask(api) {
                         @Override
                         protected void onPostExecute(User user) {
                             if (app.addUserAccount(user, api.getToken(), api.getRefreshToken())) {
                                 // remove old tokens after successful exchange
                                 PreferenceManager.getDefaultSharedPreferences(Main.this)
                                         .edit()
                                         .remove(User.DataKeys.OAUTH1_ACCESS_TOKEN)
                                         .remove(User.DataKeys.OAUTH1_ACCESS_TOKEN_SECRET)
                                         .commit();
                                 // and reload main
                                 startActivity(new Intent(Main.this, Main.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                 finish();
                             } else {
                                 fallback.run();
                             }
                         }
                     }.execute(CloudAPI.Enddpoints.MY_DETAILS);
                } else {
                    fallback.run();
                }
            }
        }.execute(legacyAccessToken);
    }

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
            mTabHost.setCurrentTabByTag(state.getString("tabTag"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("tabTag", mTabHost.getCurrentTabTag());
        state.putLong("lastDestroyed", System.currentTimeMillis());

        super.onSaveInstanceState(state);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                mTabHost.setCurrentTabByTag("search");
                ((ScSearch) getCurrentActivity()).doSearch(intent.getStringExtra(SearchManager.QUERY));
            } else if (intent.hasExtra("tabIndex")) {
                mTabHost.setCurrentTab(intent.getIntExtra("tabIndex", 0));
                intent.removeExtra("tabIndex");
            } else if (intent.hasExtra("tabTag")) {
                mTabHost.setCurrentTabByTag(intent.getStringExtra("tabTag"));
                intent.removeExtra("tabTag");
            } else if (intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT)) {
                Log.d(TAG, "activity start after successful authentication");
            }
        }
    }

    private TabHost buildTabHost() {
        TabHost host = getTabHost();
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

        spec = host.newTabSpec("profile").setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));
        host.addTab(spec);

        spec = host.newTabSpec("record").setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));
        spec.setContent(new Intent(this, ScCreate.class));
        host.addTab(spec);

        spec = host.newTabSpec("search").setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));
        spec.setContent(new Intent(this, ScSearch.class));
        host.addTab(spec);

        return host;
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

    private boolean isConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info =  manager.getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }
}
