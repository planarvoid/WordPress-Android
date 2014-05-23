package com.soundcloud.android.playback.widget;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.utils.Log;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import javax.inject.Inject;

class PlayerWidgetPresenter {

    private static final ComponentName PLAYER_WIDGET_PROVIDER = new ComponentName("com.soundcloud.android",
            PlayerAppWidgetProvider.class.getCanonicalName());
    private AppWidgetManager appWidgetManager;

    @Inject
    PlayerWidgetPresenter(AppWidgetManager appWidgetManager) {
        this.appWidgetManager = appWidgetManager;
    }

    /* package */ void updatePlayState(Context context, boolean isPlaying) {
        PlayerWidgetRemoteViews remoteViews = new PlayerWidgetRemoteViewsBuilder()
                .forIsPlaying(isPlaying)
                .build(context);
        pushUpdate(remoteViews);
    }

    /* package */ void updatePlayableInformation(Context context, Playable playable) {
        PlayerWidgetRemoteViews remoteViews = new PlayerWidgetRemoteViewsBuilder()
                .forPlayable(playable)
                .build(context);
        pushUpdate(remoteViews);
    }

    /* package */ void reset(Context context) {
        Log.d(PlayerWidgetPresenter.this, "resetting widget");
        PlayerWidgetRemoteViews remoteViews = new PlayerWidgetRemoteViewsBuilder().build(context);
        pushUpdate(remoteViews);
    }

    private void pushUpdate(RemoteViews views) {
        Log.d(PlayerWidgetPresenter.this, "Push update");
        appWidgetManager.updateAppWidget(PLAYER_WIDGET_PROVIDER, views);
    }
}
