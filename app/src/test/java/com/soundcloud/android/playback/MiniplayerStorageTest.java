package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class MiniplayerStorageTest extends AndroidUnitTest {
    private final MiniplayerStorage storage = new MiniplayerStorage(sharedPreferences());

    @Test
    public void initStateToFalse() {
        assertThat(storage.hasMinimizedPlayerManually()).isEqualTo(false);
    }

    @Test
    public void setsMinimizedPlayerManually() {
        storage.setMinimizedPlayerManually();
        assertThat(storage.hasMinimizedPlayerManually()).isEqualTo(true);
    }

    @Test
    public void clearsState() {
        storage.setMinimizedPlayerManually();
        assertThat(storage.hasMinimizedPlayerManually()).isEqualTo(true);

        storage.clear();
        assertThat(storage.hasMinimizedPlayerManually()).isEqualTo(false);
    }

}
