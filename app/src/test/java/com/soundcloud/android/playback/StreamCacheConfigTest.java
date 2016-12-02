package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class StreamCacheConfigTest {

    private StreamCacheConfig cacheConfig;

    @Mock private TelphonyBasedCountryProvider countryProvider;
    @Mock private File cacheDirectory;
    @Mock private IOUtils ioUtils;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new StreamCacheConfig(countryProvider, ioUtils, cacheDirectory);
    }

    @Test
    public void cacheIsMinInUS() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("us");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInGB() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("gb");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInDE() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("de");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInFR() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("fr");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinWithEmptyCode() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinWithNullCode() throws Exception {
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMaxInNepal() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("ne");
        assertThat(cacheConfig.getStreamCacheSize()).isEqualTo(StreamCacheConfig.SkippyConfig.MAX_SIZE_BYTES);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilSizeCeiling() {
        final long hugeSdCard = 100000000L;
        when(cacheDirectory.getUsableSpace()).thenReturn(hugeSdCard - 1);
        when(cacheDirectory.getTotalSpace()).thenReturn(hugeSdCard);

        when(countryProvider.getCountryCode()).thenReturn("us");
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES - 1);

        assertThat(cacheConfig.getRemainingCacheSpace()).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilPercentCeiling() {
        when(cacheDirectory.getUsableSpace()).thenReturn((long) (StreamCacheConfig.SkippyConfig.STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE + 1));
        when(cacheDirectory.getTotalSpace()).thenReturn(100L);

        when(countryProvider.getCountryCode()).thenReturn("us");
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES - 1);

        assertThat(cacheConfig.getRemainingCacheSpace()).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsZeroIfCacheDirNull() {
        assertThat( new StreamCacheConfig(countryProvider, ioUtils, null).getRemainingCacheSpace()).isEqualTo(0);
    }
}
