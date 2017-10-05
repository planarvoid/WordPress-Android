package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class StreamCacheConfigTest {

    private StreamCacheConfig cacheConfig;
    private String cacheKey = "cacheKey";

    @Mock private Context context;
    @Mock private TelphonyBasedCountryProvider countryProvider;
    @Mock private File cacheDirectory;
    @Mock private IOUtils ioUtils;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new StreamCacheConfig<>(context, countryProvider, ioUtils, cacheKey, cacheDirectory);
    }

    @Test
    public void cacheIsMinInUS() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("us");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInGB() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("gb");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInDE() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("de");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinInFR() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("fr");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinWithEmptyCode() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinWithNullCode() throws Exception {
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMaxInNepal() throws Exception {
        when(countryProvider.getCountryCode()).thenReturn("ne");
        assertThat(cacheConfig.size()).isEqualTo(StreamCacheConfig.SkippyConfig.MAX_SIZE_BYTES);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilSizeCeiling() {
        final long hugeSdCard = 100000000L;
        when(cacheDirectory.getUsableSpace()).thenReturn(hugeSdCard - 1);
        when(cacheDirectory.getTotalSpace()).thenReturn(hugeSdCard);

        when(countryProvider.getCountryCode()).thenReturn("us");
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES - 1);

        assertThat(cacheConfig.remainingSpace()).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilPercentCeiling() {
        when(cacheDirectory.getUsableSpace()).thenReturn((long) (StreamCacheConfig.SkippyConfig.STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE + 1));
        when(cacheDirectory.getTotalSpace()).thenReturn(100L);

        when(countryProvider.getCountryCode()).thenReturn("us");
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SkippyConfig.MIN_SIZE_BYTES - 1);

        assertThat(cacheConfig.remainingSpace()).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsZeroIfCacheDirNull() {
        assertThat(new StreamCacheConfig<>(context, countryProvider, ioUtils, cacheKey, null).remainingSpace()).isEqualTo(0);
    }
}
