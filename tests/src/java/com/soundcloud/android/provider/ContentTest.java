package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ContentTest {

    @Test
    public void shouldDefineRequest() throws Exception {
        expect(Content.ME_SOUND_STREAM.request().toUrl())
                .toEqual(Content.ME_SOUND_STREAM.remoteUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoRemoteUriDefined() throws Exception {
        Content.COLLECTIONS.request();
    }

    @Test
    public void shouldFindContentByUri() throws Exception {
        expect(Content.byUri(Content.ME.uri)).toBe(Content.ME);
    }

    @Test
    public void shouldProvideToString() throws Exception {
        expect(Content.ME_ACTIVITIES.toString()).toEqual("Content.ME_ACTIVITIES");
    }
}
