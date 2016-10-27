package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class LegacyConversionViewTest extends AndroidUnitTest {

    private LegacyConversionView view;

    @Before
    public void setUp() throws Exception {
        view = new LegacyConversionView(resources());
    }

    @Test
    public void formatPromoDurationMonths() {
        assertThat(view.formatPromoDuration(15)).isEqualTo("15 days");
        assertThat(view.formatPromoDuration(30)).isEqualTo("1 month");
        assertThat(view.formatPromoDuration(90)).isEqualTo("3 months");
        assertThat(view.formatPromoDuration(123)).isEqualTo("4 months");
    }

}