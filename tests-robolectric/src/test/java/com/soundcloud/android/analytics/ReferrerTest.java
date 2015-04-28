package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class ReferrerTest {

    @Test
    public void shouldGetTrackingTag() {
        expect(Referrer.FACEBOOK.get()).toEqual("facebook");
    }

    @Test
    public void setsAndGetsReferrerFromIntent() {
        final Intent intent = new Intent();
        Referrer.FACEBOOK.addToIntent(intent);
        expect(Referrer.fromIntent(intent)).toEqual(Referrer.FACEBOOK);
    }

    @Test
    public void getsReferrerFromOrigin() {
        expect(Referrer.fromOrigin("facebook")).toEqual(Referrer.FACEBOOK);
    }

    @Test
    public void getsDefaultReferrerFromUnknownOrigin() {
        expect(Referrer.fromOrigin("bing")).toEqual(Referrer.OTHER);
    }

    @Test
    public void getsReferrerFromHost() {
        expect(Referrer.fromHost("google.com")).toEqual(Referrer.GOOGLE);
    }

    @Test
    public void getsDefaultReferrerFromUnknownHost() {
        expect(Referrer.fromHost("bing.com")).toEqual(Referrer.OTHER);
    }

    @Test
    public void getsReferrerFromHostWithWww() {
        expect(Referrer.fromHost("www.facebook.com")).toEqual(Referrer.FACEBOOK);
    }
}
