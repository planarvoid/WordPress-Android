package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CacheConfigTest {

    @Test
    public void cacheIsMinInUS() throws Exception {
        expect(CacheConfig.getCacheSize("us")).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }
    @Test
    public void cacheIsMinInGB() throws Exception {
        expect(CacheConfig.getCacheSize("gb")).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }
    @Test
    public void cacheIsMinInDE() throws Exception {
        expect(CacheConfig.getCacheSize("de")).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }
    @Test
    public void cacheIsMinInFR() throws Exception {
        expect(CacheConfig.getCacheSize("fr")).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }
    @Test
    public void cacheIsMinWithEmptyCode() throws Exception {
        expect(CacheConfig.getCacheSize("")).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMinWithNullCode() throws Exception {
        expect(CacheConfig.getCacheSize(null)).toEqual(CacheConfig.MIN_SIZE_BYTES);
    }

    @Test
    public void cacheIsMaxInNepal() throws Exception {
        expect(CacheConfig.getCacheSize("ne")).toEqual(CacheConfig.MAX_SIZE_BYTES);
    }

}