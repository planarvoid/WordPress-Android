package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.NowPlayingActionBarController;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
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

import java.lang.ref.WeakReference;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends ActionBarActivity implements ActionBarController.ActionBarOwner {

    protected static final int CONNECTIVITY_MSG = 0;
    private static final String BUNDLE_CONFIGURATION_CHANGE = "BUNDLE_CONFIGURATION_CHANGE";

    protected NetworkConnectivityListener connectivityListener;
    private long currentUserId;

    private Boolean isConnected;
    private boolean isForeground;
    private boolean onCreateCalled;
    private boolean isConfigurationChange;

    private Subscription userEventSubscription = Subscriptions.empty();

    private ImageOperations imageOperations;

    protected AccountOperations accountOperations;
    protected EventBus eventBus;

    @Nullable
    protected ActionBarController actionBarController;
    private UnauthorisedRequestReceiver unauthoriedRequestReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView();

        imageOperations = SoundCloudApplication.fromContext(this).getImageOperations();
        eventBus = SoundCloudApplication.fromContext(this).getEventBus();
        accountOperations = SoundCloudApplication.fromContext(this).getAccountOperations();

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this.getClass()));

        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
        unauthoriedRequestReceiver = new UnauthorisedRequestReceiver(getApplicationContext(), getSupportFragmentManager());
        // Volume mode should always be music in this app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        userEventSubscription = eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, userEventObserver);
        if (getSupportActionBar() != null) {
            actionBarController = createActionBarController();
        }

        onCreateCalled = true;

        if (savedInstanceState != null) {
            isConfigurationChange = savedInstanceState.getBoolean(BUNDLE_CONFIGURATION_CHANGE, false);
        }
    }

    // Override this in activities with custom content views
    protected void setContentView() {
        setContentView(R.layout.container_layout);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // XXX : This is false in some situations where we seem to actually be changing configurations
        // (hit the power off button on a genymotion emulator while in landscape). This is not conclusive yet. Investigating further
        outState.putBoolean(BUNDLE_CONFIGURATION_CHANGE, getChangingConfigurations() != 0);
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
        return new NowPlayingActionBarController(this, eventBus);
    }

    public void restoreActionBar() {
        /** no-op. Used in {@link com.soundcloud.android.main.MainActivity#restoreActionBar()} */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        userEventSubscription.unsubscribe();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        if (actionBarController != null) {
            actionBarController.onDestroy();
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
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this.getClass()));

        //Ensures that ImageLoader will be resumed if the preceding activity was killed during scrolling
        imageOperations.resume();

        registerReceiver(unauthoriedRequestReceiver, new IntentFilter(Consts.GeneralIntents.UNAUTHORIZED));
        if (!accountOperations.isUserLoggedIn()) {
            sendBroadcast(new Intent(PlaybackService.Actions.RESET_ALL));
            finish();
            return;
        }

        isForeground = true;
        if (actionBarController != null) {
            actionBarController.onResume();
        }
    }

    @Override
    protected void onPause() {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this.getClass()));

        safeUnregisterReceiver(unauthoriedRequestReceiver);
        isForeground = false;
        onCreateCalled = false;
        if (actionBarController != null) {
            actionBarController.onPause();
        }
        super.onPause();
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    public SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    /**
     * @return true if the Activity didn't receive a create event before onResume, e.g. when returning from another
     * activity further up the task stack.
     */
    protected boolean isReallyResuming() {
        return !onCreateCalled;
    }

    protected boolean isConfigurationChange() {
        return isConfigurationChange;
    }

    protected boolean shouldTrackScreen() {
        return !isConfigurationChange() || isReallyResuming();
    }

    public boolean isForeground() {
        return isForeground;
    }

    public boolean isConnected() {
        if (isConnected == null) {
            if (connectivityListener == null) {
                isConnected = true;
            } else {
                // isConnected not set yet
                NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                isConnected = networkInfo == null || networkInfo.isConnectedOrConnecting();
            }
        }
        return isConnected;
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
        this.isConnected = isConnected;
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
                return new AlertDialog.Builder(this).setTitle(R.string.menu_clear_user_title)
                        .setMessage(R.string.menu_clear_user_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LogoutActivity.start(ScActivity.this);
                            }
                        }).create();

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
        if (actionBarController != null) {
            actionBarController.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarController != null) {
            return actionBarController.onOptionsItemSelected(item);
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public long getCurrentUserId() {
        if (currentUserId == 0) {
            currentUserId = accountOperations.getLoggedInUserId();
        }
        return currentUserId;
    }

    private static final class ConnectivityHandler extends Handler {
        private WeakReference<ScActivity> contextRef;

        private ConnectivityHandler(ScActivity context) {
            this.contextRef = new WeakReference<ScActivity>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            final ScActivity context = contextRef.get();
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

    @NotNull
    @Override
    public ActionBarActivity getActivity() {
        return this;
    }

    private final DefaultSubscriber<CurrentUserChangedEvent> userEventObserver = new DefaultSubscriber<CurrentUserChangedEvent>() {
        @Override
        public void onNext(CurrentUserChangedEvent args) {
            if (args.getKind() == CurrentUserChangedEvent.USER_REMOVED) {
                finish();
            }
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
            ErrorUtils.handleSilentException("Couldnt unregister receiver", e);
        }
    }
}
