package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.SharedPreferences;

public class UninterruptedPlaytimeStorageTest extends AndroidUnitTest {

    public static final long PLAYTIME1 = 123L;
    public static final long PLAYTIME2 = 456L;
    private final SharedPreferences preferences = sharedPreferences("test", Context.MODE_PRIVATE);

    private UninterruptedPlaytimeStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new UninterruptedPlaytimeStorage(preferences);
    }

    @Test
    public void savesPlayTimeForMediaPlayer() {
        storage.setPlaytime(PLAYTIME1, PlayerType.MEDIA_PLAYER);
        assertThat(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).isEqualTo(PLAYTIME1);
    }

    @Test
    public void savesPlayTimeForSkippy() {
        storage.setPlaytime(PLAYTIME1, PlayerType.SKIPPY);
        assertThat(storage.getPlayTime(PlayerType.SKIPPY)).isEqualTo(PLAYTIME1);
    }

    @Test
    public void doesNotGetUnrelatedPlayTime() {
        storage.setPlaytime(PLAYTIME1, PlayerType.SKIPPY);
        storage.setPlaytime(PLAYTIME2, PlayerType.MEDIA_PLAYER);
        assertThat(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).isEqualTo(PLAYTIME2);
    }

    @Test
    public void getsZeroedPlaytimeAsDefault() {
        assertThat(storage.getPlayTime(PlayerType.MEDIA_PLAYER)).isEqualTo(0L);
    }

}
