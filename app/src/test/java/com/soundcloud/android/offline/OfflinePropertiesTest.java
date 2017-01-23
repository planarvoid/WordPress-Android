package com.soundcloud.android.offline;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OfflinePropertiesTest {

    @Test
    public void returnNOT_OFFLINEWhenEntityIsAbsent() {
        assertThat(OfflineProperties.empty()
                                    .likedTracksState())
                .isEqualTo(OfflineState.NOT_OFFLINE);

        assertThat(OfflineProperties.empty()
                                    .state(Urn.forTrack(123L)))
                .isEqualTo(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void returnStateAsCreated() {
        assertThat(OfflineProperties
                           .from(emptyMap(), OfflineState.REQUESTED)
                           .likedTracksState())
                .isEqualTo(OfflineState.REQUESTED);

        assertThat(OfflineProperties
                           .from(singletonMap(Urn.forTrack(123L), OfflineState.DOWNLOADED), OfflineState.NOT_OFFLINE)
                           .state(Urn.forTrack(123L)))
                .isEqualTo(OfflineState.DOWNLOADED);
    }
}
