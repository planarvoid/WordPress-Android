package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationTest {

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {

        SoundAssociation soundItem = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("sound_item.json"),
                SoundAssociation.class);

        expect(soundItem.associationType).not.toBeNull();
        expect(soundItem.created_at).not.toBeNull();
        expect(soundItem.track).not.toBeNull();

        Parcel p = Parcel.obtain();
        soundItem.writeToParcel(p, 0);

        SoundAssociation soundItem1 = new SoundAssociation(p);
        compareSoundItems(soundItem, soundItem1);
    }

    @Test
    public void shouldPersistStreamItems() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        final ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        SoundAssociationHolder sounds  = AndroidCloudAPI.Mapper.readValue(
                        getClass().getResourceAsStream("sounds_with_sets.json"),
                        SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();

        expect(sounds.size()).toEqual(41);

        expect(manager.writeCollection(sounds,
                ScResource.CacheUpdateMode.NONE)).toEqual(44); // 38 tracks, 3 sets and 3 diff users

        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                Content.ME_SOUNDS.uri,0,50).size()).toEqual(41);

        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);

        expect(newItems.size()).toEqual(41);
    }

    @Test
    public void shouldProvideUniqueListItemId() throws Exception {

        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.track = new Track(123l);
        soundAssociation1.setType(SoundAssociation.Type.TRACK.type);

        SoundAssociation soundAssociation2 = new SoundAssociation();
        soundAssociation2.track = soundAssociation1.track;
        soundAssociation2.setType(SoundAssociation.Type.TRACK_REPOST.type);

        expect(soundAssociation1.getListItemId()).not.toEqual(soundAssociation2.getListItemId());
    }


    private void compareSoundItems(SoundAssociation soundItem, SoundAssociation soundItem2) {
        expect(soundItem2.id).toEqual(soundItem.id);
        expect(soundItem2.created_at).toEqual(soundItem.created_at);
        expect(soundItem2.associationType).toEqual(soundItem.associationType);
        expect(soundItem2.track).toEqual(soundItem.track);
    }
}
