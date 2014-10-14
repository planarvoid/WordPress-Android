package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class PlayBillingUtilTest {

    private Bundle bundle;
    private Intent intent;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        intent = new Intent();
    }

    @Test
    public void okCodeWhenResponseValueIsNull() throws Exception {
        int response = PlayBillingUtil.getResponseCodeFromBundle(bundle);
        expect(response).toEqual(PlayBillingUtil.RESULT_OK);
    }

    @Test
    public void getsResponseCodeAsInteger() throws Exception {
        bundle.putInt(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_USER_CANCELED);
        int response = PlayBillingUtil.getResponseCodeFromBundle(bundle);
        expect(response).toEqual(PlayBillingUtil.RESULT_USER_CANCELED);
    }

    @Test
    public void getsResponseCodeAsLong() throws Exception {
        bundle.putLong(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_USER_CANCELED);
        int response = PlayBillingUtil.getResponseCodeFromBundle(bundle);
        expect(response).toEqual(PlayBillingUtil.RESULT_USER_CANCELED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownErrorCodeIfUnexpectedObject() throws Exception {
        bundle.putString(PlayBillingUtil.RESPONSE_CODE, "invalid type");
        PlayBillingUtil.getResponseCodeFromBundle(bundle);
    }

    @Test
    public void getsResponseCodeFromIntentExtra() throws Exception {
        intent.putExtra(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_USER_CANCELED);
        int response = PlayBillingUtil.getResponseCodeFromIntent(intent);
        expect(response).toEqual(PlayBillingUtil.RESULT_USER_CANCELED);
    }

    @Test
    public void stripsAppNameFromProductTitle() throws Exception {
        String title = PlayBillingUtil.removeAppName("Super cool product (SoundCloud - Music & Audio)");
        expect(title).toEqual("Super cool product");
    }
}