package com.soundcloud.android.deeplinks;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class ResolveActivityTest {
    @Mock private IntentResolver intentResolver;

    @InjectMocks private ResolveActivity activity;

    @Test
    public void acceptsSoundCloudScheme() {
        expect(ResolveActivity.accept(Uri.parse("soundcloud:something:123"), Robolectric.application.getResources())).toBeTrue();
    }

    @Test
    public void doesNotAcceptOtherScheme() {
        expect(ResolveActivity.accept(Uri.parse("dubstep:something:123"), Robolectric.application.getResources())).toBeFalse();
    }

    @Test
    public void acceptsSoundCloudHost() {
        expect(ResolveActivity.accept(Uri.parse("http://www.soundcloud.com"), Robolectric.application.getResources())).toBeTrue();
    }

    @Test
    public void doesNotAcceptOtherHost() {
        expect(ResolveActivity.accept(Uri.parse("http://www.asdf.com"), Robolectric.application.getResources())).toBeFalse();
    }
}
