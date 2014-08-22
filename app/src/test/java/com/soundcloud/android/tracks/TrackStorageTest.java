package com.soundcloud.android.tracks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class TrackStorageTest extends StorageIntegrationTest {

    private TrackStorage storage;

    @Mock
    private Observer<PropertySet> observer;

    @Before
    public void setup() {
        storage = new TrackStorage(testScheduler());
    }

    @Test
    public void trackByUrnEmitsInsertedTrack() throws CreateModelException {

        final ApiTrack track = testHelper().insertTrack();
        storage.track(track.getUrn(), Urn.forUser(123)).subscribe(observer);
        final PropertySet trackPropertySet = createTrackPropertySet(track);

        verify(observer).onNext(trackPropertySet);
        verify(observer).onCompleted();
    }

    @Test
    public void trackByUrnDoesNotEmitInsertedTrackWithoutTitle() throws CreateModelException {
        ApiTrack track = TestHelper.getModelFactory().createModel(ApiTrack.class);
        track.setTitle(null);
        testHelper().insertTrack(track);

        storage.track(track.getUrn(), Urn.forUser(123)).subscribe(observer);

        verify(observer, never()).onNext(any(PropertySet.class));
        verify(observer).onCompleted();
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() throws CreateModelException {
        final TrackUrn trackUrn = Urn.forTrack(123);
        testHelper().insertDescription(trackUrn, "description123");
        storage.trackDetails(trackUrn).subscribe(observer);
        verify(observer).onNext(eq(PropertySet.from(TrackProperty.DESCRIPTION.bind("description123"))));
        verify(observer).onCompleted();
    }

    private PropertySet createTrackPropertySet(final ApiTrack track) {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                PlayableProperty.DURATION.bind(track.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(track.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(track.getUser().getUrn()),
                TrackProperty.WAVEFORM_URL.bind(track.getWaveformUrl()),
                TrackProperty.STREAM_URL.bind(track.getStreamUrl()),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()),
                TrackProperty.COMMENTS_COUNT.bind(track.getStats().getCommentsCount()),
                PlayableProperty.LIKES_COUNT.bind(track.getStats().getLikesCount()),
                PlayableProperty.REPOSTS_COUNT.bind(track.getStats().getRepostsCount()),
                TrackProperty.MONETIZABLE.bind(track.isMonetizable()),
                TrackProperty.POLICY.bind(track.getPolicy()),
                PlayableProperty.IS_LIKED.bind(false),
                PlayableProperty.PERMALINK_URL.bind(track.getPermalinkUrl()),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.CREATED_AT.bind(track.getCreatedAt()),
                PlayableProperty.IS_REPOSTED.bind(false));
    }
}
