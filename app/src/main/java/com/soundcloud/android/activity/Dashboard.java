package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.SharedPreferencesUtils;
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

public class Dashboard extends ScListActivity {
    protected ScListView mListView;
    private Page mTrackingPage;
    private Main.Tab mCurrentTab;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Intent intent = getIntent();
        if (redirectToMain(intent)) return;

        ScTabView trackListView;
        EmptyCollection ec = new EmptyCollection(this);

        mCurrentTab = Main.Tab.fromIntent(intent);
        switch(mCurrentTab) {
            case STREAM:
                ec.setMessageText(R.string.list_empty_stream_message)
                        .setImage(R.drawable.empty_follow)
                        .setActionText(R.string.list_empty_stream_action)
                        .setSecondaryText(R.string.list_empty_stream_secondary)
                        .setButtonActionListener(new EmptyCollection.ActionListener() {
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

                mTrackingPage = Page.Stream_main;
                break;

            case ACTIVITY:
                if (getApp().getLoggedInUser() == null || getApp().getLoggedInUser().track_count > 0) {
                    ec.setMessageText(R.string.list_empty_activity_message)
                            .setImage(R.drawable.empty_share)
                            .setActionText(R.string.list_empty_activity_action)
                            .setSecondaryText(R.string.list_empty_activity_secondary)
                            .setButtonActionListener(new EmptyCollection.ActionListener() {
                                @Override
                                public void onAction() {
                                    startActivity(new Intent(Actions.MY_PROFILE)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.tracks));
                                }

                                @Override
                                public void onSecondaryAction() {
                                    goTo101s();
                                }
                            });
                } else {
                    EmptyCollection.ActionListener record = new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            startActivity(new Intent(Actions.RECORD)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                        }

                        @Override
                        public void onSecondaryAction() {
                            goTo101s();
                        }
                    };

                    ec.setMessageText(R.string.list_empty_activity_nosounds_message)
                            .setImage(R.drawable.empty_rec)
                            .setActionText(R.string.list_empty_activity_nosounds_action)
                            .setSecondaryText(R.string.list_empty_activity_nosounds_secondary)
                            .setButtonActionListener(record)
                            .setImageActionListener(record);
                }
                trackListView = createList(Content.ME_ACTIVITIES,
                        Activity.class,
                        ec,
                        Consts.ListId.LIST_ACTIVITY, true);

                mTrackingPage = Page.Activity_activity;
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
        track(Page.You_find_friends, getApp().getLoggedInUser());
        startActivity(new Intent(Actions.MY_PROFILE)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.friend_finder));
    }

    private void goTo101s() {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

    private Content getIncomingType() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Consts.PrefKeys.EXCLUSIVE_ONLY_KEY, false)
                ? Content.ME_EXCLUSIVE_STREAM
                : Content.ME_SOUND_STREAM;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getApp().getAccount() != null) {
            track(mTrackingPage, getApp().getLoggedInUser());
        }

        ((NotificationManager) getApp().getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(mCurrentTab == Main.Tab.ACTIVITY ?
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
         if (mCurrentTab != Main.Tab.ACTIVITY) {
            menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

}
