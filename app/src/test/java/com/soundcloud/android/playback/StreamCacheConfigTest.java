package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class StreamCacheConfigTest {

    private StreamCacheConfig cacheConfig;

    @Mock private File cacheDirectory;
    @Mock private IOUtils ioUtils;

    @Before
    public void setUp() throws Exception {
        cacheConfig = new StreamCacheConfig(cacheDirectory, ioUtils);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilSizeCeiling() {
        final long hugeSdCard = 100000000L;
        when(cacheDirectory.getUsableSpace()).thenReturn(hugeSdCard - 1);
        when(cacheDirectory.getTotalSpace()).thenReturn(hugeSdCard);
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SIZE_BYTES - 1);

        long remainingCacheSpace = cacheConfig.getRemainingCacheSpace();

        assertThat(remainingCacheSpace).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsSpaceUntilPercentCeiling() {
        when(cacheDirectory.getUsableSpace()).thenReturn((long) (StreamCacheConfig.STREAM_CACHE_MIN_FREE_SPACE_AVAILABLE_PERCENTAGE + 1));
        when(cacheDirectory.getTotalSpace()).thenReturn(100L);
        when(ioUtils.dirSize(cacheDirectory)).thenReturn(StreamCacheConfig.SIZE_BYTES - 1);

        long remainingCacheSpace = cacheConfig.getRemainingCacheSpace();

        assertThat(remainingCacheSpace).isEqualTo(1);
    }

    @Test
    public void getsRemainingCacheSpaceReturnsZeroIfCacheDirNull() {
        assertThat(new StreamCacheConfig(null, ioUtils).getRemainingCacheSpace()).isEqualTo(0);
    }
}
