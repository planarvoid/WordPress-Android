package com.soundcloud.android.activity;

import com.actionbarsherlock.view.Menu;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class Dashboard extends ScListActivity {
    protected ScListView mListView;
    private Page mTrackingPage;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Intent intent = getIntent();
        if (redirectToMain(intent)) return;

        EmptyCollection ec = new EmptyCollection(this);

        /*
        mCurrentTab = Main.Tab.fromIntent(intent);
        switch(mCurrentTab) {
            case ACTIVITY:

                            if (getApp().getLoggedInUser() == null || getApp().getLoggedInUser().track_count > 0) {
                                ec.setMessageText(R.string.list_empty_activity_message)
                                        .setImage(R.drawable.empty_share)
                                        .setActionText(R.string.list_empty_activity_action)
                                        .setSecondaryText(R.string.list_empty_activity_secondary)
                                        .setActionListener(new EmptyCollection.ActionListener() {
                                            @Override public void onAction() {
                                                startActivity(new Intent(Actions.MY_PROFILE)
                                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                                        .putExtra("userBrowserTag", UserBrowser.Tab.tracks.name()));
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

                            mTrackingPage = Page.Activity_activity;
                            break;
            default:
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

                mTrackingPage = Page.Stream_main;
                break;

        }*/


        mTrackingPage = Page.Stream_main;

        setContentView(null);


    }

    private void goToFriendFinder() {
        track(Page.You_find_friends, getApp().getLoggedInUser());
        startActivity(new Intent(Actions.MY_PROFILE)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.friend_finder.name()));
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
                .cancel(Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);
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
        menu.add(menu.size(), Consts.OptionsMenu.FILTER, 0, R.string.menu_stream_setting).setIcon(
                R.drawable.ic_menu_incoming);

        return super.onCreateOptionsMenu(menu);
    }

}
