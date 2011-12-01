package com.soundcloud.android.service.playback;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class PlayerAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    private long mCurrentTrackId = -1;

    public static final String CMDAPPWIDGETUPDATE = "playerwidgetupdate";
    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.service.playback.PlayerAppWidgetProvider");

    private static PlayerAppWidgetProvider sInstance;

    public static synchronized PlayerAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new PlayerAppWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        // Send broadcast intent to any running CloudPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(CloudPlaybackService.SERVICECMD);
        updateIntent.putExtra(CloudPlaybackService.CMDNAME,
                PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_player);
        views.setTextViewText(R.id.title_txt, context.getString(R.string.widget_touch_to_open));
        views.setViewVisibility(R.id.by_txt, View.GONE);
        views.setViewVisibility(R.id.user_txt, View.GONE);

        // initialize controls
        linkButtons(context, views, null);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }

    /* package */ void notifyChange(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "notify change " + intent);
        if (hasInstances(context)) {
            if (action.equals(CloudPlaybackService.META_CHANGED) ||
                    action.equals(CloudPlaybackService.PLAYBACK_COMPLETE) ||
                    action.equals(CloudPlaybackService.PLAYSTATE_CHANGED) ||
                    action.equals(CloudPlaybackService.BUFFERING) ||
                    action.equals(CloudPlaybackService.BUFFERING_COMPLETE) ||
                    action.equals(CloudPlaybackService.PLAYBACK_ERROR) ||
                    action.equals(CloudPlaybackService.FAVORITE_SET)) {

                performUpdate(context, null, intent);
            }
        }
    }

    /* package */  void performUpdate(Context context, int[] appWidgetIds, Intent intent) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_player);


        final boolean playing = intent.getBooleanExtra("isSupposedToBePlaying", false);
        views.setImageViewResource(R.id.pause,
                playing ? R.drawable.ic_widget_pause_states : R.drawable.ic_widget_play_states);


        Track current = intent.getParcelableExtra("trackParcel");

        if (current != null) {
            views.setImageViewResource(R.id.btn_favorite,
                    current.user_favorite ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_favorite_states);
            if (mCurrentTrackId != current.id) {
                mCurrentTrackId = current.id;

                views.setTextViewText(R.id.title_txt, current.title);
                views.setTextViewText(R.id.user_txt, current.user.username);
                views.setViewVisibility(R.id.by_txt, View.VISIBLE);
                views.setViewVisibility(R.id.user_txt, View.VISIBLE);
            }

            linkButtons(context, views, current);
            pushUpdate(context, appWidgetIds, views);
        }
    }

    private void linkButtons(Context context, RemoteViews views, Track track) {
        // Connect up various buttons and touch events
        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);
        final Intent previous = new Intent(CloudPlaybackService.PREVIOUS_ACTION).setComponent(name);
        views.setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                0 /* requestCode */, previous, 0 /* flags */));

        final Intent toggle = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION).setComponent(name);
        views.setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                0 /* requestCode */, toggle, 0 /* flags */));

        final Intent next = new Intent(CloudPlaybackService.NEXT_ACTION).setComponent(name);
        views.setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                0 /* requestCode */, next, 0 /* flags */));

        final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        views.setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, 0, player, 0));

        if (track != null) {
            final Intent browser = new Intent(context, UserBrowser.class).putExtra("userId", track.user.id);
            views.setOnClickPendingIntent(R.id.user_txt,
                    PendingIntent.getActivity(context, 0, browser, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent toggleLike = new Intent(
                    track.user_favorite ?
                        CloudPlaybackService.REMOVE_FAVORITE :
                        CloudPlaybackService.ADD_FAVORITE)
                    .setComponent(name)
                    .putExtra("trackId", track.id);
            views.setOnClickPendingIntent(R.id.btn_favorite, PendingIntent.getService(context,
                    0 /* requestCode */, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }
}

