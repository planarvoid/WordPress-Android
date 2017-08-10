package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class DisplayPropertiesTest {

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
