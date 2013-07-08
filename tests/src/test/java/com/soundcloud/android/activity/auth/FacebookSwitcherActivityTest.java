package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class FacebookSwitcherActivityTest {

    @Test
    public void testFallbackToWebFlow() throws Exception {
        FacebookSwitcherActivity fb = new FacebookSwitcherActivity();
        ShadowActivity shadow = shadowOf(fb);
        fb.onCreate(null);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();

        expect(intent.intent.getComponent().getClassName()).toEqual(FacebookWebFlow.class.getName());
    }

    @Test
    public void shouldUseSSOIfAvailable() throws Exception {
        FacebookSwitcherActivity fb = new FacebookSwitcherActivity() {
            @Override boolean clientSupportsSSO() {
                return true;
            }

            @Override
            boolean isSSOEnabled() {
                return true;
            }
        };
        ShadowActivity shadow = shadowOf(fb);
        fb.onCreate(null);
        ShadowActivity.IntentForResult intent = shadow.peekNextStartedActivityForResult();
        expect(intent.intent.getComponent().getClassName()).toEqual(FacebookSSO.class.getName());
    }
}
