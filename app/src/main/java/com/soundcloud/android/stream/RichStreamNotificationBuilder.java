package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.functions.Func1;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat.Builder;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class RichStreamNotificationBuilder extends StreamNotificationBuilder {

    private final ImageOperations imageOperations;
    private final Func1<Builder, Notification> buildNotification = new Func1<Builder, Notification>() {
        @Override
        public Notification call(Builder builder) {
            return builder.build();
        }
    };

    @Inject
    public RichStreamNotificationBuilder(Context context, ImageOperations imageOperations, Provider<Builder> builderProvider) {
        super(context, builderProvider);
        this.imageOperations = imageOperations;
    }

    public Observable<Notification> notification(List<PropertySet> streamItems) {
        final Builder builder = getBuilder(streamItems);
        final Urn artworkUrn = streamItems.get(0).get(PlayableProperty.URN);
        return builderWithArtwork(builder, artworkUrn).map(buildNotification);
    }

    protected Observable<Builder> builderWithArtwork(final Builder builder, Urn artworkUrn) {
        final Resources resources = appContext.getResources();
        final int targetIconWidth = resources.getDimensionPixelSize(R.dimen.notification_image_large_width);
        final int targetIconHeight = resources.getDimensionPixelSize(R.dimen.notification_image_large_height);
        final ApiImageSize notificationLargeIconImageSize = ApiImageSize.getNotificationLargeIconImageSize(resources);

        return imageOperations.artwork(artworkUrn, notificationLargeIconImageSize, targetIconWidth, targetIconHeight)
                .map(setLargeIcon(builder))
                .onErrorResumeNext(returnBuilderFunc(builder));

    }

    private Func1<Throwable, Observable<Builder>> returnBuilderFunc(final Builder builder) {
        return new Func1<Throwable, Observable<Builder>>() {
            @Override
            public Observable<Builder> call(Throwable throwable) {
                return Observable.just(builder);
            }
        };
    }

    private Func1<Bitmap, Builder> setLargeIcon(final Builder builder) {
        return new Func1<Bitmap, Builder>() {
            @Override
            public Builder call(Bitmap bitmap) {
                return builder.setLargeIcon(bitmap);
            }
        };
    }
}
