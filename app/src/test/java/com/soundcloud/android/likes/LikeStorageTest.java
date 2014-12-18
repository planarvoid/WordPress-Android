package com.soundcloud.android.likes;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
        storage = new LikeStorage(testScheduler(), propeller());
    }

    @Test
    public void shouldLoadTrackLikes() {
        ApiTrack track1 = ModelFixtures.create(ApiTrack.class);
        testFixtures().insertTrackLike(testFixtures().insertTrack(track1), new Date(100));
        ApiTrack track2 = ModelFixtures.create(ApiTrack.class);
        testFixtures().insertTrackLike(testFixtures().insertTrack(track2), new Date(200));

        storage.trackLikes().subscribe(observer);

        PropertySet trackLike1 = PropertySet.from(LikeProperty.TARGET_URN.bind(track1.getUrn()),
                LikeProperty.CREATED_AT.bind(new Date(100)));
        PropertySet trackLike2 = PropertySet.from(LikeProperty.TARGET_URN.bind(track2.getUrn()),
                LikeProperty.CREATED_AT.bind(new Date(200)));

        verify(observer).onNext(eq(trackLike1));
        verify(observer).onNext(eq(trackLike2));

        verify(observer).onCompleted();
    }
}