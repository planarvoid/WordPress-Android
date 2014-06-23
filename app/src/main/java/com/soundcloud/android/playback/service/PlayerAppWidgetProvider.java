package com.soundcloud.android.playback.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.utils.Log;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import javax.inject.Inject;

/**
 * Do not change this class name or move it to another package.
 * Doing so will break the widgets that were created with previous versions of the app.
 */
public class PlayerAppWidgetProvider extends AppWidgetProvider {

    @Inject PlayerWidgetController controller;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(PlayerAppWidgetProvider.class, "onUpdate");
        SoundCloudApplication.getObjectGraph().inject(this);
        controller.update();
    }

}

