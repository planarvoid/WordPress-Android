package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.propeller.CursorReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class OfflineStateMapperTest {

    @Mock CursorReader cursorReader;

    private final Date unavailable = new Date();

    @Before
    public void setUp() throws Exception {
        when(cursorReader.isNotNull(TrackDownloads.UNAVAILABLE_AT)).thenReturn(true);
        when(cursorReader.getDateFromTimestamp(TrackDownloads.UNAVAILABLE_AT)).thenReturn(unavailable);
    }

    @Test
    public void returnsUnavailableStateForOfflineCollection() {
        assertThat(OfflineStateMapper.fromDates(cursorReader, true)).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void returnsNotOfflineInsteadOfUnavailableForNonOfflineCollections() {
        assertThat(OfflineStateMapper.fromDates(cursorReader, false)).isEqualTo(OfflineState.NOT_OFFLINE);
    }
}
