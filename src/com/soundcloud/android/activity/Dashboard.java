package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.model.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected ScListView mListView;
    private String mTrackingPath;
    private Tab mCurrentTab;

    private static final String EXCLUSIVE_ONLY_KEY = "incoming_exclusive_only";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Intent intent = getIntent();
        if (redirectToMain(intent)) return;

        CloudUtils.checkState(this);

        ScTabView trackListView;
        EmptyCollection ec = new EmptyCollection(this);

        mCurrentTab = Tab.fromIntent(intent);
        switch(mCurrentTab) {
            case STREAM:
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
                        Activity.class,
                        ec,
                        Consts.ListId.LIST_STREAM, false);
                mTrackingPath = Consts.Tracking.STREAM;
                break;

            case ACTIVITY:
                if (getApp().getLoggedInUser() == null || getApp().getLoggedInUser().track_count > 0) {
                    ec.setMessageText(R.string.list_empty_activity_message)
                            .setImage(R.drawable.empty_share)
                            .setActionText(R.string.list_empty_activity_action)
                            .setSecondaryText(R.string.list_empty_activity_secondary)
                            .setActionListener(new EmptyCollection.ActionListener() {
                                @Override public void onAction() {
                                    startActivity(new Intent(Actions.USER_BROWSER)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            .putExtra("userBrowserTag", UserBrowser.TabTags.tracks));
                                }

                                @Override public void onSecondaryAction() {
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
                trackListView = createList(Content.ME_ACTIVITIES,
                        Activity.class,
                        ec,
                        Consts.ListId.LIST_ACTIVITY, true);
                mTrackingPath = Consts.Tracking.ACTIVITY;
                break;
            default:
                throw new IllegalArgumentException("no valid tab extra");
        }

        setContentView(trackListView);

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

    private Content getIncomingType() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(EXCLUSIVE_ONLY_KEY, false)
                ? Content.ME_EXCLUSIVE_STREAM
                : Content.ME_SOUND_STREAM;
    }

    @Override
    public void onResume() {
        super.onResume();
        trackPage(mTrackingPath);
        ((NotificationManager) getApp().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(mCurrentTab == Tab.ACTIVITY ?
                        Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID :
                        Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mListView != null && mListView.getWrapper() != null){
            ((EventsAdapterWrapper) mListView.getWrapper()).onPause();
        }
    }

    protected ScTabView createList(Content content, Class<?> model, EmptyCollection emptyView, int listId, boolean isNews) {
        EventsAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>(), isNews, model);
        EventsAdapterWrapper adpWrap = new EventsAdapterWrapper(this, adp, content);

        final ScTabView view = new ScTabView(this);
        mListView = view.setLazyListView(buildList(!isNews), adpWrap, listId, true);
        mListView.getRefreshableView().setFastScrollEnabled(true);
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
    private boolean redirectToMain(Intent intent) {
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())) {
            startActivity(new Intent(this, Main.class));
            finish();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         if (mCurrentTab != Tab.ACTIVITY) {
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
                                        ((EventsAdapterWrapper) mListView.getWrapper()).setContent(which == 1 ?
                                                Content.ME_EXCLUSIVE_STREAM : Content.ME_SOUND_STREAM);
                                        mListView.getWrapper().reset();
                                        mListView.getRefreshableView().invalidateViews();
                                        mListView.post(new Runnable() {
                                            @Override public void run() {
                                                mListView.getWrapper().onRefresh();
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
            } else if (intent.hasExtra("tab")) {
                return fromString(intent.getStringExtra("tab"));
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
                intent.putExtra("tab", tag);
            }
            return intent;
        }
    }
}
