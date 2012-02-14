package com.soundcloud.android.activity;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.ResolveTask;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
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
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.ResolveTask;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Main extends TabActivity implements
        ResolveTask.ResolveListener {

    private View mSplash;

    public static final String TAB_TAG = "tab";
    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY   = 400;

    private ResolveTask mResolveTask;
    private FetchTrackTask mFetchTrackTask;
    private FetchUserTask mFetchUserTask;
    private ChangeLog mChangeLog;


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main);
        mChangeLog = new ChangeLog(this);

        final SoundCloudApplication app = getApp();

        if (mChangeLog.isFirstRun()) {
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

        buildTabHost(getApp(), getTabHost(), getTabWidget());
        handleIntent(getIntent());

        Object[] previousState = (Object[]) getLastNonConfigurationInstance();
        if (previousState != null) {
            mResolveTask = (ResolveTask) previousState[0];
            if (mResolveTask != null) mResolveTask.setListener(this);

            mFetchTrackTask = (FetchTrackTask) previousState[1];
            if (mFetchTrackTask != null) mFetchTrackTask.addListener(onFetchTrackListener);

            mFetchUserTask = (FetchUserTask) previousState[2];
            if (mFetchUserTask != null) mFetchUserTask.addListener(onFetchUserListener);
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
        (new FetchUserTask((SoundCloudApplication) getApplication(),-1) {
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
        if (state.containsKey(TAB_TAG)) {
            getTabHost().setCurrentTabByTag(state.getString(TAB_TAG));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(TAB_TAG, getTabHost().getCurrentTabTag());
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
        if (intent == null || (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return;
        final Tab tab = Main.Tab.fromIntent(intent);

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && handleViewUrl(intent)) {
            // already handled
        } else if (Actions.MESSAGE.equals(intent.getAction())) {
            final long recipient = intent.getLongExtra("recipient", -1);
            if (recipient != -1) {
                startActivity(new Intent(this, UserBrowser.class)
                        .putExtra("userId", recipient)
                        .putExtra("userBrowserTag", UserBrowser.Tab.privateMessage.name()));
            }
        } else if (tab != Main.Tab.UNKNOWN) {
            getTabHost().setCurrentTabByTag(tab.tag);
        } else if (Actions.PLAYER.equals(intent.getAction())) {
            // start another activity to control history
            startActivity(new Intent(this, ScPlayer.class));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            getTabHost().setCurrentTabByTag(Main.Tab.SEARCH.tag);
            if (getCurrentActivity() instanceof ScSearch) {
                ((ScSearch) getCurrentActivity()).perform(
                        Search.forSounds(intent.getStringExtra(SearchManager.QUERY)));
            }
        } else if (Actions.MY_PROFILE.equals(intent.getAction()) && intent.hasExtra("userBrowserTag")) {
            getTabHost().setCurrentTabByTag(Main.Tab.PROFILE.tag);
            if (getCurrentActivity() instanceof UserBrowser) {
                ((UserBrowser) getCurrentActivity()).setTab(intent.getStringExtra("userBrowserTag"));
            }
        } else if (Actions.USER_BROWSER.equals(intent.getAction())) {
            startActivity((new Intent(this,UserBrowser.class).putExtras(intent.getExtras())));
        } else if (Actions.ACCOUNT_PREF.equals(intent.getAction())) {
            startActivity(
                new Intent(this, AccountPreferences.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            );
        } else if (justAuthenticated(intent)) {
            Log.d(TAG, "activity start after successful authentication");
            getTabHost().setCurrentTabByTag(Main.Tab.RECORD.tag);
        }
        intent.setAction("");
        intent.setData(null);
        intent.removeExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    protected boolean handleViewUrl(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return false;
        ScModel model = ResolveTask.resolveLocally(getContentResolver(), data);
        if (model instanceof Track) {
            onFetchTrackListener.onSuccess((Track) model, null);
            return true;
        } else if (model == null) {
            Uri uri = ResolveTask.resolveSoundCloudURI(data, Env.LIVE);
            if (uri != null) {
                onUrlResolved(uri, null);
                return true;
            }
        }

        // not a client url (e.g. soundcloud:tracks:123456)
        if (!data.getPathSegments().isEmpty()) {
            // TODO put this code under test
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

    private void buildTabHost(final SoundCloudApplication app, final TabHost host, final TabWidget widget) {
        for (Tab tab : Main.Tab.values()) {
            if (tab == Main.Tab.UNKNOWN) continue;
            TabHost.TabSpec spec = host.newTabSpec(tab.tag).setIndicator(
                    getString(tab.labelId),
                    getResources().getDrawable(tab.drawableId));
            spec.setContent(tab.getIntent(this));
            host.addTab(spec);
        }
        host.setCurrentTabByTag(app.getAccountData(User.DataKeys.DASHBOARD_IDX));

        if (ImageUtils.isScreenXL(this)){
            configureTabs(this, widget, 90, -1, false);
        }
        setTabTextStyle(this, widget, false);


        if (Build.VERSION.SDK_INT > 7) {
            for (int i = 0; i < widget.getChildCount(); i++) {
                widget.getChildAt(i).setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_indicator));
            }
            widget.setLeftStripDrawable(R.drawable.tab_bottom_left);
            widget.setRightStripDrawable(R.drawable.tab_bottom_right);
        }

        // set record tab to just image
        final int recordTabIdx =  Main.Tab.RECORD.ordinal();
        View view = recordTabIdx < widget.getChildCount() ? widget.getChildAt(recordTabIdx) : null;
        if (view instanceof RelativeLayout) {
            RelativeLayout relativeLayout = (RelativeLayout) view;
            for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                if (relativeLayout.getChildAt(j) instanceof TextView) {
                    relativeLayout.getChildAt(j).setVisibility(View.GONE);
                }
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
                mFetchTrackTask,
                mFetchUserTask
        };
    }

    @Override
    public void onUrlResolved(Uri uri, String action) {
        List<String> params = uri.getPathSegments();
        if (params.size() >= 2) {
            if (params.get(0).equalsIgnoreCase("tracks")) {
                mFetchTrackTask = new FetchTrackTask(getApp(), 0);
                mFetchTrackTask.addListener(onFetchTrackListener);
                mFetchTrackTask.action = action;
                mFetchTrackTask.execute(Request.to(uri.getPath()));
            } else if (params.get(0).equalsIgnoreCase("users")) {
                mFetchUserTask = new FetchUserTask(getApp(), 0);
                mFetchUserTask.addListener(onFetchUserListener);
                mFetchUserTask.execute(Request.to(uri.getPath()));
            }
        }
    }

    @Override
    public void onUrlError() {
        Toast.makeText(this,getString(R.string.error_resolving_url),Toast.LENGTH_LONG).show();
    }


    /** this handling is overkill but avoids breaking the tests for now **/
    public FetchTrackTask.FetchTrackListener onFetchTrackListener = new FetchTrackTask.FetchTrackListener() {
        @Override
        public void onSuccess(Track track, String action) {
            onTrackLoaded(track,action);
        }

        @Override
        public void onError(long modelId) {
            onTrackError(modelId);
        }
    };

    public FetchUserTask.FetchUserListener onFetchUserListener = new FetchUserTask.FetchUserListener() {
        @Override
        public void onSuccess(User user, String action) {
            onUserLoaded(user, action);
        }

        @Override
        public void onError(long modelId) {
            onUserError(modelId);
        }
    };

    protected void onTrackLoaded(Track track, String action) {
        startService(new Intent(Main.this, CloudPlaybackService.class)
                .setAction(CloudPlaybackService.PLAY)
                .putExtra("track", track));

        startActivity(new Intent(Main.this, ScPlayer.class));
    }

    protected void onTrackError(long trackId) {
        Toast.makeText(Main.this, getString(R.string.error_loading_sound), Toast.LENGTH_LONG).show();
    }

    protected void onUserLoaded(User u, String action) {
        Intent i = new Intent(Main.this, UserBrowser.class);
            i.putExtra("user", u);
            i.putExtra("updateInfo", false);
            startActivity(i);
    }

    protected void onUserError(long userId) {
        Toast.makeText(Main.this, getString(R.string.error_loading_user), Toast.LENGTH_LONG).show();
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

    public enum Tab {
        STREAM("stream", Dashboard.class, R.string.tab_stream, R.drawable.ic_tab_incoming),
        ACTIVITY("activity",Dashboard.class, R.string.tab_activity, R.drawable.ic_tab_news),
        RECORD("record", ScCreate.class, R.string.tab_record, R.drawable.ic_tab_record),
        PROFILE("profile", UserBrowser.class, R.string.tab_you, R.drawable.ic_tab_you),
        SEARCH("search", ScSearch.class, R.string.tab_search, R.drawable.ic_tab_search),
        UNKNOWN("unknown", null, -1, -1);

        final String tag;
        final int labelId, drawableId;
        final Class<? extends android.app.Activity> activityClass;

        static final Tab DEFAULT = UNKNOWN;

        Tab(String tag, Class<? extends android.app.Activity> activityClass, int labelId, int drawableId) {
            this.tag = tag;
            this.labelId = labelId;
            this.drawableId = drawableId;
            this.activityClass = activityClass;
        }

        public static Tab fromIntent(Intent intent) {
            if (intent == null) {
                return DEFAULT;
            } else if (intent.hasExtra(TAB_TAG)) {
                return fromString(intent.getStringExtra(TAB_TAG));
            } else if (intent.getAction() != null) {
                return fromAction(intent.getAction());
            } else {
                return DEFAULT;
            }
        }

        public static Tab fromString(String s) {
            for (Tab t : values()) {
                if (t.tag.equalsIgnoreCase(s)) return t;
            }
            return UNKNOWN;
        }

        private static Tab fromAction(String action) {
            Tab tab;
            if (Actions.ACTIVITY.equals(action)) {
                tab = ACTIVITY;
            } else if (Actions.RECORD.equals(action)) {
                tab = RECORD;
            } else if (Actions.SEARCH.equals(action)) {
                tab = SEARCH;
            } else if (Actions.STREAM.equals(action)) {
                tab = STREAM;
            } else if (Actions.PROFILE.equals(action)) {
                tab = PROFILE;
            } else {
                tab = DEFAULT;
            }
            return tab;
        }

        public Intent getIntent(Context context) {
            Intent intent = new Intent(context, activityClass);
            if (Dashboard.class.equals(activityClass)) {
                intent.putExtra(TAB_TAG, tag);
            }
            return intent;
        }
    }

    private static void setTabTextStyle(Context context, TabWidget tabWidget, boolean textOnly) {
        // a hacky way of setting the font of the indicator texts
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            if (tabWidget.getChildAt(i) instanceof RelativeLayout) {
                RelativeLayout relativeLayout = (RelativeLayout) tabWidget.getChildAt(i);
                for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                    if (relativeLayout.getChildAt(j) instanceof TextView) {
                        ((TextView) relativeLayout.getChildAt(j)).setTextAppearance(context,
                                R.style.TabWidgetTextAppearance);
                        if (textOnly) {
                            relativeLayout.getChildAt(j).getLayoutParams().width = FILL_PARENT;
                            relativeLayout.getChildAt(j).getLayoutParams().height = FILL_PARENT;
                            ((TextView) relativeLayout.getChildAt(j)).setGravity(Gravity.CENTER);
                        }

                    }
                }
                if (textOnly) {
                    for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                        if (!(relativeLayout.getChildAt(j) instanceof TextView)) {
                            relativeLayout.removeViewAt(j);
                        }
                    }
                }
            }
        }
    }

    private static void configureTabs(Context context, TabWidget tabWidget, int height, int width,
                                     boolean scrolltabs) {
        // Convert the tabHeight depending on screen density
        final float scale = context.getResources().getDisplayMetrics().density;
        height = (int) (scale * height);

        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            tabWidget.getChildAt(i).getLayoutParams().height = height;
            if (width > -1)
                tabWidget.getChildAt(i).getLayoutParams().width = width;

            if (scrolltabs)
                tabWidget.getChildAt(i).setPadding(Math.round(30 * scale),
                        tabWidget.getChildAt(i).getPaddingTop(), Math.round(30 * scale),
                        tabWidget.getChildAt(i).getPaddingBottom());
        }

        tabWidget.getLayoutParams().height = height;
    }
}
