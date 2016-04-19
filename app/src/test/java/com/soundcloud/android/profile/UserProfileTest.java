package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class UserProfileTest extends AndroidUnitTest {

    @Test
    public void shouldCreateUserProfileFromUserProfileRecord() throws Exception {
        final UserProfileRecord userProfileRecord = new UserProfileRecordFixtures.Builder().populateAllCollections().build();

        UserProfile userProfile = UserProfile.fromUserProfileRecord(userProfileRecord);

        assertThat(userProfile.getUser()).isEqualTo(userProfileRecord.getUser().toPropertySet());

        assertApiEntityHolderSourceMapping(userProfileRecord.getSpotlight(), userProfile.getSpotlight());
        assertApiEntityHolderMapping(userProfileRecord.getTracks(), userProfile.getTracks());
        assertApiEntityHolderMapping(userProfileRecord.getAlbums(), userProfile.getAlbums());
        assertApiEntityHolderMapping(userProfileRecord.getPlaylists(), userProfile.getPlaylists());
        assertApiEntityHolderSourceMapping(userProfileRecord.getReposts(), userProfile.getReposts());
        assertApiEntityHolderSourceMapping(userProfileRecord.getLikes(), userProfile.getLikes());
    }

    private void assertApiEntityHolderMapping(ModelCollection<? extends ApiEntityHolder> sourceCollection,
                                              ModelCollection<PropertySet> actualCollection) {
        assertThat(actualCollection.getCollection().size()).isEqualTo(sourceCollection.getCollection().size());
        assertThat(actualCollection.getCollection().get(0))
                .isEqualTo(sourceCollection.getCollection().get(0).toPropertySet());
    }

    private void assertApiEntityHolderSourceMapping(ModelCollection<? extends ApiEntityHolderSource> sourceCollection,
                                                    ModelCollection<PropertySet> actualCollection) {
        assertThat(actualCollection.getCollection().size()).isEqualTo(sourceCollection.getCollection().size());
        assertThat(actualCollection.getCollection().get(0))
                .isEqualTo(sourceCollection.getCollection().get(0).getEntityHolder().get().toPropertySet());
    }
}