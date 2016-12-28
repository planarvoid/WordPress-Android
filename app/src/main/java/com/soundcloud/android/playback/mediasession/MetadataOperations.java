package com.soundcloud.android.playback.mediasession;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.NotificationTrack;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;

import javax.inject.Inject;
import javax.inject.Named;

class MetadataOperations {
    private final Resources resources;
    private final TrackRepository trackRepository;
    private final ImageOperations imageOperations;
    private final Scheduler scheduler;

    @Inject
    MetadataOperations(Resources resources,
                       TrackRepository trackRepository,
                       ImageOperations imageOperations,
                       @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.resources = resources;
        this.trackRepository = trackRepository;
        this.imageOperations = imageOperations;
        this.scheduler = scheduler;
    }

    Observable<MediaMetadataCompat> metadata(final Urn urn,
                                             boolean isAd,
                                             Optional<MediaMetadataCompat> existingMetadata) {
        if (urn.isTrack()) {
            return trackRepository
                    .track(urn)
                    .filter(track -> track != null)
                    .doOnNext(trackItem -> trackItem.setAd(isAd))
                    .flatMap(toTrackWithBitmap(existingMetadata))
                    .map(toMediaMetadata())
                    .subscribeOn(scheduler);
        } else if (isAd) {
            return adMediaMetadata();
        } else {
            return Observable.empty();
        }
    }

    void preload(final Urn trackUrn) {
        fireAndForget(metadata(trackUrn, false, Optional.<MediaMetadataCompat>absent()));
    }

    private Observable<MediaMetadataCompat> adMediaMetadata() {
        return Observable.just(new MediaMetadataCompat.Builder()
                                       .putString(METADATA_KEY_TITLE, resources.getString(R.string.ads_advertisement))
                                       .putString(METADATA_KEY_ARTIST, Strings.EMPTY)
                                       .putBitmap(METADATA_KEY_ART, getAdArtwork()).build());
    }

    @Nullable
    private Bitmap getAdArtwork() {
        return imageOperations.decodeResource(resources, R.drawable.notification_loading);
    }

    private Func1<TrackItem, Observable<TrackAndBitmap>> toTrackWithBitmap(final Optional<MediaMetadataCompat> existingMetadata) {
        return new Func1<TrackItem, Observable<TrackAndBitmap>>() {
            @Override
            public Observable<TrackAndBitmap> call(final TrackItem track) {
                final SimpleImageResource imageResource = SimpleImageResource.create(track);
                final Bitmap cachedBitmap = getCachedBitmap(imageResource);

                if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                    return Observable.just(new TrackAndBitmap(track, Optional.of(cachedBitmap)));
                } else {
                    return Observable.concat(
                            Observable.just(new TrackAndBitmap(track, getCurrentBitmap(existingMetadata))),
                            loadArtwork(track, imageResource)
                    );
                }
            }
        };
    }

    private Bitmap getCachedBitmap(SimpleImageResource imageResource) {
        final int targetSize = getTargetImageSize();
        return imageOperations.getCachedBitmap(imageResource, getImageSize(), targetSize, targetSize);
    }

    private Observable<TrackAndBitmap> loadArtwork(final TrackItem track, final SimpleImageResource imageResource) {
        final int targetSize = getTargetImageSize();

        return imageOperations.artwork(imageResource, getImageSize(), targetSize, targetSize)
                              .map(new Func1<Bitmap, TrackAndBitmap>() {
                                  @Override
                                  public TrackAndBitmap call(Bitmap bitmap) {
                                      return new TrackAndBitmap(track, Optional.fromNullable(bitmap));
                                  }
                              });
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
        return resources.getDimensionPixelSize(R.dimen.notification_image_large_width);
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getNotificationLargeIconImageSize(resources);
    }

    private Func1<TrackAndBitmap, MediaMetadataCompat> toMediaMetadata() {
        return new Func1<TrackAndBitmap, MediaMetadataCompat>() {
            @Override
            public MediaMetadataCompat call(TrackAndBitmap trackAndBitmap) {
                NotificationTrack notificationTrack =
                        new NotificationTrack(resources, trackAndBitmap.track);

                Bitmap bitmap = notificationTrack.isAudioAd()
                                ? getAdArtwork()
                                : trackAndBitmap.bitmap.orNull();

                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                        .putString(METADATA_KEY_TITLE, notificationTrack.getTitle())
                        .putString(METADATA_KEY_ARTIST, notificationTrack.getCreatorName())
                        .putBitmap(METADATA_KEY_ART, bitmap);

                if (!notificationTrack.isAudioAd()) {
                    builder.putLong(METADATA_KEY_DURATION, notificationTrack.getDuration());
                }

                return builder.build();
            }
        };
    }

    private final class TrackAndBitmap {
        private final TrackItem track;
        private final Optional<Bitmap> bitmap;

        private TrackAndBitmap(TrackItem track, Optional<Bitmap> bitmap) {
            this.track = track;
            this.bitmap = bitmap;
        }
    }
}
