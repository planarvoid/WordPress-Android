package com.soundcloud.android.dao;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationDAOTest extends BaseDAOTest<SoundAssociationDAO> {

    public SoundAssociationDAOTest() {
        super(new SoundAssociationDAO(Robolectric.application.getContentResolver()));
    }

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(100L);
    }

    @Test
    public void shouldSyncSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                SoundAssociationTest.class.getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(getDAO().syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect no change, syncing to itself
        expect(getDAO().syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeFalse();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect change, syncing with 2 items
        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK_REPOST.type));
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK.type));

        expect(getDAO().syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(2);

        // remove the repost and make sure it gets removed locally
        holder.collection.remove(0);
        expect(getDAO().syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(1);
    }

    @Test
    public void shouldSyncSoundAssociationsMeLikes() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                SoundAssociationHolder.class);

        expect(getDAO().syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(3);

        // expect no change, syncing to itself
        expect(getDAO().syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeFalse();
        expect(Content.ME_LIKES).toHaveCount(3);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(56143158l, SoundAssociation.Type.TRACK_LIKE.type));

        expect(getDAO().syncToLocal(holder.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(1);
    }

    /*
    @Test
    public void shouldPersistStreamItems() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        final ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        SoundAssociationHolder sounds  = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds_with_sets.json"),
                SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();

        expect(sounds.size()).toEqual(41);


        expect(manager.writeCollection(sounds, ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks, 3 sets


        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                Content.ME_SOUNDS.uri, 0, 50).size()).toEqual(41);

        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);

        expect(newItems.size()).toEqual(41);

        expect(Content.ME_PLAYLISTS).toHaveCount(2); // does not include the repost

    }


    @Test
    public void shouldInsertNewSoundAssociation() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        //initial population
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(manager.writeCollection(old, ScResource.CacheUpdateMode.NONE)).toEqual(38); // 38 tracks and 3 diff users

        Playlist p = manager.getModelFromStream(SyncAdapterServiceTest.class.getResourceAsStream("playlist.json"));
        SoundAssociation soundAssociation1 = new SoundAssociation(p, new Date(System.currentTimeMillis()),SoundAssociation.Type.PLAYLIST);

        final Uri uri = soundAssociation1.insert(DefaultTestRunner.application.getContentResolver(),Content.ME_SOUNDS.uri);
        expect(uri).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/sounds/39"));
    }
    */

    private SoundAssociation createAssociation(long id, String type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(id);
        soundAssociation1.setType(type);
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        return soundAssociation1;
    }
}
