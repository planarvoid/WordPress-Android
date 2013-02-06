package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationTest {

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {

        SoundAssociation soundItem = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sound_item.json"),
                SoundAssociation.class);

        expect(soundItem.associationType).not.toBeNull();
        expect(soundItem.created_at).not.toBeNull();
        expect(soundItem.playable).not.toBeNull();

        Parcel p = Parcel.obtain();
        soundItem.writeToParcel(p, 0);

        SoundAssociation soundItem1 = new SoundAssociation(p);
        compareSoundItems(soundItem, soundItem1);
    }

    @Test
    public void shouldPersistStreamItems() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        final ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        SoundAssociationHolder sounds  = TestHelper.getObjectMapper().readValue(
                        getClass().getResourceAsStream("sounds_with_sets.json"),
                        SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();

        expect(sounds.size()).toEqual(41);

        expect(manager.writeCollection(sounds,
                ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks, 3 sets

        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                Content.ME_SOUNDS.uri,0,50).size()).toEqual(41);

        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);

        expect(newItems.size()).toEqual(41);

        expect(Content.ME_PLAYLISTS).toHaveCount(3);

    }

    @Test
    public void shouldProvideUniqueListItemId() throws Exception {

        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(123l);
        soundAssociation1.setType(SoundAssociation.Type.TRACK.type);

        SoundAssociation soundAssociation2 = new SoundAssociation();
        soundAssociation2.playable = soundAssociation1.playable;
        soundAssociation2.setType(SoundAssociation.Type.TRACK_REPOST.type);

        expect(soundAssociation1.getListItemId()).not.toEqual(soundAssociation2.getListItemId());
    }

    @Test
    public void shouldInsertNewSoundAssociation() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        //initial population
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(manager.writeCollection(old, ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks and 3 diff users

        Playlist p = manager.getModelFromStream(SyncAdapterServiceTest.class.getResourceAsStream("playlist.json"));
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playlist = p;
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        soundAssociation1.setType(SoundAssociation.Type.PLAYLIST.toString());

        final Uri uri = soundAssociation1.insert(DefaultTestRunner.application.getContentResolver(),Content.ME_SOUNDS.uri);
        expect(uri).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/sounds/39"));
    }


    private void compareSoundItems(SoundAssociation soundItem, SoundAssociation soundItem2) {
        expect(soundItem2.id).toEqual(soundItem.id);
        expect(soundItem2.created_at).toEqual(soundItem.created_at);
        expect(soundItem2.associationType).toEqual(soundItem.associationType);
        expect(soundItem2.playable).toEqual(soundItem.playable);
    }



}
