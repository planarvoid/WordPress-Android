package com.soundcloud.android.activity;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.landing.FriendFinder;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.ScLandingPage;
import com.soundcloud.android.activity.landing.SuggestedUsers;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.model.Comment;
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
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.RelativeLayout;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends SherlockFragmentActivity implements Tracker,RootView.OnMenuStateListener, ImageLoader.LoadBlocker {
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


        mRootView.setOnMenuStateListener(this);
        mRootView.configureMenu(R.menu.main_nav, new MainMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClicked(int id) {
                switch (id) {
                    case R.id.nav_stream:
                        startNavActivity(Home.class);
                        return true;
                    case R.id.nav_news:
                        startNavActivity(News.class);
                        return true;
                    case R.id.nav_you:
                        startNavActivity(You.class);
                        return true;
                    case R.id.nav_record:
                        startNavActivity(ScCreate.class);
                        return true;
                    case R.id.nav_likes:
                        startActivity(getNavIntent(You.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(UserBrowser.Tab.EXTRA,UserBrowser.Tab.likes.tag));
                        return true;
                    case R.id.nav_followings:
                        startActivity(getNavIntent(You.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.followings.tag));
                        return true;
                    case R.id.nav_friend_finder:
                        startNavActivity(FriendFinder.class);
                        return true;
                    case R.id.nav_suggested_users:
                        startNavActivity(SuggestedUsers.class);
                        return true;
                    case R.id.nav_settings:
                        startActivity(new Intent(ScActivity.this, Settings.class));
                        mRootView.animateClose();
                        return false;
                }
                return false;
            }
        });

        if (!(this instanceof Home)) getSupportActionBar().setTitle(null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    protected void setupNowPlayingIndicator() {
        RelativeLayout nowPlayingHolder = (RelativeLayout) View.inflate(this, R.layout.now_playing_view, null);
        nowPlayingHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToPlayer();
            }
        });

        mNowPlaying = (NowPlayingIndicator) nowPlayingHolder.findViewById(R.id.waveform_progress);
        getSupportActionBar().setCustomView(nowPlayingHolder, new ActionBar.LayoutParams(Gravity.RIGHT));
        getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    protected abstract int getSelectedMenuId();

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

        if (getApp().getAccount() == null && !(this instanceof Home)) {
            startActivity(new Intent(this, Home.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        mRootView.onResume();
        mIsForeground = true;
        if (getApp().getAccount() == null) {
            pausePlayback();
            finish();
        }

        if (mNowPlaying != null) {
            mNowPlaying.resume();
        } else {
            setupNowPlayingIndicator();
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
        final int menuResourceId = getMenuResourceId();
        if (menuResourceId < 0) return true;

        getSupportMenuInflater().inflate(menuResourceId, menu);

        // Get the SearchView and set the searchable configuration
        if (menu.findItem(R.id.menu_search) != null){
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            /*
            This is how you can find the search text view. It's hacky. Hopefully we don't need it
            AutoCompleteTextView search_text = (AutoCompleteTextView) searchView.findViewById(searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null));
            if (search_text == null){
                search_text = (AutoCompleteTextView) searchView.findViewById(R.id.abs__search_src_text);
            }*/

            final SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
            searchView.setSearchableInfo(searchableInfo);

            final SuggestionsAdapter suggestionsAdapter = new SuggestionsAdapter(this, null, getApp());
            searchView.setSuggestionsAdapter(suggestionsAdapter);
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    final Uri itemUri = suggestionsAdapter.getItemUri(position);
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(itemUri));
                    return true;
                }
            });
        }

        return true;
    }

    private void goToPlayer() {
        if (!(this instanceof ScPlayer)){
            if (mRootView.isExpanded()) {
                startNavActivity(ScPlayer.class);
            } else {
                startActivity(new Intent(this, ScPlayer.class));
            }
        }
    }

    protected int getMenuResourceId(){
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mRootView.animateToggleMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public long getCurrentUserId() {
        if (mCurrentUserId == 0) {
            mCurrentUserId = SoundCloudApplication.getUserId();
        }
        return mCurrentUserId;
    }

    private final Handler connHandler = new Handler() {
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

    @Override
    public void onMenuOpenLeft() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        invalidateOptionsMenu();
    }

    @Override
    public void onMenuOpenRight() {
    }

    @Override
    public void onMenuClosed() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu();
    }

    @Override
    public void onScrollStarted() {
        ImageLoader.get(this).block(this);
    }

    @Override
    public void onScrollEnded() {
        ImageLoader.get(this).unblock(this);
    }
}
