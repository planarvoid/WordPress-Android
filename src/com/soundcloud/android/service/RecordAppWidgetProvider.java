package com.soundcloud.android.service;

import com.soundcloud.android.R;
import com.soundcloud.android.ScPlaybackActivityStarter;
import com.soundcloud.android.activity.Dashboard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.RemoteViews;

    /**
     * Recording Widget
     */
    public class RecordAppWidgetProvider extends AppWidgetProvider {
        static final String TAG = "RecordWidget";
        
        public static final String CMDAPPWIDGETUPDATE = "recordwidgetupdate";
        
        static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.RecordAppWidgetProvider");
        
        private static RecordAppWidgetProvider sInstance;
        
        static synchronized RecordAppWidgetProvider getInstance() {
            if (sInstance == null) {
                sInstance = new RecordAppWidgetProvider();
            }
            return sInstance;
        }

        @Override
        public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            defaultAppWidget(context, appWidgetIds);
            
           
        }
        
        /**
         * Initialize given widgets to default state
         */
        private void defaultAppWidget(Context context, int[] appWidgetIds) {
            final Resources res = context.getResources();
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_record);
            
            
            linkButtons(context, views, CloudCreateService.States.IDLE_RECORDING);
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
        
        /**
         * Link up various button actions using {@link PendingIntents}.
         * 
         * @param playerActive True if player is active in background, which means
         *            widget click will launch {@link ScPlaybackActivityStarter},
         *            otherwise we launch {@link MusicBrowserActivity}.
         */
        private void linkButtons(Context context, RemoteViews views, int state) {
            // Connect up various buttons and touch events
            Intent intent;
            PendingIntent pendingIntent;
            
            Intent i = (new Intent(context, Dashboard.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("tabIndex", Dashboard.TabIndexes.TAB_RECORD);

            pendingIntent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
            
            views.setOnClickPendingIntent(R.id.btn_action, pendingIntent);
            
        }
    }

