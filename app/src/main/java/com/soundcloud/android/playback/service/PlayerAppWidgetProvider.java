package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.views.WidgetPlaybackRemoteViews;
import com.soundcloud.android.utils.Log;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerAppWidgetProvider extends AppWidgetProvider {
    public static final String TAG = "PlayerWidget";

    private static final ComponentName THIS_APPWIDGET = new ComponentName("com.soundcloud.android",
            PlayerAppWidgetProvider.class.getCanonicalName());

    @Inject
    public PlayerAppWidgetProvider() {
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        // initialize to empty view state
        pushUpdate(context, new WidgetPlaybackRemoteViews(context), appWidgetIds);

        // Send broadcast intent to any running PlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(Broadcasts.UPDATE_WIDGET_ACTION);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    private void pushUpdate(Context context, RemoteViews views, int[] appWidgetIds) {
        Log.d(TAG, "Push update");
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

    public void performUpdate(Context context, Intent intent) {
        Log.d(TAG, "performUpdate; intent = " + intent);
        String action = intent.getAction();
        if (hasInstances(context)) {
            if (action.equals(Broadcasts.META_CHANGED) ||
                    action.equals(Broadcasts.PLAYSTATE_CHANGED)) {

                final WidgetPlaybackRemoteViews.Args args = new WidgetPlaybackRemoteViews.Args(context, intent);
                pushUpdate(context, new WidgetPlaybackRemoteViews(args), new int[0]);

            } else if (action.equals(Broadcasts.RESET_ALL)) {
                pushUpdate(context, new WidgetPlaybackRemoteViews(context), new int[0]);
            }
        }
    }

    public void performUpdate(Context context, Playable playable, boolean isPlaying) {
        Log.d(TAG, "performUpdate; playable = " + playable);
        final WidgetPlaybackRemoteViews.Args args = new WidgetPlaybackRemoteViews.Args(context, playable, isPlaying);
        pushUpdate(context, new WidgetPlaybackRemoteViews(args), new int[0]);
    }

}

