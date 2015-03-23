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
        ApiDisplayProperties properties = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("display_properties.json"), ApiDisplayProperties.class);

        expect(properties.defaultBackgroundColor).toEqual("#000000");
        expect(properties.defaultTextColor).toEqual("#FFFFFF");
        expect(properties.pressedBackgroundColor).toEqual("#575057");
        expect(properties.pressedTextColor).toEqual("#FFB225");
        expect(properties.focusedBackgroundColor).toEqual("#FFFFB7");
        expect(properties.focusedTextColor).toEqual("#00CFEE");
    }

}
