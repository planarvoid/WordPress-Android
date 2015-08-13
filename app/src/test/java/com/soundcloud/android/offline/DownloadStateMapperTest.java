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

public class DownloadStateMapperTest extends AndroidUnitTest {

    @Mock CursorReader cursorReader;

    private final Date unavailable = new Date();
    private DownloadStateMapper mapper;

    @Before
    public void setUp() throws Exception {
        when(cursorReader.isNotNull(any(String.class))).thenReturn(false);
        when(cursorReader.isNotNull(TrackDownloads.UNAVAILABLE_AT)).thenReturn(true);
        when(cursorReader.getDateFromTimestamp(TrackDownloads.UNAVAILABLE_AT)).thenReturn(unavailable);

        mapper = new DownloadStateMapper();
    }

    @Test
    public void returnsUnavailableStateForOfflineCollection() {
        when(cursorReader.isNotNull(Tables.OfflineContent._ID.prefixedName())).thenReturn(true);

        final PropertySet result = mapper.map(cursorReader);
        assertThat(result.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void doesNotReturnUnavailableStateForNonOfflineCollections() {
        when(cursorReader.isNotNull(Tables.OfflineContent._ID.prefixedName())).thenReturn(false);

        PropertySet result = mapper.map(cursorReader);
        assertThat(result.contains(OfflineProperty.OFFLINE_STATE)).isFalse();
    }
}