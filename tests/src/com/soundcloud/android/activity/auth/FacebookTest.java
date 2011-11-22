package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class FacebookTest {

    @Test
    public void testFallbackToWebFlow() throws Exception {
        Facebook fb = new Facebook();
        ShadowActivity shadow = shadowOf(fb);
        fb.onCreate(null);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();

        expect(intent.intent.getComponent().getClassName()).toEqual(FacebookWebFlow.class.getName());
    }

    @Test
    public void shouldUseSSOIfAvailable() throws Exception {
        Facebook fb = new Facebook() {
            @Override boolean clientSupportsSSO() {
                return true;
            }
        };
        ShadowActivity shadow = shadowOf(fb);
        fb.onCreate(null);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();
        expect(intent.intent.getComponent().getClassName()).toEqual(FacebookSSO.class.getName());
    }
}
