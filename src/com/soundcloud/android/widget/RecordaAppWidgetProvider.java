package com.soundcloud.android.widget;

import com.soundcloud.android.R;
import com.soundcloud.android.service.CloudCreateService;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

    /**
     * Recording Widget
     */
    public class RecordaAppWidgetProvider extends AppWidgetProvider {
        static final String TAG = "RecordWidget";
        
        public static final String CMDAPPWIDGETUPDATE = "recordwidgetupdate";
        
        static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.RecordAppWidgetProvider");
        
        private static RecordaAppWidgetProvider sInstance;
        
        static synchronized RecordaAppWidgetProvider getInstance() {
            if (sInstance == null) {
                sInstance = new RecordaAppWidgetProvider();
            }
            return sInstance;
        }

        @Override
        public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            defaultAppWidget(context, appWidgetIds);
            
            // Send broadcast intent to any running CloudCreateService so it can
            // wrap around with an immediate update.
            Intent updateIntent = new Intent(CloudCreateService.SERVICECMD);
            updateIntent.putExtra(CloudCreateService.CMDNAME,
                    RecordaAppWidgetProvider.CMDAPPWIDGETUPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            context.sendBroadcast(updateIntent);
        }
        
        /**
         * Initialize given widgets to default state
         */
        private void defaultAppWidget(Context context, int[] appWidgetIds) {
            final Resources res = context.getResources();
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_record);
            
            
            // initialize controls
            
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
         * Check against {@link AppWidgetManager} if there are any instances of this widget.
         */
        private boolean hasInstances(Context context) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
            return (appWidgetIds.length > 0);
        }

        /**
         * Handle a change notification coming over from {@link CloudCreateService}
         */
        void notifyChange(CloudCreateService service, String what) {
            if (hasInstances(service)) {
                if (CloudCreateService.RECORD_STARTED.equals(what) ||
                        CloudCreateService.RECORD_STOPPED.equals(what) ||
                        CloudCreateService.RECORD_ERROR.equals(what)) {
                    performUpdate(service, null);
                }
            }
        }
        
        /**
         * Update all active widget instances by pushing changes 
         */
        void performUpdate(CloudCreateService service, int[] appWidgetIds) {
            final Resources res = service.getResources();
            final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_record);
            
            //set view state based on service

            // Link actions buttons to intents
            linkButtons(service, views, service.getCurrentState());
            
            pushUpdate(service, appWidgetIds, views);
        }

        /**
         * Link up various button actions using {@link PendingIntents}.
         * 
         * @param playerActive True if player is active in background, which means
         *            widget click will launch {@link MediaPlaybackActivityStarter},
         *            otherwise we launch {@link MusicBrowserActivity}.
         */
        private void linkButtons(Context context, RemoteViews views, int state) {
            // Connect up various buttons and touch events
            Intent intent;
            PendingIntent pendingIntent;
            
            final ComponentName serviceName = new ComponentName(context, CloudCreateService.class);
            
            // set button intents based on state
            switch (state){
                
            }
            
            
        }
    }

