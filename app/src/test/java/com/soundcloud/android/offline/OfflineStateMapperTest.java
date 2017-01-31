package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.CursorReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class OfflineStateMapperTest extends AndroidUnitTest {

    @Mock CursorReader cursorReader;

    private final Date unavailable = new Date();

    @Before
    public void setUp() throws Exception {
        when(cursorReader.isNotNull(any(String.class))).thenReturn(false);
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
