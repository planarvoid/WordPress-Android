package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class OfflineStateMapperTest extends AndroidUnitTest {

    @Mock CursorReader cursorReader;

    private final Date unavailable = new Date();
    private OfflineStateMapper mapper;

    @Before
    public void setUp() throws Exception {
        when(cursorReader.isNotNull(any(String.class))).thenReturn(false);
        when(cursorReader.isNotNull(TrackDownloads.UNAVAILABLE_AT)).thenReturn(true);
        when(cursorReader.getDateFromTimestamp(TrackDownloads.UNAVAILABLE_AT)).thenReturn(unavailable);

        mapper = new OfflineStateMapper();
    }

    @Test
    public void returnsUnavailableStateForOfflineCollection() {
        when(cursorReader.isNotNull(Tables.OfflineContent._ID)).thenReturn(true);

        final PropertySet result = mapper.map(cursorReader);
        assertThat(result.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void returnsNotOfflineInsteadOfUnavailableForNonOfflineCollections() {
        when(cursorReader.isNotNull(Tables.OfflineContent._ID)).thenReturn(false);

        PropertySet result = mapper.map(cursorReader);
        assertThat(result.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.NOT_OFFLINE);
    }
}
