package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.PlayableItemStatusLoader;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SpotlightItemStatusLoaderTest extends AndroidUnitTest {

    @Mock private PlayableItemStatusLoader playableItemStatusLoader;
    private SpotlightItemStatusLoader subject;

    @Before
    public void setUp() throws Exception {
        subject = new SpotlightItemStatusLoader(playableItemStatusLoader);
    }

    @Test
    public void shouldUpdateSpotlightItems() throws Exception {
        ModelCollection<PropertySet> spotlight = new ModelCollection<>(
                singletonList(create(ApiTrack.class).toPropertySet()));
        UserProfile userProfile = new UserProfileFixtures.Builder().spotlight(spotlight).build();
        subject.call(userProfile);

        verify(playableItemStatusLoader).call(spotlight.getCollection());
    }
}