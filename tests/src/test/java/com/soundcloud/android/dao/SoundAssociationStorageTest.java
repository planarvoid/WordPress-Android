package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationStorageTest {

    private SoundAssociationStorage storage;
    
    @Before
    public void initTest() {
        storage = new SoundAssociationStorage(DefaultTestRunner.application);        
    }
    
    @Test
    public void shouldStoreLikeAndUpdateLikesCount() {
        Track track = new Track(1);
        expect(track.likes_count).not.toBe(1);

        track.likes_count = 1;
        storage.addLike(track);

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(TestHelper.reload(track).likes_count).toBe(1);
    }

    @Test
    public void shouldRemoveLikeAndUpdateLikesCount() {
        Track track = new Track(1);
        track.likes_count = 1;
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(1);
        expect(TestHelper.reload(track).likes_count).toBe(1);

        track.likes_count = 0;
        storage.removeLike(track);

        expect(Content.ME_LIKES).toHaveCount(0);
        expect(TestHelper.reload(track).likes_count).toBe(0);
    }

    @Test
    public void shouldSyncSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                SoundAssociationTest.class.getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(storage.syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect no change, syncing to itself
        expect(storage.syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeFalse();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect change, syncing with 2 items
        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK_REPOST.type));
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK.type));

        expect(storage.syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(2);

        // remove the repost and make sure it gets removed locally
        holder.collection.remove(0);
        expect(storage.syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(1);
    }

    @Test
    public void shouldSyncSoundAssociationsMeLikes() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                SoundAssociationHolder.class);

        expect(storage.syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(3);

        // expect no change, syncing to itself
        expect(storage.syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeFalse();
        expect(Content.ME_LIKES).toHaveCount(3);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(56143158l, SoundAssociation.Type.TRACK_LIKE.type));

        expect(storage.syncToLocal(holder.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(1);
    }

    private SoundAssociation createAssociation(long id, String type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(id);
        soundAssociation1.setType(type);
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        return soundAssociation1;
    }



}
