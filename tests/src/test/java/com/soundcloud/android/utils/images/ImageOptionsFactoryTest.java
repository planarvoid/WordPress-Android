package com.soundcloud.android.utils.images;

import static com.soundcloud.android.Expect.expect;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ImageOptionsFactoryTest {

    @Test
    public void shouldCreatePrefetchOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.prefetch();
        expect(displayImageOptions.isCacheInMemory()).toBeFalse();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void shouldCreateCacheOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.list(123);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void shouldCreatePlaceholderOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.list(123);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
        expect(displayImageOptions.getImageForEmptyUri()).toBe(123);
        expect(displayImageOptions.getImageOnFail()).toBe(123);
        expect(displayImageOptions.getStubImage()).toBe(123);
    }

    @Test
    public void shouldCreateListOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.list(123);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
        expect(displayImageOptions.getImageForEmptyUri()).toBe(123);
        expect(displayImageOptions.getImageOnFail()).toBe(123);
        expect(displayImageOptions.getStubImage()).toBe(123);
        expect(displayImageOptions.getDisplayer()).toBeInstanceOf(ImageOptionsFactory.ListAnimateListener.class);
    }
}
