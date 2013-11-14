package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.playback.service.PlayQueueStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueManagerStoreTest {

    private PlayQueueStorage playQueueStorage;
    
    @Before
    public void before() throws Exception {
        playQueueStorage = new PlayQueueStorage();
        List<Playable> likes = TestHelper.readResourceList("/com/soundcloud/android/sync/e1_likes.json");
        expect(TestHelper.bulkInsert(Content.ME_LIKES.uri, likes)).toEqual(3);
    }

    @Test
    public void shouldFindCorrectPositionOfPlayQueueItem() {
        expect(playQueueStorage.getPlayQueuePositionFromUri(Content.ME_LIKES.uri, 56143158l)).toEqual(1);
    }

    @Test
    public void shouldNotFindCorrectPositionOfPlayQueueItem() {
        expect(playQueueStorage.getPlayQueuePositionFromUri(Content.ME_LIKES.uri, 1122L)).toEqual(-1);
    }
}
