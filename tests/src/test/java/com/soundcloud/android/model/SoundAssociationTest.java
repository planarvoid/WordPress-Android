package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.os.Parcel;

import java.util.List;

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
                        getClass().getResourceAsStream("sounds.json"),
                        SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();
        expect(sounds.size()).toEqual(38);

        expect((int) manager.writeCollection(sounds.collection,
                Content.ME_SOUNDS.uri,
                100l,
                ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks and 3 diff users

        /*
        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);
                */

    }

    private void compareSoundItems(SoundAssociation soundItem, SoundAssociation soundItem2) {
        expect(soundItem2.id).toEqual(soundItem.id);
        expect(soundItem2.created_at).toEqual(soundItem.created_at);
        expect(soundItem2.associationType).toEqual(soundItem.associationType);
        expect(soundItem2.track).toEqual(soundItem.track);
    }
}
