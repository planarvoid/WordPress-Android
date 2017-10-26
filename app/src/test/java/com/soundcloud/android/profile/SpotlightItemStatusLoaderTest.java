package com.soundcloud.android.profile;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.PlayableItemStatusLoader;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.TrackFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SpotlightItemStatusLoaderTest {

    @Mock private PlayableItemStatusLoader playableItemStatusLoader;
    private SpotlightItemStatusLoader subject;

    @Before
    public void setUp() throws Exception {
        subject = new SpotlightItemStatusLoader(playableItemStatusLoader);
    }

    @Test
    public void shouldUpdateSpotlightItems() throws Exception {
        ModelCollection<PlayableItem> spotlight = new ModelCollection<>(
                singletonList(TrackFixtures.trackItem()));
        UserProfile userProfile = new UserProfileFixtures.Builder().spotlight(spotlight).build();
        subject.call(userProfile);

        verify(playableItemStatusLoader).call(spotlight.getCollection());
    }
}
