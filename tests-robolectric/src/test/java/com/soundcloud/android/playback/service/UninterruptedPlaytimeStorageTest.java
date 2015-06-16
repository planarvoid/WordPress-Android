package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UninterruptedPlaytimeStorageTest {

    public static final long PLAYTIME1 = 123L;
    public static final long PLAYTIME2 = 456L;
    private UninterruptedPlaytimeStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new UninterruptedPlaytimeStorage(new ScTestSharedPreferences());
    }

    @Test
    public void savesPlayTimeForMediaPlayer() {
        storage.setPlaytime(PLAYTIME1, PlayerType.MEDIA_PLAYER);
        expect(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).toEqual(PLAYTIME1);
    }

    @Test
    public void savesPlayTimeForSkippy() {
        storage.setPlaytime(PLAYTIME1, PlayerType.SKIPPY);
        expect(storage.getPlayTime(PlayerType.SKIPPY)).toEqual(PLAYTIME1);
    }

    @Test
    public void doesNotGetUnrelatedPlayTime() {
        storage.setPlaytime(PLAYTIME1, PlayerType.SKIPPY);
        storage.setPlaytime(PLAYTIME2, PlayerType.MEDIA_PLAYER);
        expect(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).toEqual(PLAYTIME2);
    }

    @Test
    public void getsZeroedPlaytimeAsDefault() {
        expect(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).toEqual(0L);
    }

}