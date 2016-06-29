package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.ITEM;
import static com.soundcloud.android.playback.PlaySessionStateStorage.Keys.PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.fakes.RoboSharedPreferences;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class PlaySessionStateStorageTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123);
    private RoboSharedPreferences prefs = new RoboSharedPreferences(new HashMap<String, Map<String, Object>>(),
                                                                    "prefs", Context.MODE_PRIVATE);

    private PlaySessionStateStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlaySessionStateStorage(prefs);
    }

    @Test
    public void savePlayInfoStoresCurrentInfoInPrefs() {
        storage.savePlayInfo(TRACK, "play-id");

        assertThat(prefs.getString(ITEM.name(), Strings.EMPTY)).isEqualTo(TRACK.toString());
    }

    @Test
    public void saveProgressStoresProgressInPrefs() {
        storage.saveProgress(123);

        assertThat(prefs.getLong(PlaySessionStateStorage.Keys.PROGRESS.name(), 0)).isEqualTo(123L);
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
}
