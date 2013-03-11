package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationHolderTest {

    private ScModelManager manager;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(100L);
        manager = DefaultTestRunner.application.MODEL_MANAGER;
    }

    @Test
    public void shouldSyncSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect no change, syncing to itself
        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toBeFalse();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK_REPOST.type));
        holder.collection.add(createAssociation(62633570l, SoundAssociation.Type.TRACK.type));

        expect(holder.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(2);
    }

    @Test
    public void shouldSyncSoundAssociationsMeLikes() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                SoundAssociationHolder.class);

        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(3);

        // expect no change, syncing to itself
        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_LIKES.uri)).toBeFalse();
        expect(Content.ME_LIKES).toHaveCount(3);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(56143158l, SoundAssociation.Type.TRACK_LIKE.type));

        expect(holder.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(1);
    }

    @Test
    public void shouldDetectChangeOnDuplicateSoundRemoval() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        old.collection.remove(0); // a repost of a previous sound, remove it to check change logic
        expect(old.syncToLocal(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(37);
    }

    private SoundAssociation createAssociation(long id, String type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(id);
        soundAssociation1.setType(type);
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        return soundAssociation1;
    }
}
