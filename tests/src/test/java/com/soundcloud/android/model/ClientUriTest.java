package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class ClientUriTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnInvalidUri() throws Exception {
        new ClientUri(Uri.parse("foo:bar:baz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnInvalidString() throws Exception {
        new ClientUri("foo:bar:baz");
    }

    @Test
    public void shouldParseCorrectlyUser() throws Exception {
        ClientUri uri = new ClientUri(Uri.parse("soundcloud:users:123"));
        expect(uri.type).toEqual("users");
        expect(uri.id).toEqual("123");
        expect(uri.isUser()).toBeTrue();
        expect(uri.isSound()).toBeFalse();
    }

    @Test
    public void shouldParseCorrectlySound() throws Exception {
        ClientUri uri = new ClientUri(Uri.parse("soundcloud:tracks:123"));
        expect(uri.type).toEqual("tracks");
        expect(uri.id).toEqual("123");
        expect(uri.isUser()).toBeFalse();
        expect(uri.isSound()).toBeTrue();
    }
}
