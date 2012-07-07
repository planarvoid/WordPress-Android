package com.soundcloud.android.activity;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.settings.AccountSettings;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ChangeLog;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.viewpagerindicator.TabPageIndicator;
import org.jetbrains.annotations.Nullable;

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class Main extends ScListActivity implements
        FetchModelTask.FetchModelListener<ScModel> {

    private static final int RESOLVING = 0;

    private View mSplash;

    public static final String TAB_TAG = "tab";
    private static final long SPLASH_DELAY = 1200;
    private static final long FADE_DELAY = 400;

    protected ScListView mListView;

    private ResolveFetchTask mResolveTask;
    private ChangeLog mChangeLog;
    private MainFragmentAdapter mAdapter;
    private ViewPager mPager;
    private TabPageIndicator mIndicator;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main);

        mAdapter = new MainFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        mChangeLog = new ChangeLog(this);

        final SoundCloudApplication app = getApp();

        final boolean showSplash = showSplash(state);
        mSplash = findViewById(R.id.splash);
        mSplash.setVisibility(showSplash ? View.VISIBLE : View.GONE);
        if (IOUtils.isConnected(this) &&
                app.getAccount() != null &&
                app.getToken().valid() &&
                !app.getLoggedInUser().isPrimaryEmailConfirmed() &&
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

        handleIntent(getIntent());

        mResolveTask = (ResolveFetchTask) getLastCustomNonConfigurationInstance();
        if (mResolveTask != null) {
            mResolveTask.setListener(this);
            if (!AndroidUtils.isTaskFinished(mResolveTask)) {
                showDialog(RESOLVING);
            }
        }
    }

    class MainFragmentAdapter extends FragmentPagerAdapter {
        protected final Content[] contents = new Content[]{Content.ME_FAVORITES};
        protected final int[] titleIds = new int[]{R.string.tab_title_my_likes};

        public MainFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public ScListFragment getItem(int position) {
            return ScListFragment.newInstance(contents[position]);
        }

        @Override
        public int getCount() {
            return contents.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getString(titleIds[position]);
        }
    }

    private boolean showSplash(Bundle state) {
        // don't show splash on configChanges (screen rotate)
        return state == null;
    }

    @Override
    protected void onResume() {
        if (getApp().getAccount() == null) {
            dismissSplash();
            getApp().addAccount(Main.this, managerCallback);
            finish();
        }
        super.onResume();
    }

    private void checkEmailConfirmed(final SoundCloudApplication app) {
        (new FetchUserTask(app) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    dismissSplash();
                } else {
                    startActivityForResult(
                            new Intent(Main.this, EmailConfirm.class)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        dismissSplash();
        if (resultCode == RESULT_OK) {
            if (data != null && Actions.RESEND.equals(data.getAction())) {
                // user pressed resend button
            }
        } else {
            // back button or no thanks
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null || (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return;

        if (handleViewUrl(intent)) {
            // already handled
        } else if (Actions.MESSAGE.equals(intent.getAction())) {
            final long recipient = intent.getLongExtra("recipient", -1);
            if (recipient != -1) {
                // TODO, fix this
                startActivity(new Intent(this, UserBrowser.class)
                        .putExtra("userId", recipient));
            }
        } else if (Actions.PLAYER.equals(intent.getAction())) {
            // start another activity to control history
            startActivity(new Intent(this, ScPlayer.class));

        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            startActivity(new Intent(this, ScSearch.class));

        } else if (Actions.MY_PROFILE.equals(intent.getAction()) && intent.hasExtra("userBrowserTag")) {
            Intent i = new Intent(this, UserBrowser.class);
            if (intent.getStringExtra("userBrowserTag") != null) {
                i.putExtra("userBrowserTag", intent.getStringExtra("userBrowserTag"));
            }
            startActivity(i);

        } else if (Actions.USER_BROWSER.equals(intent.getAction())) {
            startActivity((new Intent(this, UserBrowser.class).putExtras(intent.getExtras())));
        } else if (Actions.ACCOUNT_PREF.equals(intent.getAction())) {
            startActivity(
                    new Intent(this, AccountSettings.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            );
        } else if (justAuthenticated(intent)) {
            Log.d(TAG, "activity start after successful authentication");
        }

        intent.setAction("");
        intent.setData(null);
        intent.removeExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    protected boolean handleViewUrl(Intent intent) {
        if (!Intent.ACTION_VIEW.equals(intent.getAction()) && !FacebookSSO.isFacebookView(this, intent))
            return false;
        Uri data = intent.getData();
        if (data == null) return false;

        mResolveTask = new ResolveFetchTask(getApp());
        mResolveTask.setListener(this);
        mResolveTask.execute(data);
        showDialog(RESOLVING);
        return true;
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

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mResolveTask;
    }

    protected void onTrackLoaded(Track track, @Nullable String action) {
        startService(new Intent(Main.this, CloudPlaybackService.class)
                .setAction(CloudPlaybackService.PLAY_ACTION)
                .putExtra("track", track));

        startActivity(new Intent(Main.this, ScPlayer.class));

    }

    protected void onUserLoaded(User u, @Nullable String action) {
        startActivity(new Intent(this, UserBrowser.class)
                .putExtra("user", u)
                .putExtra("updateInfo", false));
    }

    @Override
    public void onError(long modelId) {
        removeDialog(RESOLVING);
        Toast.makeText(Main.this, R.string.error_loading_url, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSuccess(ScModel m, @Nullable String action) {
        removeDialog(RESOLVING);
        if (m instanceof Track) {
            onTrackLoaded((Track) m, null);
        } else if (m instanceof User) {
            onUserLoaded((User) m, null);
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
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);
        return true;
    }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, Dashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.menu_record:
                // app icon in action bar clicked; go home
                intent = new Intent(this, ScCreate.class);
                startActivity(intent);
                return true;
            case R.id.menu_search:
                // app icon in action bar clicked; go home
                intent = new Intent(this, ScSearch.class);
                startActivity(intent);
                return true;
            case R.id.menu_you:
                // app icon in action bar clicked; go home
                intent = new Intent(this, UserBrowser.class);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.FILTER:
                track(Page.Stream_stream_setting, getApp().getLoggedInUser());
                track(Click.Stream_main_stream_setting);

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dashboard_filter_title))
                        .setNegativeButton(R.string.dashboard_filter_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                track(Click.Stream_box_stream_cancel);
                            }
                        })
                        .setItems(new String[]{
                                getString(R.string.dashboard_filter_all),
                                getString(R.string.dashboard_filter_exclusive)
                        },
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final boolean exclusive = which == 1;
                                        /*
                                        SharedPreferencesUtils.apply(PreferenceManager
                                                .getDefaultSharedPreferences(Dashboard.this)
                                                .edit()
                                                .putBoolean(Consts.PrefKeys.EXCLUSIVE_ONLY_KEY, exclusive));

                                        ((EventsAdapterWrapper) mListView.getWrapper()).setContent(exclusive ?
                                                Content.ME_EXCLUSIVE_STREAM : Content.ME_SOUND_STREAM);

                                        mListView.getWrapper().reset();
                                        mListView.getRefreshableView().invalidateViews();
                                        mListView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mListView.getWrapper().onRefresh();
                                            }
                                        });

                                        track(exclusive ? Click.Stream_box_stream_only_Exclusive
                                                : Click.Stream_box_stream_all_tracks);
                                    }
                                })
                        .create()
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
*/

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case RESOLVING:
                ProgressDialog progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.resolve_progress));
                progress.setCancelable(true);
                progress.setIndeterminate(true);
                return progress;
            default:
                return super.onCreateDialog(id);
        }
    }
}
