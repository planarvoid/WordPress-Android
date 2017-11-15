package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

public class UninterruptedPlaytimeStorageTest extends AndroidUnitTest {

    private static final long PLAYTIME1 = 123L;
    private static final long PLAYTIME2 = 456L;
    private final SharedPreferences preferences = sharedPreferences();

    private UninterruptedPlaytimeStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new UninterruptedPlaytimeStorage(preferences);
    }

    @Test
    public void savesPlayTimeForMediaPlayer() {
        storage.setPlaytime(PLAYTIME1, PlayerType.MediaPlayer.INSTANCE);
        assertThat(storage.getPlayTime(PlayerType.MediaPlayer.INSTANCE)).isEqualTo(PLAYTIME1);
    }

    @Test
    public void savesPlayTimeForSkippy() {
        storage.setPlaytime(PLAYTIME1, PlayerType.Skippy.INSTANCE);
        assertThat(storage.getPlayTime(PlayerType.Skippy.INSTANCE)).isEqualTo(PLAYTIME1);
    }

    @Test
    public void doesNotGetUnrelatedPlayTime() {
        storage.setPlaytime(PLAYTIME1, PlayerType.Skippy.INSTANCE);
        storage.setPlaytime(PLAYTIME2, PlayerType.MediaPlayer.INSTANCE);
        assertThat(storage.getPlayTime(PlayerType.MediaPlayer.INSTANCE)).isEqualTo(PLAYTIME2);
    }

    @Test
    public void getsZeroedPlaytimeAsDefault() {
        assertThat(storage.getPlayTime(PlayerType.MediaPlayer.INSTANCE)).isEqualTo(0L);
    }

}
