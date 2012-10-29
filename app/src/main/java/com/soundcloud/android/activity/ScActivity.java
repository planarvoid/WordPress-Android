package com.soundcloud.android.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.landing.ScLandingPage;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.ScSearch;
import com.soundcloud.android.activity.landing.Stream;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.AddCommentDialog;
import com.soundcloud.android.view.MainMenu;
import com.soundcloud.android.view.NowPlayingIndicator;
import com.soundcloud.android.view.RootView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends SherlockFragmentActivity implements Tracker {
    protected static final int CONNECTIVITY_MSG = 0;
    protected NetworkConnectivityListener connectivityListener;
    private long mCurrentUserId;

    protected RootView mRootView;
    private Boolean mIsConnected;
    private boolean mIsForeground;

    private NowPlayingIndicator mNowPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);

        // Volume mode should always be music in this app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mRootView = new RootView(this, getSelectedMenuId());
        super.setContentView(mRootView);



        mRootView.configureMenu(R.menu.main_nav, new MainMenu.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClicked(int id) {
                switch (id) {
                    case R.id.nav_stream:
                        startNavActivity(Stream.class);
                        break;
                    case R.id.nav_news:
                        startNavActivity(News.class);
                        break;
                    case R.id.nav_you:
                        startNavActivity(You.class);
                        break;
                    case R.id.nav_record:
                        startNavActivity(ScCreate.class);
                        break;
                    case R.id.nav_settings:
                        startActivity(new Intent(ScActivity.this, Settings.class));
                        mRootView.animateClose();
                        break;

                }
            }

            @Override
            public void onSearchQuery(Search search) {
                startActivity(getNavIntent(ScSearch.class)
                        .putExtra(ScSearch.EXTRA_QUERY, search.query)
                        .putExtra(ScSearch.EXTRA_SEARCH_TYPE, search.search_type));
            }

            @Override
            public void onSearchSuggestedTrackClicked(long id) {
                // go to track, for now just play it
                startService(new Intent(CloudPlaybackService.PLAY_ACTION).putExtra(CloudPlaybackService.EXTRA_TRACK_ID, id));
                mRootView.animateClose();
            }

            @Override
            public void onSearchSuggestedUserClicked(long id) {
                // go to user
                startActivity(getNavIntent(UserBrowser.class).putExtra(UserBrowser.EXTRA_USER_ID, id));
            }
        });

        configureActionBar();

        if (this instanceof ScLandingPage){
            getApp().setAccountData(User.DataKeys.LAST_LANDING_PAGE_IDX, ((ScLandingPage) this).getPageValue().key);
        }

        if (savedInstanceState == null) {
            /*Fragment newFragment = new PlayerFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(mRootView.getPlayerHolderId(), newFragment).commit();*/

            handleIntent(getIntent());
        }
    }

    protected abstract int getSelectedMenuId();

    /**
     * Basically, hack the action bar to make it look like next
     */
    private void configureActionBar() {

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(!(this instanceof ScLandingPage));
        if (getApp().getLoggedInUser() != null) getSupportActionBar().setTitle(getApp().getLoggedInUser().username);

        // configure home image to fill vertically
        final float density = getResources().getDisplayMetrics().density;
        ImageView homeImage = (ImageView) getWindow().getDecorView().findViewById(android.R.id.home);
        if (homeImage == null){ // sherlock compatibility id
            homeImage = (ImageView) getWindow().getDecorView().findViewById(R.id.abs__home);
        }
        if (homeImage != null) {

            ViewGroup parent = (ViewGroup) homeImage.getParent();
            parent.setBackgroundColor(0x00000000);
            homeImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.logo_states));

            final int paddingVert = (int) (13 * density);
            final int paddingHor = (int) (5 * density);
            homeImage.setPadding(paddingHor, paddingVert, paddingHor, paddingVert);
            homeImage.setDuplicateParentStateEnabled(true);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) homeImage.getLayoutParams();
            lp.topMargin = lp.bottomMargin = 0;
            //homeImage.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            homeImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        // configure title for extra spacing
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        View title = getWindow().getDecorView().findViewById(titleId);
        if (title == null){ // sherlock compatibility id
            title = getWindow().getDecorView().findViewById(R.id.abs__action_bar_title);
        }
        if (title != null){
            ViewGroup parent = (ViewGroup) title.getParent();
            parent.setPadding((int) (parent.getPaddingLeft() + density * 10),
                    parent.getPaddingTop(), parent.getPaddingRight(), parent.getPaddingBottom());
        }
    }

    @Override
    public void setContentView(int id) {
        setContentView(View.inflate(this, id, new FrameLayout(this)));

    }

    @Override
    public void setContentView(View layout) {
        layout.setBackgroundDrawable(getWindow().getDecorView().getBackground());
        layout.setDrawingCacheBackgroundColor(Color.WHITE);
        mRootView.setContent(layout);
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

    private void startNavActivity(Class activity) {
        if (ScLandingPage.class.isAssignableFrom(activity)){
            startActivity(getNavIntent(activity).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(getNavIntent(activity));
        }

    }

    private Intent getNavIntent(Class activity) {
        return new Intent(ScActivity.this, activity)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra(RootView.EXTRA_ROOT_VIEW_STATE, mRootView.getMenuBundle());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
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
        mRootView.onResume();
        mIsForeground = true;
        if (getApp().getAccount() == null) {
            pausePlayback();
            finish();
        }

        if (mNowPlaying != null) {
            mNowPlaying.resume();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;

        if (mNowPlaying != null) {
            mNowPlaying.pause();
        }
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    public void pausePlayback() {
        startService(new Intent(this, CloudPlaybackService.class).setAction(CloudPlaybackService.PAUSE_ACTION));
    }

    public SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    public boolean isForeground(){
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

    public void safeShowDialog(Dialog dialog) {
        if (!isFinishing()) {
            dialog.show();
        }
    }

    protected void onDataConnectionChanged(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected) {
            // clear image loading errors
            ImageLoader.get(ScActivity.this).clearErrors();
        }
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNAUTHORIZED:
                return new AlertDialog.Builder(this).setTitle(R.string.error_unauthorized_title)
                        .setMessage(R.string.error_unauthorized_message).setNegativeButton(
                                R.string.menu_settings, new DialogInterface.OnClickListener() {
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
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(getMenuResourceId(), menu);

        if (mNowPlaying != null) {
            mNowPlaying.destroy();
            mNowPlaying = null;
        }

        MenuItem waveform = menu.findItem(R.id.menu_waveform);
        mNowPlaying = (NowPlayingIndicator) waveform.getActionView().findViewById(R.id.waveform_progress);

        if (this instanceof ScPlayer) {
            menu.add(menu.size(), Consts.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                    R.drawable.ic_menu_refresh);
        }
        return true;
    }

    protected int getMenuResourceId(){
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHomeButtonPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    protected void onHomeButtonPressed() {
        if (this instanceof ScLandingPage) {
            mRootView.animateToggleMenu();
        } else {
            onBackPressed();
        }
    }

    public long getCurrentUserId() {
        if (mCurrentUserId == 0) {
            mCurrentUserId = SoundCloudApplication.getUserId();
        }
        return mCurrentUserId;
    }

    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final ScActivity ctxt = ScActivity.this;
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (msg.obj instanceof NetworkInfo) {
                        NetworkInfo networkInfo = (NetworkInfo) msg.obj;
                        final boolean connected = networkInfo.isConnectedOrConnecting();
                        if (connected) {
                            ImageLoader.get(getApplicationContext()).clearErrors();

                            // announce potential proxy change
                            sendBroadcast(new Intent(Actions.CHANGE_PROXY_ACTION)
                                    .putExtra(Actions.EXTRA_PROXY, IOUtils.getProxy(ctxt, networkInfo)));
                        }
                        ctxt.onDataConnectionChanged(connected);
                    }
                    break;
            }
        }
    };

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

    public void addNewComment(final Comment comment) {
        getApp().pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
    }
}
