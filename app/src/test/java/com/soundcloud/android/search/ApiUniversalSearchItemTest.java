package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class ApiUniversalSearchItemTest extends AndroidUnitTest {

    @Test
    public void shouldConvertWrappedApiUserToPropertySet() {
        final ApiUser user = ModelFixtures.create(ApiUser.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forUser(user);

        final PropertySet propertySet = searchResult.toPropertySet();

        assertThat(propertySet.get(UserProperty.URN)).isEqualTo(user.getUrn());
    }

    @Test
    public void shouldConvertWrappedApiTrackToPropertySet() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forTrack(track);

        final PropertySet propertySet = searchResult.toPropertySet();

        assertThat(propertySet.get(PlayableProperty.URN)).isEqualTo(track.getUrn());
    }

    @Test
    public void shouldConvertWrappedApiPlaylistToPropertySet() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forPlaylist(playlist);

        final PropertySet propertySet = searchResult.toPropertySet();

        assertThat(propertySet.get(PlayableProperty.URN)).isEqualTo(playlist.getUrn());
    }

}
