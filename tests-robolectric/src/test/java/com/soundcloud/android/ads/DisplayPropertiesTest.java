package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class DisplayPropertiesTest {

    @Test
    public void deserializeDisplayProperties() throws IOException {
        DisplayProperties properties = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("display_properties.json"), DisplayProperties.class);

        expect(properties.getDefaultBackgroundColor()).toEqual("#000000");
        expect(properties.getDefaultTextColor()).toEqual("#FFFFFF");
        expect(properties.getPressedBackgroundColor()).toEqual("#575057");
        expect(properties.getPressedTextColor()).toEqual("#FFB225");
        expect(properties.getFocusedBackgroundColor()).toEqual("#FFFFB7");
        expect(properties.getFocusedTextColor()).toEqual("#00CFEE");
    }


}