package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadTask;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

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

public class Main extends TabActivity {
    private TabHost mTabHost;
    private ViewGroup mSplash;

    private static final long SPLASH_DELAY = 1000;
    private static final long FADE_DELAY   = 1000;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main);

        mSplash = (ViewGroup) findViewById(R.id.splash);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        if (isConnected() && app.getState() == SoundCloudAPI.State.AUTHORIZED && !app.isEmailConfirmed()) {
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
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissSplash();
                }
            }, SPLASH_DELAY);
        }

        mTabHost = buildTabHost();
        mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(SoundCloudApplication.Prefs.DASHBOARD_IDX, 0));

        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                PreferenceManager.getDefaultSharedPreferences(Main.this).edit()
                        .putInt(SoundCloudApplication.Prefs.DASHBOARD_IDX, mTabHost.getCurrentTab())
                        .commit();
            }
        });

        handleIntent(getIntent());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        dismissSplash();

        switch (resultCode) {
            case EmailConfirm.RESEND:    break;
            case EmailConfirm.NO_THANKS: break;
            case EmailConfirm.IGNORED:   break;
            default:
        }
    }

    private boolean isConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info =  manager.getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }
}
