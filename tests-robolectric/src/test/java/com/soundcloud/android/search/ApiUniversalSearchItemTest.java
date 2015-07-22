package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiUniversalSearchItemTest {


    @Test
    public void shouldConvertWrappedApiUserToPropertySet() {
        final ApiUser user = ModelFixtures.create(ApiUser.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forUser(user);

        final PropertySet propertySet = searchResult.toPropertySet();

        expect(propertySet.get(UserProperty.URN)).toEqual(user.getUrn());
    }

    @Test
    public void shouldConvertWrappedApiTrackToPropertySet() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forTrack(track);

        final PropertySet propertySet = searchResult.toPropertySet();

        expect(propertySet.get(PlayableProperty.URN)).toEqual(track.getUrn());
    }

    @Test
    public void shouldConvertWrappedApiPlaylistToPropertySet() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiUniversalSearchItem searchResult = ApiUniversalSearchItem.forPlaylist(playlist);

        final PropertySet propertySet = searchResult.toPropertySet();

        expect(propertySet.get(PlayableProperty.URN)).toEqual(playlist.getUrn());
    }

}