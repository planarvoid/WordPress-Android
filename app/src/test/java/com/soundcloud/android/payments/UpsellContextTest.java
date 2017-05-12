package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;

public class UpsellContextTest extends AndroidUnitTest {

    @Test
    public void savesUpsellContextToIntent() {
        Intent intent = new Intent();

        UpsellContext.OFFLINE.addTo(intent);

        assertThat(UpsellContext.from(intent)).isEqualTo(UpsellContext.OFFLINE);
    }

    @Test
    public void returnsDefaultUpsellContextIfNotAbsent() {
        assertThat(UpsellContext.from(new Intent())).isEqualTo(UpsellContext.DEFAULT);
    }

}
