package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.absent;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class SearchModelCollectionTest {

    @Test
    public void premiumContentShouldNotBePresent() {
        final SearchModelCollection<ApiTrack> searchResultTracks =
                new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2), Collections.<String, Link>emptyMap());
        final SearchModelCollection<ApiPlaylist> searchResultPlaylists =
                new SearchModelCollection<>(ModelFixtures.create(ApiPlaylist.class, 2), Collections.<String, Link>emptyMap(), "queryUrn", null);
        final SearchModelCollection<ApiUser> searchResultUsers =
                new SearchModelCollection<>(ModelFixtures.create(ApiUser.class, 1));

        assertThat(searchResultTracks.premiumContent()).isEqualTo(absent());
        assertThat(searchResultPlaylists.premiumContent()).isEqualTo(absent());
        assertThat(searchResultUsers.premiumContent()).isEqualTo(absent());
    }

    @Test
    public void premiumContentShouldNotBePresentWhenPremiumItemListIsEmpty() {
        final ModelCollection<ApiTrack> premiumTracks =
                new ModelCollection<>(Collections.<ApiTrack>emptyList());

        final SearchModelCollection<ApiTrack> searchResultTracks =
                new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2), Collections.<String, Link>emptyMap(), "queryUrn", premiumTracks);

        assertThat(searchResultTracks.premiumContent()).isEqualTo(absent());
    }
}
