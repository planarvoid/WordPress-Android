package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class UserStorageTest {

    @Test
    @Ignore
    public void shouldSyncSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder associations = TestHelper.getObjectMapper().readValue(
                SoundAssociationTest.class.getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        TestHelper.bulkInsert(Content.ME_SOUNDS.uri,associations.collection);
        expect(Content.ME_SOUNDS).toHaveCount(38);

        new UserStorage(DefaultTestRunner.application).removeAssociationsFromLoggedInUser();
        expect(Content.ME_SOUNDS).toHaveCount(0);
    }
}
