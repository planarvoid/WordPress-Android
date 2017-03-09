package com.soundcloud.android.playback.mediasession;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

public class MetadataOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123);
    private static final Urn VIDEO_URN = Urn.forAd("dfp", "video");
    private static final TrackItem TRACK = ModelFixtures.trackItem();
    private static final Optional<MediaMetadataCompat> EMPTY_METADATA = Optional.absent();
    private static final String ADVERTISING_TITLE = "Advertisement";
    private static final String OLD_TITLE = "old title";

    @Mock Resources resources;
    @Mock TrackItemRepository trackRepository;
    @Mock ImageOperations imageOperations;
    @Mock Bitmap adBitmap;
    @Mock Bitmap bitmap;
    @Mock Bitmap oldBitmap;

    private TestSubscriber<MediaMetadataCompat> subscriber = new TestSubscriber<>();
    private MetadataOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new MetadataOperations(context().getResources(), trackRepository,
                                            imageOperations, Schedulers.immediate());

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(TRACK));
        when(imageOperations.artwork(eq(SimpleImageResource.create(TRACK)),
                                     any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));
        when(imageOperations.decodeResource(context().getResources(), R.drawable.notification_loading))
                .thenReturn(adBitmap);
    }

    @Test
    public void metadataShouldOnlyEmitOnceWhenCachedBitmapIsAvailable() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);

        operations.metadata(TRACK_URN, false, EMPTY_METADATA).subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void metadataShouldEmitCachedBitmapWhenAvailable() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);

        operations.metadata(TRACK_URN, false, EMPTY_METADATA).subscribe(subscriber);

        assertThat(getBitmap(0)).isEqualTo(oldBitmap);
    }

    @Test
    public void metadataShouldNotEmitCachedBitmapWhenIsRecycled() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);
        when(oldBitmap.isRecycled()).thenReturn(true);

        operations.metadata(TRACK_URN, false, EMPTY_METADATA).subscribe(subscriber);

        assertThat(getBitmap(0)).isNotEqualTo(oldBitmap);
        assertThat(getBitmap(1)).isEqualTo(bitmap);
    }

    @Test
    public void metadataEmitsTwiceWhenIsNotCached() throws Exception {
        operations.metadata(TRACK_URN, false, EMPTY_METADATA).subscribe(subscriber);

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void metadataEmitsMediaMetadataWithExistingArtworkOnlyOnFirstEmit() throws Exception {
        operations.metadata(TRACK_URN, false, Optional.of(previousMetadata())).subscribe(subscriber);

        assertThat(getBitmap(0)).isEqualTo(oldBitmap);
        assertThat(getTitle(0)).isNotEqualTo("old title");
    }

    @Test
    public void metadataEmitsMediaMetadataWithNewArtworkOnSecondEmit() throws Exception {
        operations.metadata(TRACK_URN, false, Optional.of(previousMetadata())).subscribe(subscriber);

        assertThat(getBitmap(1)).isEqualTo(bitmap);
        assertThat(getTitle(1)).isEqualTo(TRACK.title());
    }

    @Test
    public void metadataEmitsOnlyOnceWhenIsAVideoAd() throws Exception {
        operations.metadata(VIDEO_URN, true, EMPTY_METADATA).subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void metadataEmitsAdMetadataWhenIsAVideoAd() throws Exception {
        operations.metadata(VIDEO_URN, true, EMPTY_METADATA).subscribe(subscriber);

        assertThat(getTitle(0)).isEqualTo(ADVERTISING_TITLE);
        assertThat(getBitmap(0)).isEqualTo(adBitmap);
    }

    @Test
    public void metadataDoesNotLoadTrackWhenIsVideoAd() throws Exception {
        operations.metadata(VIDEO_URN, true, EMPTY_METADATA).subscribe(subscriber);

        verify(trackRepository, never()).track(VIDEO_URN);
    }

    @Test
    public void metadataEmitsAdMetadataWhenIsAnAudioAd() throws Exception {
        operations.metadata(TRACK_URN, true, EMPTY_METADATA).subscribe(subscriber);

        assertThat(getTitle(1)).isEqualTo(ADVERTISING_TITLE);
        assertThat(getBitmap(1)).isEqualTo(adBitmap);
    }

    @Test
    public void metadataLoadsTrackWhenIsAnAudioAd() throws Exception {
        operations.metadata(TRACK_URN, true, EMPTY_METADATA).subscribe(subscriber);

        verify(trackRepository).track(TRACK_URN);
    }

    @Test
    public void preloadWarmsUpTheCache() {
        operations.preload(TRACK_URN);

        verify(imageOperations).artwork(eq(SimpleImageResource.create(TRACK)),
                                        any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void preloadDoesNotWarmUpWhenAlreadyCached() {
        setCachedBitmap(TRACK, oldBitmap);

        operations.preload(TRACK_URN);

        verify(imageOperations, never()).artwork(eq(SimpleImageResource.create(TRACK)),
                                                 any(ApiImageSize.class), anyInt(), anyInt());
    }

    private void setCachedBitmap(TrackItem track, Bitmap bitmap) {
        when(imageOperations.getCachedBitmap(eq(SimpleImageResource.create(track)),
                                             any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(bitmap);
    }

    private MediaMetadataCompat getMetadata(int position) {
        return subscriber.getOnNextEvents().get(position);
    }

    private Bitmap getBitmap(int position) {
        return getMetadata(position).getBitmap(METADATA_KEY_ART);
    }

    private String getTitle(int position) {
        return getMetadata(position).getString(METADATA_KEY_TITLE);
    }

    private MediaMetadataCompat previousMetadata() {
        return new MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_TITLE, OLD_TITLE)
                .putBitmap(METADATA_KEY_ART, oldBitmap)
                .build();
    }

}
