package com.soundcloud.android.playback.mediasession;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

public class MetadataOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123);
    private static final Urn VIDEO_URN = Urn.forAd("dfp", "video");
    private static final TrackItem TRACK = ModelFixtures.trackItem();
    private static final Optional<MediaMetadataCompat> EMPTY_METADATA = Optional.absent();
    private static final String ADVERTISING_TITLE = "Advertisement";
    private static final String OLD_TITLE = "old title";

    @Mock TrackItemRepository trackRepository;
    @Mock ImageOperations imageOperations;
    @Mock Bitmap adBitmap;
    @Mock Bitmap bitmap;
    @Mock Bitmap oldBitmap;

    private MetadataOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new MetadataOperations(context().getResources(), trackRepository,
                                            imageOperations, Schedulers.trampoline());

        when(trackRepository.track(TRACK_URN)).thenReturn(Maybe.just(TRACK));
        when(imageOperations.artwork(eq(SimpleImageResource.create(TRACK)),
                                     any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(rx.Observable.just(bitmap));
        when(imageOperations.decodeResource(context().getResources(), R.drawable.notification_loading))
                .thenReturn(adBitmap);
    }

    @Test
    public void metadataShouldOnlyEmitOnceWhenCachedBitmapIsAvailable() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);

        operations.metadata(TRACK_URN, false, EMPTY_METADATA)
                  .test()
                  .assertValueCount(1)
                  .assertComplete();
    }

    @Test
    public void metadataShouldEmitCachedBitmapWhenAvailable() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);

        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(TRACK_URN, false, EMPTY_METADATA).test();

        assertThat(getBitmap(testObserver, 0)).isEqualTo(oldBitmap);
    }

    @Test
    public void metadataShouldNotEmitCachedBitmapWhenIsRecycled() throws Exception {
        setCachedBitmap(TRACK, oldBitmap);
        when(oldBitmap.isRecycled()).thenReturn(true);

        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(TRACK_URN, false, EMPTY_METADATA).test();

        assertThat(getBitmap(testObserver, 0)).isNotEqualTo(oldBitmap);
        assertThat(getBitmap(testObserver, 1)).isEqualTo(bitmap);
    }

    @Test
    public void metadataEmitsTwiceWhenIsNotCached() throws Exception {
        operations.metadata(TRACK_URN, false, EMPTY_METADATA)
                  .test()
                  .assertValueCount(2)
                  .assertComplete();
    }

    @Test
    public void metadataEmitsMediaMetadataWithExistingArtworkOnlyOnFirstEmit() throws Exception {
        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(TRACK_URN, false, Optional.of(previousMetadata())).test();

        assertThat(getBitmap(testObserver, 0)).isEqualTo(oldBitmap);
        assertThat(getTitle(testObserver, 0)).isNotEqualTo("old title");
    }

    @Test
    public void metadataEmitsMediaMetadataWithNewArtworkOnSecondEmit() throws Exception {
        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(TRACK_URN, false, Optional.of(previousMetadata())).test();

        assertThat(getBitmap(testObserver, 1)).isEqualTo(bitmap);
        assertThat(getTitle(testObserver, 1)).isEqualTo(TRACK.title());
    }

    @Test
    public void metadataEmitsOnlyOnceWhenIsAVideoAd() throws Exception {
        operations.metadata(VIDEO_URN, true, EMPTY_METADATA)
                  .test()
                  .assertValueCount(1)
                  .assertComplete();
    }

    @Test
    public void metadataEmitsAdMetadataWhenIsAVideoAd() throws Exception {
        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(VIDEO_URN, true, EMPTY_METADATA).test();

        assertThat(getTitle(testObserver, 0)).isEqualTo(ADVERTISING_TITLE);
        assertThat(getBitmap(testObserver, 0)).isEqualTo(adBitmap);
    }

    @Test
    public void metadataDoesNotLoadTrackWhenIsVideoAd() throws Exception {
        operations.metadata(VIDEO_URN, true, EMPTY_METADATA).test();

        verify(trackRepository, never()).track(VIDEO_URN);
    }

    @Test
    public void metadataEmitsAdMetadataWhenIsAnAudioAd() throws Exception {
        TestObserver<MediaMetadataCompat> testObserver = operations.metadata(TRACK_URN, true, EMPTY_METADATA).test();

        assertThat(getTitle(testObserver, 1)).isEqualTo(ADVERTISING_TITLE);
        assertThat(getBitmap(testObserver, 1)).isEqualTo(adBitmap);
    }

    @Test
    public void metadataLoadsTrackWhenIsAnAudioAd() throws Exception {
        operations.metadata(TRACK_URN, true, EMPTY_METADATA).test();

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

    private MediaMetadataCompat getMetadata(TestObserver<MediaMetadataCompat> observer, int position) {
        return observer.values().get(position);
    }

    private Bitmap getBitmap(TestObserver<MediaMetadataCompat> observer, int position) {
        return getMetadata(observer, position).getBitmap(METADATA_KEY_ART);
    }

    private String getTitle(TestObserver<MediaMetadataCompat> observer, int position) {
        return getMetadata(observer, position).getString(METADATA_KEY_TITLE);
    }

    private MediaMetadataCompat previousMetadata() {
        return new MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_TITLE, OLD_TITLE)
                .putBitmap(METADATA_KEY_ART, oldBitmap)
                .build();
    }

}
