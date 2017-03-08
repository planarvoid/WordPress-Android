package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.strings.Strings;
import org.junit.Test;

public class OfflineContentLocationTest {

    @Test
    public void shouldMapToOfflineContentLocationFromId() {
        assertThat(OfflineContentLocation.fromId(null)).isEqualTo(OfflineContentLocation.DEVICE_STORAGE);
        assertThat(OfflineContentLocation.fromId(Strings.EMPTY)).isEqualTo(OfflineContentLocation.DEVICE_STORAGE);
        assertThat(OfflineContentLocation.fromId("sd_card")).isEqualTo(OfflineContentLocation.SD_CARD);
        assertThat(OfflineContentLocation.fromId("device_storage")).isEqualTo(OfflineContentLocation.DEVICE_STORAGE);
    }
}
