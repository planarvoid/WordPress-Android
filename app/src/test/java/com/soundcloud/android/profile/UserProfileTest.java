package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import org.junit.Test;

public class UserProfileTest extends AndroidUnitTest {

    @Test
    public void shouldCreateUserProfileFromApiUserProfile() throws Exception {
        final ApiUserProfile apiUserProfile = new UserProfileRecordFixtures.Builder().populateAllCollections()
                                                                                           .build();

        UserProfile userProfile = UserProfile.fromUserProfileRecord(apiUserProfile);

        assertThat(userProfile.getUser()).isEqualTo(UserItem.from(apiUserProfile.getUser()));

        assertApiTrackHolderSourceMapping(apiUserProfile.getSpotlight(), userProfile.getSpotlight());
        assertApiTrackHolderMapping(apiUserProfile.getTracks(), userProfile.getTracks());
        assertApiPlaylistHolderMapping(apiUserProfile.getAlbums(), userProfile.getAlbums());
        assertApiPlaylistHolderMapping(apiUserProfile.getPlaylists(), userProfile.getPlaylists());
        assertApiTrackHolderSourceMapping(apiUserProfile.getReposts(), userProfile.getReposts());
        assertApiTrackHolderSourceMapping(apiUserProfile.getLikes(), userProfile.getLikes());
    }

    private void assertApiTrackHolderMapping(ModelCollection<ApiTrackPost> sourceCollection,
                                       ModelCollection<TrackItem> actualCollection) {
        assertThat(actualCollection.getCollection().size()).isEqualTo(sourceCollection.getCollection().size());
        assertThat(actualCollection.getCollection().get(0))
                .isEqualTo(TrackItem.from(sourceCollection.getCollection().get(0).getApiTrack()));
    }

    private void assertApiPlaylistHolderMapping(ModelCollection<ApiPlaylistPost> sourceCollection,
                                       ModelCollection<PlaylistItem> actualCollection) {
        assertThat(actualCollection.getCollection().size()).isEqualTo(sourceCollection.getCollection().size());
        assertThat(actualCollection.getCollection().get(0))
                .isEqualTo(PlaylistItem.from(sourceCollection.getCollection().get(0).getApiPlaylist()));
    }

    private void assertApiTrackHolderSourceMapping(ModelCollection<ApiPlayableSource> sourceCollection, ModelCollection<PlayableItem> actualCollection) {
        assertThat(actualCollection.getCollection().size()).isEqualTo(sourceCollection.getCollection().size());
        assertThat(actualCollection.getCollection().get(0)).isInstanceOf(TrackItem.class);
        assertThat(actualCollection.getCollection().get(0)).isEqualTo(TrackItem.from(sourceCollection.getCollection().get(0).getTrack().get()));
    }
}
