package com.soundcloud.android.likes;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LikeStorageTest extends StorageIntegrationTest {

    private LikeStorage storage;

    @Mock private Observer<PropertySet> observer;

    @Before
    public void setUp() {
        storage = new LikeStorage(testScheduler());
    }

    @Test
    public void shouldLoadTrackLikesFromObservable() {
        ApiTrack track1 = testFixtures().insertLikedTrack(new Date(100));
        ApiTrack track2 = testFixtures().insertLikedTrack(new Date(200));

        storage.trackLikes().subscribe(observer);

        verify(observer).onNext(eq(expectedLikeFor(track1.getUrn(), new Date(100))));
        verify(observer).onNext(eq(expectedLikeFor(track2.getUrn(), new Date(200))));

        verify(observer).onCompleted();
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt) {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(urn),
                LikeProperty.CREATED_AT.bind(createdAt));
    }

}