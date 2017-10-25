package com.soundcloud.android.playback.mediasession;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.NotificationTrack;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;

import javax.inject.Inject;
import javax.inject.Named;

class MetadataOperations {
    private final Resources resources;
    private final TrackItemRepository trackRepository;
    private final ImageOperations imageOperations;
    private final Scheduler scheduler;

    @Inject
    MetadataOperations(Resources resources,
                       TrackItemRepository trackRepository,
                       ImageOperations imageOperations,
                       @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.resources = resources;
        this.trackRepository = trackRepository;
        this.imageOperations = imageOperations;
        this.scheduler = scheduler;
    }

    Observable<MediaMetadataCompat> metadata(final Urn urn,
                                             boolean isAd,
                                             Optional<MediaMetadataCompat> existingMetadata) {
        if (urn.isTrack()) {
            return trackRepository.track(urn)
                                  .filter(track -> track != null)
                                  .flatMapObservable(toTrackWithBitmap(existingMetadata))
                                  .map(toMediaMetadata(isAd))
                                  .subscribeOn(scheduler);
        } else if (isAd) {
            return adMediaMetadata().toObservable();
        } else {
            return Observable.empty();
        }
    }

    void preload(final Urn trackUrn) {
        metadata(trackUrn, false, Optional.absent()).subscribeWith(new DefaultObserver<>());
    }

    private Single<MediaMetadataCompat> adMediaMetadata() {
        return Single.just(new MediaMetadataCompat.Builder()
                                   .putString(METADATA_KEY_TITLE, resources.getString(R.string.ads_advertisement))
                                   .putString(METADATA_KEY_ARTIST, Strings.EMPTY)
                                   .putBitmap(METADATA_KEY_ART, getAdArtwork()).build());
    }

    @Nullable
    private Bitmap getAdArtwork() {
        return imageOperations.decodeResource(resources, R.drawable.notification_loading);
    }

    private Function<TrackItem, Observable<TrackAndBitmap>> toTrackWithBitmap(final Optional<MediaMetadataCompat> existingMetadata) {
        return trackItem -> {
            final Bitmap cachedBitmap = getCachedBitmap(trackItem.getUrn(), trackItem.getImageUrlTemplate());

            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                return Observable.just(new TrackAndBitmap(trackItem, Optional.of(cachedBitmap)));
            } else {
                return Single.concat(Single.just(new TrackAndBitmap(trackItem, getCurrentBitmap(existingMetadata))),
                                     loadArtwork(trackItem)).toObservable();
            }
        };
    }

    @Nullable
    private Bitmap getCachedBitmap(Urn urn, Optional<String> imageUrlTemplate) {
        final int targetSize = getTargetImageSize();
        return imageOperations.getCachedBitmap(urn, imageUrlTemplate, getImageSize(), targetSize, targetSize);
    }

    private Single<TrackAndBitmap> loadArtwork(final TrackItem trackItem) {
        final int targetSize = getTargetImageSize();

        return imageOperations.artwork(trackItem.getUrn(), trackItem.getImageUrlTemplate(), getImageSize(), targetSize, targetSize)
                              .map(bitmap -> new TrackAndBitmap(trackItem, Optional.fromNullable(bitmap)));
    }

    @Nullable
    private Optional<Bitmap> getCurrentBitmap(Optional<MediaMetadataCompat> metadata) {
        if (metadata.isPresent()) {
            Bitmap bitmap = metadata.get().getBitmap(METADATA_KEY_ART);
            if (bitmap != null && !bitmap.isRecycled()) {
                return Optional.of(bitmap);
            }
        }
        return Optional.absent();
    }

    private int getTargetImageSize() {
        return resources.getDimensionPixelSize(R.dimen.notification_image_large_size);
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getNotificationLargeIconImageSize(resources);
    }

    private Function<TrackAndBitmap, MediaMetadataCompat> toMediaMetadata(boolean isAd) {
        return trackAndBitmap -> {
            NotificationTrack notificationTrack =
                    new NotificationTrack(resources, trackAndBitmap.trackItem, isAd);

            Bitmap bitmap = notificationTrack.isAudioAd()
                            ? getAdArtwork()
                            : trackAndBitmap.bitmap.orNull();

            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_TITLE, notificationTrack.getTitle())
                    .putString(METADATA_KEY_ARTIST, notificationTrack.getCreatorName())
                    .putBitmap(METADATA_KEY_ART, bitmap);

            if (!isAd) {
                builder.putLong(METADATA_KEY_DURATION, notificationTrack.getDuration());
            }

            return builder.build();
        };
    }

    private final class TrackAndBitmap {
        private final TrackItem trackItem;
        private final Optional<Bitmap> bitmap;

        private TrackAndBitmap(TrackItem trackItem, Optional<Bitmap> bitmap) {
            this.trackItem = trackItem;
            this.bitmap = bitmap;
        }
    }
}
