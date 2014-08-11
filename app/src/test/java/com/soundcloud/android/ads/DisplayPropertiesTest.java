package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class DisplayPropertiesTest {

    @Test
    public void deserializeDisplayProperties() throws IOException {
        DisplayProperties properties = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("display_properties.json"), DisplayProperties.class);

        expect(properties.getDefaultBackground()).toEqual("#000000");
        expect(properties.getDefaultText()).toEqual("#FFFFFF");
        expect(properties.getPressedBackground()).toEqual("#575057");
        expect(properties.getPressedText()).toEqual("#FFB225");
        expect(properties.getFocusedBackground()).toEqual("#FFFFB7");
        expect(properties.getFocusedText()).toEqual("#00CFEE");
    }


}