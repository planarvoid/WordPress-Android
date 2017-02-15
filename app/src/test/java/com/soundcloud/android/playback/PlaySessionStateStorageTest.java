package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.DURATION;
import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.ITEM;
import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.PLAY_ID;
import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

public class PlaySessionStateStorageTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123);
    private SharedPreferences prefs = sharedPreferences();

    private PlaySessionStateStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlaySessionStateStorage(prefs);
    }

    @Test
    public void savePlayInfoStoresCurrentInfoInPrefs() {
        storage.savePlayInfo(TRACK);

        assertThat(prefs.getString(ITEM.name(), Strings.EMPTY)).isEqualTo(TRACK.toString());
    }

    @Test
    public void savePlayIdStoresPlayIdInPrefs() {
        storage.savePlayId("play-id");

        assertThat(prefs.getString(PLAY_ID.name(), Strings.EMPTY)).isEqualTo("play-id");
    }

    @Test
    public void savePlayInfoClearsPlayId() {
        storage.savePlayId("play-id");
        storage.savePlayInfo(TRACK);

        assertThat(prefs.contains(PLAY_ID.name())).isFalse();
    }

    @Test
    public void saveProgressStoresProgressInPrefs() {
        storage.saveProgress(123, 456);

        assertThat(prefs.getLong(PlaySessionStateStorage.Keys.PROGRESS.name(), 0)).isEqualTo(123L);
        assertThat(prefs.getLong(PlaySessionStateStorage.Keys.DURATION.name(), 0)).isEqualTo(456L);
    }

    @Test
    public void getLastPlayingItemReturnsNotSet() {
        assertThat(storage.getLastPlayingItem()).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void getLastPlayingItemReturnsLastPlayingItemFromPrefs() {
        prefs.edit().putString(ITEM.name(), TRACK.toString()).apply();

        assertThat(storage.getLastPlayingItem()).isEqualTo(TRACK);
    }

    @Test
    public void getLastProgressReturnsLastProgressFromPrefs() {
        prefs.edit().putLong(PROGRESS.name(), 456L).apply();

        assertThat(storage.getLastStoredProgress()).isEqualTo(456);
    }

    @Test
    public void getLastDurationReturnsLastDurationFromPrefs() {
        prefs.edit().putLong(DURATION.name(), 987L).apply();

        assertThat(storage.getLastStoredDuration()).isEqualTo(987);
    }

    @Test
    public void getLastProgressReturnsZeroIfNotStored() {
        assertThat(storage.getLastStoredProgress()).isEqualTo(0);
    }
}
