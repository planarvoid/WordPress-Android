package com.soundcloud.android.testsupport.matchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Test;

public class UrlMatcherTest extends PlatformUnitTest {

    @Test
    public void urlsAreEqualIfSchemeAndHostAndPathAndQueryMatches() {
        assertThat("http://host.com/path?a=1", is(RequestMatchers.urlEqualTo("http://host.com/path?a=1")));
    }

    @Test
    public void urlsAreInequalIfSchemeDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((RequestMatchers.urlEqualTo("https://host.com/path?a=1")))));
    }

    @Test
    public void urlsAreInequalIfHostDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((RequestMatchers.urlEqualTo("http://host.de/path?a=1")))));
    }

    @Test
    public void urlsAreInequalIfPathDoesNotMatch() {
        assertThat("http://host.com/path?a=1", is(not((RequestMatchers.urlEqualTo("http://host.com/other?a=1")))));
    }

    @Test
    public void urlsAreInequalIfQueryDoesNotMatch() {
        assertThat("http://host.com/path?a=1&b=2", is(not((RequestMatchers.urlEqualTo("http://host.com/path?a=1&b=0")))));
    }

}