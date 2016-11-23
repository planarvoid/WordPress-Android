package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import java.io.IOException;

public class DisplayPropertiesTest extends AndroidUnitTest {

    @Test
    public void deserializeDisplayProperties() throws IOException {
        ApiDisplayProperties properties = JacksonJsonTransformer.buildObjectMapper().readValue(
                getClass().getResourceAsStream("display_properties.json"), ApiDisplayProperties.class);

        assertThat(properties.defaultBackgroundColor).isEqualTo("#000000");
        assertThat(properties.defaultTextColor).isEqualTo("#FFFFFF");
        assertThat(properties.pressedBackgroundColor).isEqualTo("#575057");
        assertThat(properties.pressedTextColor).isEqualTo("#FFB225");
        assertThat(properties.focusedBackgroundColor).isEqualTo("#FFFFB7");
        assertThat(properties.focusedTextColor).isEqualTo("#00CFEE");
    }

}
