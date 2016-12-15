package com.soundcloud.android.playback.widget;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import javax.inject.Inject;

class PlayerWidgetPresenter {

    private static final ComponentName PLAYER_WIDGET_PROVIDER = new ComponentName(BuildConfig.APPLICATION_ID,
                                                                                  PlayerAppWidgetProvider.class.getCanonicalName());

    private final AppWidgetManager appWidgetManager;
    private final ImageOperations imageOperations;

    private Subscription artworkSubscription = RxUtils.invalidSubscription();

    @Nullable private WidgetItem widgetItem;

    @Inject
    PlayerWidgetPresenter(AppWidgetManager appWidgetManager, ImageOperations imageOperations) {
        this.appWidgetManager = appWidgetManager;
        this.imageOperations = imageOperations;
    }

    void updateForVideoAd(final Context context) {
        artworkSubscription.unsubscribe();
        widgetItem = WidgetItem.forVideoAd(context.getResources());
        updateRemoveViews(context);
    }

    void updateForAudioAd(final Context context) {
        artworkSubscription.unsubscribe();
        widgetItem = WidgetItem.forAudioAd(context.getResources());
        updateRemoveViews(context);
    }

    void updatePlayState(Context context, boolean isPlaying) {
        if (widgetItem != null) {
            PlayerWidgetRemoteViews remoteViews = new PlayerWidgetRemoteViewsBuilder()
                    .forIsPlaying(widgetItem, isPlaying)
                    .build(context);
            pushUpdate(remoteViews);
        }
    }

    void updateTrackInformation(final Context context, final TrackItem trackItem) {
        artworkSubscription.unsubscribe();
        widgetItem = WidgetItem.fromTrackItem(trackItem);
        updateAndLoadArtwork(context);
    }

    private void updateAndLoadArtwork(Context context) {
        Bitmap cachedArtwork = getCachedBitmap(context, widgetItem);
        updateRemoveViews(context, cachedArtwork);
        if (cachedArtwork == null) {
            loadArtwork(context);
        }
    }

    private void loadArtwork(Context context) {
        artworkSubscription = imageOperations.artwork(widgetItem,
                                                      getApiImageSize(context.getResources()),
                                                      context.getResources()
                                                             .getDimensionPixelSize(R.dimen.widget_image_estimated_width),
                                                      context.getResources()
                                                             .getDimensionPixelSize(R.dimen.widget_image_estimated_height))
                                             .observeOn(AndroidSchedulers.mainThread())
                                             .subscribe(getArtworkSubscriber(context));
    }

    private void updateRemoveViews(Context context) {
        pushUpdate(new PlayerWidgetRemoteViewsBuilder()
                           .forItem(widgetItem)
                           .build(context));
    }

    private void updateRemoveViews(Context context, Bitmap artwork) {
        PlayerWidgetRemoteViews remoteViews = new PlayerWidgetRemoteViewsBuilder()
                .forItem(widgetItem)
                .forArtwork(artwork)
                .build(context);
        pushUpdate(remoteViews);
    }

    @NotNull
    private DefaultSubscriber<Bitmap> getArtworkSubscriber(final Context context) {
        return new DefaultSubscriber<Bitmap>() {
            @Override
            public void onNext(Bitmap bitmap) {
                updateRemoveViews(context, bitmap);
            }
        };
    }

    void reset(Context context) {
        Log.d(PlayerWidgetPresenter.this, "resetting widget");
        artworkSubscription.unsubscribe();
        widgetItem = null;

        pushUpdate(buildEmptyRemoteViews(context));
    }

    private RemoteViews buildEmptyRemoteViews(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget_empty_player);
        remoteViews.setOnClickPendingIntent(R.id.logo, getPendingIntentForMainActivity(context));
        return remoteViews;
    }

    private PendingIntent getPendingIntentForMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context,
                                         R.id.player_widget_request_id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void pushUpdate(RemoteViews views) {
        Log.d(PlayerWidgetPresenter.this, "Push update");
        appWidgetManager.updateAppWidget(PLAYER_WIDGET_PROVIDER, views);
    }

    private Bitmap getCachedBitmap(Context context, final ImageResource imageResource) {
        return imageOperations.getCachedBitmap(imageResource, getApiImageSize(context.getResources()),
                                               context.getResources()
                                                      .getDimensionPixelSize(R.dimen.widget_image_estimated_width),
                                               context.getResources()
                                                      .getDimensionPixelSize(R.dimen.widget_image_estimated_height));
    }

    private ApiImageSize getApiImageSize(Resources resources) {
        return ApiImageSize.getNotificationLargeIconImageSize(resources);
    }
}
