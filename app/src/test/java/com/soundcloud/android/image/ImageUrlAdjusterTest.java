package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ImageUrlAdjusterTest {

    ImageUrlAdjuster imageUrlAdjuster;

    @Before
    public void setUp() throws Exception {
        imageUrlAdjuster = new ImageUrlAdjuster();
    }

    @Test
    public void shouldAdjustProtocolToBeInsecure() throws Exception {
        expect(imageUrlAdjuster.adjust("https://www.image.com")).toEqual("http://www.image.com");
    }
}
