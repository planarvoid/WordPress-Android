package com.soundcloud.android.service.playback;

import com.soundcloud.android.R;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.soundcloud.android.model.Sound;
import com.soundcloud.android.view.play.PlaybackRemoteViews;

public class PlayerAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    private long mCurrentTrackId = -1;

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
        updateWidget(context, appWidgetIds);

        // Send broadcast intent to any running CloudPlaybackService so it can
        // wrap around with an immediate update.

        Intent updateIntent = new Intent(CloudPlaybackService.UPDATE_WIDGET_ACTION);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    private void updateWidget(Context context, int[] appWidgetIds) {
        final PlaybackRemoteViews views = new PlaybackRemoteViews(context.getPackageName(), R.layout.appwidget_player);
        views.setTextViewText(R.id.title_txt, context.getString(R.string.widget_touch_to_open));
        views.setViewVisibility(R.id.by_txt, View.GONE);
        views.setViewVisibility(R.id.user_txt, View.GONE);

        // initialize controls
        views.linkButtonsWidget(context, -1, -1, false);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
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

        // TODO, move to ScModelManager to get data

        String action = intent.getAction();
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "notify change " + intent);
        if (hasInstances(context)) {
            if (action.equals(CloudPlaybackService.META_CHANGED) ||
                    action.equals(CloudPlaybackService.PLAYBACK_COMPLETE) ||
                    action.equals(CloudPlaybackService.PLAYSTATE_CHANGED) ||
                    action.equals(CloudPlaybackService.BUFFERING) ||
                    action.equals(CloudPlaybackService.BUFFERING_COMPLETE) ||
                    action.equals(CloudPlaybackService.PLAYBACK_ERROR) ||
                    action.equals(Sound.ACTION_TRACK_ASSOCIATION_CHANGED)
                            && intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1) == mCurrentTrackId) {

                performUpdate(context, new int[0], intent);
            }
        }
    }

    /* package */  void performUpdate(Context context, int[] appWidgetIds, Intent intent) {
        final PlaybackRemoteViews views = new PlaybackRemoteViews(context.getPackageName(), R.layout.appwidget_player);
        views.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, false));

        final long trackId = intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1);
        if (trackId != -1){
            views.setImageViewResource(R.id.btn_like, intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isLike, false)
                    ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_like_states);

            if (mCurrentTrackId != trackId) {
                mCurrentTrackId = trackId;
                views.setCurrentTrackTitle(intent.getStringExtra(CloudPlaybackService.BroadcastExtras.title));
                views.setCurrentUsername(intent.getStringExtra(CloudPlaybackService.BroadcastExtras.username));
            }
            views.linkButtonsNotification(context);

            pushUpdate(context, appWidgetIds, views);
        }
    }
}

