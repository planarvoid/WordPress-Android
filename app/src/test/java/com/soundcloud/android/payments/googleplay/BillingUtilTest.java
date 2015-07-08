package com.soundcloud.android.payments.googleplay;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.Intent;
import android.os.Bundle;

public class BillingUtilTest extends AndroidUnitTest {

    private Bundle bundle;
    private Intent intent;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        intent = new Intent();
    }

    @Test
    public void okCodeWhenResponseValueIsNull() throws Exception {
        int response = BillingUtil.getResponseCodeFromBundle(bundle);
        assertThat(response).isEqualTo(BillingUtil.RESULT_OK);
    }

    @Test
    public void getsResponseCodeAsInteger() throws Exception {
        bundle.putInt(BillingUtil.RESPONSE_CODE, BillingUtil.RESULT_USER_CANCELED);
        int response = BillingUtil.getResponseCodeFromBundle(bundle);
        assertThat(response).isEqualTo(BillingUtil.RESULT_USER_CANCELED);
    }

    @Test
    public void getsResponseCodeAsLong() throws Exception {
        bundle.putLong(BillingUtil.RESPONSE_CODE, BillingUtil.RESULT_USER_CANCELED);
        int response = BillingUtil.getResponseCodeFromBundle(bundle);
        assertThat(response).isEqualTo(BillingUtil.RESULT_USER_CANCELED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownErrorCodeIfUnexpectedObject() throws Exception {
        bundle.putString(BillingUtil.RESPONSE_CODE, "invalid type");
        BillingUtil.getResponseCodeFromBundle(bundle);
    }

    @Test
    public void getsResponseCodeFromIntentExtra() throws Exception {
        intent.putExtra(BillingUtil.RESPONSE_CODE, BillingUtil.RESULT_USER_CANCELED);
        int response = BillingUtil.getResponseCodeFromIntent(intent);
        assertThat(response).isEqualTo(BillingUtil.RESULT_USER_CANCELED);
    }

    @Test
    public void stripsAppNameFromProductTitle() throws Exception {
        String title = BillingUtil.removeAppName("Super cool product (SoundCloud - Music & Audio)");
        assertThat(title).isEqualTo("Super cool product");
    }
}