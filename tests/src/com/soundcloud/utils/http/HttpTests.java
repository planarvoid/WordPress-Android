package com.soundcloud.utils.http;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class HttpTests {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArguemntForNonEvenParams() throws Exception {
        new Http.Params("1", 2, "3");
    }

    @Test
    public void shouldBuildAQueryString() throws Exception {
        assertThat(
                new Http.Params("foo", 100, "baz", 22.3f, "met¿l", false).toString(),
                equalTo("foo=100&baz=22.3&met%C3%B8l=false"));
    }

    @Test
    public void shouldHaveToStringAsQueryString() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        assertThat(p.queryString(), equalTo(p.toString()));
    }
}
