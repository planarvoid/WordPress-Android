package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.Cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueManagerStorageTest {

    private PlayQueueManagerStore storage;
    
    @Before
    public void initTest() {
        storage = new PlayQueueManagerStore(DefaultTestRunner.application);
    }

    @Test
    public void shouldFindCorrectPositionOfPlayQueueItem() throws Exception {
        List<Playable> likes = TestHelper.readResourceList("/com/soundcloud/android/service/sync/e1_likes.json");
        expect(TestHelper.bulkInsert(Content.ME_LIKES.uri, likes)).toEqual(3);
        expect(storage.getPlayQueuePositionFromUri(Content.ME_LIKES.uri, 56143158l)).toEqual(1);
    }
}
