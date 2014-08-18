package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    // there are still cases where we do not have these strings, so check before adding them
    // Need to fix this as part of https://github.com/soundcloud/SoundCloud-Android/issues/2054
    @Test
    public void shouldNotCrashIfEssentialFieldsLikeTitleAreMissing() throws CreateModelException {
        ApiTrack trackWithoutTitle = TestHelper.getModelFactory().createModel(ApiTrack.class);
        trackWithoutTitle.setTitle(null);
        testHelper().insertTrack(trackWithoutTitle);

        storage.track(trackWithoutTitle.getUrn(), Urn.forUser(123)).subscribe(observer);

        ArgumentCaptor<PropertySet> captor = ArgumentCaptor.forClass(PropertySet.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().get(PlayableProperty.TITLE)).toEqual(ScTextUtils.EMPTY_STRING);
    }

    private PropertySet createTrackPropertySet(final ApiTrack track) throws CreateModelException {
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
