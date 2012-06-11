package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class ResolveFetchTaskTest {

    @Test
    public void shouldFixUris() throws Exception {
        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/follow")))
                .toEqual("http://soundcloud.com/foo");

        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/favorite")))
                .toEqual("http://soundcloud.com/foo");

        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/bla/baz")))
                .toEqual("http://soundcloud.com/foo/bla/baz");
    }
}
