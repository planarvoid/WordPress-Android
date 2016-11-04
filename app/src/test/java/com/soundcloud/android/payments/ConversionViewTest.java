package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.images.BackgroundDecoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ConversionViewTest extends AndroidUnitTest {

    @Mock BackgroundDecoder decoder;

    private ConversionView view;

    @Before
    public void setUp() throws Exception {
        view = new ConversionView(resources(), decoder);
    }

    @Test
    public void formatPromoDurationMonths() {
        assertThat(view.formatPromoDuration(15)).isEqualTo("15 days");
        assertThat(view.formatPromoDuration(30)).isEqualTo("1 month");
        assertThat(view.formatPromoDuration(90)).isEqualTo("3 months");
        assertThat(view.formatPromoDuration(123)).isEqualTo("4 months");
    }

}
