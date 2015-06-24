package com.soundcloud.android.testsupport.matchers;

import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UrlMatcherTest {

    @Test
    public void urlsAreEqualIfSchemeAndHostAndPathAndQueryMatches() {
        assertThat("http://host.com/path?a=1", is(urlEqualTo("http://host.com/path?a=1")));
    }

    @Test
    public void urlsAreInequalIfSchemeDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((urlEqualTo("https://host.com/path?a=1")))));
    }

    @Test
    public void urlsAreInequalIfHostDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((urlEqualTo("http://host.de/path?a=1")))));
    }

    @Test
    public void urlsAreInequalIfPathDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((urlEqualTo("http://host.com/other?a=1")))));
    }

    @Test
    public void urlsAreInequalIfQueryDoesNotMatch() {
        assertThat("http://host.com/path?a=1&b=2", is(not((urlEqualTo("http://host.com/path?a=1&b=0")))));
    }

}