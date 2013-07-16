package com.soundcloud.android.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.landing.FriendFinder;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.ScLandingPage;
import com.soundcloud.android.activity.landing.SuggestedUsersActivity;
import com.soundcloud.android.activity.landing.WhoToFollowActivity;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.imageloader.OldImageLoader;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.AddCommentDialog;
import com.soundcloud.android.view.MainMenu;
import com.soundcloud.android.view.RootView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends SherlockFragmentActivity implements Tracker, RootView.OnMenuStateListener, OldImageLoader.LoadBlocker, ActionBarController.ActionBarOwner {
    protected static final int CONNECTIVITY_MSG = 0;
    protected NetworkConnectivityListener connectivityListener;
    private long mCurrentUserId;

    protected RootView mRootView;
    private Boolean mIsConnected;
    private boolean mIsForeground;

    private AccountOperations mAccountOperations;
    private AndroidCloudAPI mAndroidCloudAPI;

    @Nullable
    protected ActionBarController mActionBarController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountOperations = new AccountOperations(this);
        mAndroidCloudAPI = new OldCloudAPI(this);
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);

        // Volume mode should always be music in this app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mRootView = new RootView(this, getWindow().getDecorView().getBackground(), getSelectedMenuId());
        super.setContentView(mRootView);
        getWindow().setBackgroundDrawable(null);

        mRootView.setOnMenuStateListener(this);
        mRootView.configureMenu(R.menu.main_nav, new MainMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClicked(int id) {
                final Bundle menuBundle = mRootView.getMenuBundle();
                switch (id) {
                    case R.id.nav_stream:
                        startNavActivity(ScActivity.this, Home.class, menuBundle);
                        return true;
                    case R.id.nav_news:
                        startNavActivity(ScActivity.this, News.class, menuBundle);
                        return true;
                    case R.id.nav_you:
                        startNavActivity(ScActivity.this, You.class, menuBundle);
                        return true;
                    case R.id.nav_record:
                        startNavActivity(ScActivity.this, ScCreate.class, menuBundle);
                        return true;
                    case R.id.nav_likes:
                        startActivity(getNavIntent(ScActivity.this, You.class, menuBundle)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.likes.tag));
                        return true;
                    case R.id.nav_sets:
                        startActivity(getNavIntent(ScActivity.this, You.class, menuBundle)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.sets.tag));
                        return true;
                    case R.id.nav_friend_finder:
                        startNavActivity(ScActivity.this, FriendFinder.class, menuBundle);
                        return true;
                    case R.id.nav_suggested_users:
                        final Class<? extends Activity> destination = SoundCloudApplication.DEV_MODE ?
                                SuggestedUsersActivity.class : WhoToFollowActivity.class;
                        startNavActivity(ScActivity.this, destination, menuBundle);
                        return true;
                    case R.id.nav_settings:
                        startActivity(new Intent(ScActivity.this, Settings.class));
                        mRootView.setCloseOnResume(true);
                        return false;
                }
                return false;
            }
        });

        if (getSupportActionBar() != null) {
            mActionBarController = createActionBarController(mRootView);
        }

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Consts.GeneralIntents.ACTIVITIES_UNSEEN_CHANGED);
        f.addAction(Consts.GeneralIntents.UNAUTHORIZED);
        f.addAction(Actions.LOGGING_OUT);
        registerReceiver(mGeneralIntentListener, new IntentFilter(f));
    }

    protected ActionBarController createActionBarController(RootView rootView) {
        return new NowPlayingActionBarController(this, rootView, mAndroidCloudAPI);
    }

    protected abstract int getSelectedMenuId();

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mActionBarController != null) {
            mActionBarController.onSaveInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mActionBarController != null) {
            mActionBarController.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void setContentView(int id) {
        setContentView(View.inflate(this, id, new FrameLayout(this)));
    }

    @Override
    public void setContentView(View layout) {
        mRootView.setContent(layout);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (mActionBarController != null) mActionBarController.setTitle(title);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.hasExtra(RootView.EXTRA_ROOT_VIEW_STATE)) {
            overridePendingTransition(0, 0);
            mRootView.restoreStateFromExtra(intent.getExtras().getBundle(RootView.EXTRA_ROOT_VIEW_STATE));
        }
    }

    static void startNavActivity(Context c, Class activity, Bundle rootViewState) {
        c.startActivity(getNavIntent(c, activity, rootViewState));
    }

    static Intent getNavIntent(Context c, Class activity, Bundle rootViewState) {
        return new Intent(c, activity)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra(RootView.EXTRA_ROOT_VIEW_STATE, rootViewState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(mGeneralIntentListener);
        } catch (IllegalArgumentException e){
            // this seems to happen in EmailConfirm. Seems like it doesn't respect the full lifecycle.
            Log.e(SoundCloudApplication.TAG,"Exception unregistering general intent listener: ", e);
        }

        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        if (mActionBarController != null) {
            mActionBarController.onDestroy();
        }
        mRootView.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectivityListener.startListening(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectivityListener.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mAccountOperations.soundCloudAccountExists()) {
            pausePlayback();
            finish();
            return;
        }

        mIsForeground = true;
        mRootView.onResume();
        if (mActionBarController != null) {
            mActionBarController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
        if (mActionBarController != null) {
            mActionBarController.onPause();
        }
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    public void pausePlayback() {
        startService(new Intent(CloudPlaybackService.PAUSE_ACTION));
    }

    public SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    public boolean isForeground() {
        return mIsForeground;
    }

    public boolean isConnected() {
        if (mIsConnected == null) {
            if (connectivityListener == null) {
                mIsConnected = true;
            } else {
                // mIsConnected not set yet
                NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                mIsConnected = networkInfo == null || networkInfo.isConnectedOrConnecting();
            }
        }
        return mIsConnected;
    }

    public void showToast(int stringId) {
        AndroidUtils.showToast(this, stringId);
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            try {
                showDialog(dialogId);
            } catch (WindowManager.BadTokenException ignored) {
                // the !isFinishing() check should prevent these - but not always
            }
        }
    }

    protected void onDataConnectionChanged(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected) {
            // clear image loading errors
            OldImageLoader.get(this).clearErrors();
        }
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNAUTHORIZED:
                return new AlertDialog.Builder(this).setTitle(R.string.error_unauthorized_title)
                        .setMessage(R.string.error_unauthorized_message).setNegativeButton(
                                R.string.side_menu_settings, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(ScActivity.this, Settings.class));
                            }
                        }).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_ERROR_LOADING:
                return new AlertDialog.Builder(this).setTitle(R.string.error_loading_title)
                        .setMessage(R.string.error_loading_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_LOGOUT:
                return Settings.createLogoutDialog(this);

            case Consts.Dialogs.DIALOG_ADD_COMMENT:
                final AddCommentDialog dialog = new AddCommentDialog(this);
                dialog.getWindow().setGravity(Gravity.TOP);
                return dialog;

            case Consts.Dialogs.DIALOG_TRANSCODING_FAILED:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_transcoding_failed_title)
                        .setMessage(R.string.dialog_transcoding_failed_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(Consts.Dialogs.DIALOG_TRANSCODING_FAILED);
                            }
                        }).setNegativeButton(
                                R.string.visit_support, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(
                                        new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(getString(R.string.authentication_support_uri))));
                                removeDialog(Consts.Dialogs.DIALOG_TRANSCODING_FAILED);
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_TRANSCODING_PROCESSING:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_transcoding_processing_title)
                        .setMessage(R.string.dialog_transcoding_processing_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(Consts.Dialogs.DIALOG_TRANSCODING_PROCESSING);
                            }
                        }).create();

            default:
                return super.onCreateDialog(which);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mActionBarController != null) {
            mActionBarController.onCreateOptionsMenu(menu);
        }
        return true;
    }

    public int getMenuResourceId() {
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (mActionBarController != null && !mActionBarController.onOptionsItemSelected(item))
                || super.onOptionsItemSelected(item);
    }

    public long getCurrentUserId() {
        if (mCurrentUserId == 0) {
            mCurrentUserId = SoundCloudApplication.getUserId();
        }
        return mCurrentUserId;
    }

    private static final class ConnectivityHandler extends Handler {
        private WeakReference<ScActivity> mContextRef;

        private ConnectivityHandler(ScActivity context) {
            this.mContextRef = new WeakReference<ScActivity>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            final ScActivity context = mContextRef.get();
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (context != null && msg.obj instanceof NetworkInfo) {
                        NetworkInfo networkInfo = (NetworkInfo) msg.obj;
                        final boolean connected = networkInfo.isConnectedOrConnecting();
                        if (connected) {
                            OldImageLoader.get(context.getApplicationContext()).clearErrors();

                            // announce potential proxy change
                            context.sendBroadcast(new Intent(Actions.CHANGE_PROXY_ACTION)
                                    .putExtra(Actions.EXTRA_PROXY, IOUtils.getProxy(context, networkInfo)));
                        }
                        context.onDataConnectionChanged(connected);
                    }
                    break;
            }
        }
    }

    private final Handler connHandler = new ConnectivityHandler(this);

    // tracking shizzle
    public void track(Event event, Object... args) {
        getApp().track(event, args);
    }

    public void track(Class<?> klazz, Object... args) {
        getApp().track(klazz, args);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // handle back button to go back to previous screen
        if (keyCode == KeyEvent.KEYCODE_BACK
                && (mRootView.isExpanded() || mRootView.isMoving())) {
            mRootView.onBack();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


    @Override
    public void onMenuOpenLeft() {
        if (mActionBarController != null) {
            mActionBarController.hideMenuIndicator();
        }
    }

    @Override
    public void onMenuClosed() {
        if (mActionBarController != null) {
            mActionBarController.showMenuIndicator();
        }
    }

    @Override
    public void onScrollStarted() {
        OldImageLoader.get(this).block(this);
    }

    @Override
    public void onScrollEnded() {
        OldImageLoader.get(this).unblock(this);
    }

    @Override
    public void onBlockerClick() {
        if (mActionBarController != null) {
            mActionBarController.closeSearch(false);
        }
    }

    @Override
    public void onHomePressed() {
        if (this instanceof ScLandingPage){
            mRootView.animateToggleMenu();
        } else if (isTaskRoot()) {
            // empty backstack and not a landing page, might be from a notification or deeplink
            // just go to the home activity
            startActivity(new Intent(this, Home.class));
            finish();
        } else {
            super.onBackPressed();
        }
    }


    @NotNull
    @Override
    public Activity getActivity() {
        return this;
    }

    private final BroadcastReceiver mGeneralIntentListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Consts.GeneralIntents.ACTIVITIES_UNSEEN_CHANGED)) {
                mRootView.getMenu().refresh();
            } else if (action.equals(Consts.GeneralIntents.UNAUTHORIZED) && mIsForeground) {
                safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            } else if (action.equals(Actions.LOGGING_OUT)){
                mRootView.close();
                finish();
            }
        }
    };
}
