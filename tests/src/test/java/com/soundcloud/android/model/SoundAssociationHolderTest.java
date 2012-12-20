package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationHolderTest {

    private ScModelManager manager;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(100L);
        manager = DefaultTestRunner.application.MODEL_MANAGER;
    }

    @Test
    public void shouldRemoveMissingSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder old = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(manager.writeCollection(old,
                        ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks and 3 diff users

        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                        Content.ME_SOUNDS.uri, 0, 50).size()).toEqual(38);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK_REPOST.type));
        holder.collection.add(createAssociation(62633570l, SoundAssociation.Type.TRACK.type));

        expect(holder.removeMissingLocallyStoredItems(DefaultTestRunner.application.getContentResolver(), Content.ME_SOUNDS.uri)).toEqual(36);
        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                                Content.ME_SOUNDS.uri, 0, 50).size()).toEqual(2);
    }

    @Test
        public void shouldRemoveMissingSoundAssociationsMeLikes() throws Exception {
            SoundAssociationHolder old = AndroidCloudAPI.Mapper.readValue(
                    ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                    SoundAssociationHolder.class);

            expect(manager.writeCollection(old,
                            ScResource.CacheUpdateMode.NONE)).toEqual(4); // 4 tracks (2 from a playlist)

            expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                            Content.ME_LIKES.uri, 0, 50).size()).toEqual(2); // 2 tracks, ignoring playlist

            SoundAssociationHolder holder = new SoundAssociationHolder();
            holder.collection = new ArrayList<SoundAssociation>();
            holder.collection.add(createAssociation(56143158l, SoundAssociation.Type.TRACK.type));

            expect(holder.removeMissingLocallyStoredItems(DefaultTestRunner.application.getContentResolver(), Content.ME_LIKES.uri)).toEqual(1);
            expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                                    Content.ME_LIKES.uri, 0, 50).size()).toEqual(1);
        }


    private SoundAssociation createAssociation(long id, String type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.track = new Track(id);
        soundAssociation1.setType(type);
        return soundAssociation1;
    }
}
