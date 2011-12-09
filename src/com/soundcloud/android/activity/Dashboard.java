package com.soundcloud.android.activity;

import android.net.Uri;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.ScTabView;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected ScListView mListView;
    private String mTrackingPath;
    private boolean mIsActivityTab;
    private final String EXCLUSIVE_ONLY_KEY = "incoming_exclusive_only";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (redirectToMain()) return;

        CloudUtils.checkState(this);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            ScTabView trackListView;
            EmptyCollection ec = new EmptyCollection(this);

            if (Tabs.STREAM.equalsIgnoreCase(tab)) {
                ec.setMessageText(R.string.list_empty_stream_message)
                        .setImage(R.drawable.empty_follow)
                        .setActionText(R.string.list_empty_stream_action)
                        .setSecondaryText(R.string.list_empty_stream_secondary)
                        .setActionListener(new EmptyCollection.ActionListener() {
                            @Override
                            public void onAction() {
                                goToFriendFinder();
                            }

                            @Override
                            public void onSecondaryAction() {
                                goToFriendFinder();
                            }
                        });

                trackListView = createList(getIncomingType(),
                        Event.class,
                        ec,
                        Consts.ListId.LIST_STREAM, false);
                mTrackingPath = Consts.Tracking.STREAM;
            } else if (Tabs.ACTIVITY.equalsIgnoreCase(tab)) {
                mIsActivityTab = true;

                if (getApp().getLoggedInUser() == null || getApp().getLoggedInUser().track_count > 0){
                    ec.setMessageText(R.string.list_empty_activity_message)
                            .setImage(R.drawable.empty_share)
                            .setActionText(R.string.list_empty_activity_action)
                            .setSecondaryText(R.string.list_empty_activity_secondary)
                            .setActionListener(new EmptyCollection.ActionListener() {
                                @Override
                                public void onAction() {
                                    startActivity(new Intent(Actions.USER_BROWSER)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            .putExtra("userBrowserTag", UserBrowser.TabTags.tracks));
                                }

                                @Override
                                public void onSecondaryAction() {
                                    goTo101s();
                                }
                            });
                } else {
                    ec.setMessageText(R.string.list_empty_activity_nosounds_message)
                            .setImage(R.drawable.empty_rec)
                            .setActionText(R.string.list_empty_activity_nosounds_action)
                            .setSecondaryText(R.string.list_empty_activity_nosounds_secondary)
                            .setActionListener(new EmptyCollection.ActionListener() {
                                @Override
                                public void onAction() {
                                    startActivity(new Intent(Actions.RECORD)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                                }

                                @Override
                                public void onSecondaryAction() {
                                    goTo101s();
                                }
                            });
                }


                trackListView = createList(Consts.EventTypes.ACTIVITY,
                        Event.class,
                        ec,
                        Consts.ListId.LIST_ACTIVITY, true);
                mTrackingPath = Consts.Tracking.ACTIVITY;
            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
            setContentView(trackListView);
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }

    private void goToFriendFinder() {
        trackPage(Consts.Tracking.PEOPLE_FINDER);
        startActivity(new Intent(Actions.USER_BROWSER)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("userBrowserTag", UserBrowser.TabTags.friend_finder));
    }

    private void goTo101s() {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

    private int getIncomingType() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(EXCLUSIVE_ONLY_KEY, false)
                ? Consts.EventTypes.EXCLUSIVE
                : Consts.EventTypes.INCOMING;
    }

    @Override
    public void onResume() {
        super.onResume();
        trackPage(mTrackingPath);

        ((NotificationManager) getApp().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(mIsActivityTab ?
                        Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID :
                        Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);
    }

    protected ScTabView createList(int type, Class<?> model, EmptyCollection emptyView, int listId, boolean isNews) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), isNews, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, Content.fromEventType(type));

        final ScTabView view = new ScTabView(this);
        mListView = view.setLazyListView(buildList(!isNews), adpWrap, listId, true);
        adpWrap.setEmptyView(emptyView);
        return view;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null && mListView.getWrapper() != null){
            return mListView.getWrapper().saveState();
        } else {
            return null;
        }
    }

    // legacy action, redirect to Main
    private boolean redirectToMain() {
        if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            Intent intent = new Intent(this, Main.class);
            startActivity(intent);
            finish();
            return true;
        } else {
            return false;
        }
    }

    public void refreshIncoming() {
        mListView.onRefresh(false);
        if (Build.VERSION.SDK_INT >= 8) {
            mListView.smoothScrollToPosition(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         if (!mIsActivityTab) {
            menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.FILTER:
                new AlertDialog.Builder(this)
                   .setTitle(getString(R.string.dashboard_filter_title))
                   .setNegativeButton(R.string.dashboard_filter_cancel, null)
                        .setItems(new String[]{getString(R.string.dashboard_filter_all), getString(R.string.dashboard_filter_exclusive)},
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit()
                                                .putBoolean(EXCLUSIVE_ONLY_KEY, which == 1).commit();

                                        mListView.getWrapper().clearRefreshTask();
                                        mListView.getWrapper().reset();
                                        mListView.setLastUpdated(0);
                                        mListView.invalidateViews();
                                        mListView.post(new Runnable() {
                                            @Override public void run() {
                                                mListView.onRefresh(false);
                                            }
                                        });

                                    }
                                })

                   .create()
                   .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static final class Tabs {
        public static final String STREAM = "stream";
        public static final String ACTIVITY = "activity";
        public static final String RECORD = "record";
        public static final String PROFILE = "profile";
        public static final String SEARCH = "search";

        public static String fromAction(String action, String defaultTab) {
            String tab = defaultTab;
            if (action != null) {
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
                }
            }
            return tab;
        }
    }
}
