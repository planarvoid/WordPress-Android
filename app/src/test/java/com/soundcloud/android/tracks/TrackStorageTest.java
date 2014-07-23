package com.soundcloud.android.tracks;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
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

    private PropertySet createTrackPropertySet(final ApiTrack track) throws CreateModelException {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                PlayableProperty.DURATION.bind(track.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(track.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(track.getUser().getUrn()),
                TrackProperty.WAVEFORM_URL.bind(track.getWaveformUrl()),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(track.getStats().getLikesCount()),
                TrackProperty.MONETIZABLE.bind(track.isMonetizable()),
                PlayableProperty.IS_LIKED.bind(false));
    }
}
