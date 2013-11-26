package com.soundcloud.android.main;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.NowPlayingActionBarController;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends ActionBarActivity implements Tracker, ActionBarController.ActionBarOwner {
    protected static final int CONNECTIVITY_MSG = 0;
    protected NetworkConnectivityListener connectivityListener;
    private long mCurrentUserId;

    private Boolean mIsConnected;
    private boolean mIsForeground;

    @Inject
    protected AccountOperations mAccountOperations;
    protected PublicCloudAPI mPublicCloudAPI;

    @Nullable
    protected ActionBarController mActionBarController;
    private UnauthorisedRequestReceiver mUnauthoriedRequestReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView();

        mAccountOperations = new AccountOperations(this);
        mPublicCloudAPI = new PublicApi(this);
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
        mUnauthoriedRequestReceiver = new UnauthorisedRequestReceiver(getApplicationContext(), getSupportFragmentManager());
        // Volume mode should always be music in this app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        registerReceiver(mLoggingOutListener, new IntentFilter(Actions.LOGGING_OUT));
        if (getSupportActionBar() != null) {
            mActionBarController = createActionBarController();
        }


    }

    // Override this in activities with custom content views
    protected void setContentView() {
        setContentView(R.layout.container_layout);
    }

    // TODO: Ugly, but the support library (r19) does not update the AB title correctly via setTitle
    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        getSupportActionBar().setTitle(titleId);
    }

    protected ActionBarController createActionBarController() {
        return new NowPlayingActionBarController(this, mPublicCloudAPI);
    }

    public void restoreActionBar() {
        /** no-op. Used in {@link com.soundcloud.android.main.MainActivity#restoreActionBar()} */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        safeUnregisterReceiver(mLoggingOutListener);
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        if (mActionBarController != null) {
            mActionBarController.onDestroy();
        }
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

        //Ensures that ImageLoader will be resumed if the preceding activity was killed during scrolling
        ImageLoader.getInstance().resume();

        registerReceiver(mUnauthoriedRequestReceiver, new IntentFilter(Consts.GeneralIntents.UNAUTHORIZED));
        if (!mAccountOperations.soundCloudAccountExists()) {
            pausePlayback();
            finish();
            return;
        }

        mIsForeground = true;
        if (mActionBarController != null) {
            mActionBarController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        safeUnregisterReceiver(mUnauthoriedRequestReceiver);
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
        startService(new Intent(PlaybackService.Actions.PAUSE_ACTION));
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
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_ERROR_LOADING:
                return new AlertDialog.Builder(this).setTitle(R.string.error_loading_title)
                        .setMessage(R.string.error_loading_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_LOGOUT:
                return SettingsActivity.createLogoutDialog(this);

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
        if (mActionBarController != null) {
            return mActionBarController.onOptionsItemSelected(item);
        } else {
            return super.onOptionsItemSelected(item);
        }
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

    @NotNull
    @Override
    public ActionBarActivity getActivity() {
        return this;
    }

    private final BroadcastReceiver mLoggingOutListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    /**
     * For the search UI, we need to block out the UI completely. this might change as requirements to
     * To be implemented
     */
    @Override
    public boolean onSupportNavigateUp() {
        if (isTaskRoot()) {
            // empty backstack and not a landing page, might be from a notification or deeplink
            // just go to the home activity
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
        return true;
    }

    /**
     * Convenience method to get the content id for usage in one-off fragments
     */
    public static int getContentHolderViewId() {
        return R.id.holder;
    }

    private void safeUnregisterReceiver(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // This should not happen if the receiver is registered/unregistered in complementary methods and
            // the full lifecycle is respected, but it does.
            SoundCloudApplication.handleSilentException("Couldnt unregister receiver", e);
        }
    }
}
