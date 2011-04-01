package com.soundcloud.api;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class HttpTests {
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArguemntForNonEvenParams() throws Exception {
        new Http.Params("1", 2, "3");
    }

    @Test
    public void shouldBuildAQueryString() throws Exception {
        assertThat(
                new Http.Params("foo", 100, "baz", 22.3f, "met\u00f8l", false).toString(),
                equalTo("foo=100&baz=22.3&met%C3%B8l=false"));
    }

    @Test
    public void shouldHaveToStringAsQueryString() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        assertThat(p.queryString(), equalTo(p.toString()));
    }

    @Test
    public void shouldGenerateUrlWithParameters() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        assertThat(p.url("http://foo.com"), equalTo("http://foo.com?foo=100&baz=22.3"));
    }

    @Test
    public void shouldHaveSizeMethod() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        assertThat(p.size(), is(2));
    }

    @Test
    public void shouldSupportAdd() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        p.add("baz", 66);
        assertThat(p.size(), is(3));
        assertThat(p.queryString(), equalTo("foo=100&baz=22.3&baz=66"));
    }

    @Test
    public void shouldImplementIterable() throws Exception {
        Http.Params p = new Http.Params("foo", 100, "baz", 22.3f);
        Iterator<NameValuePair> it = p.iterator();
        assertThat(it.next().getName(), equalTo("foo"));
        assertThat(it.next().getName(), equalTo("baz"));
        try {
            it.next();
            throw new RuntimeException("NoSuchElementException expected");
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void shouldGetStringFromHttpReponse() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity ent = mock(HttpEntity.class);
        when(ent.getContent()).thenReturn(new ByteArrayInputStream("foo".getBytes()));
        when(resp.getEntity()).thenReturn(ent);

        assertThat(Http.getString(resp), equalTo("foo"));
    }
}
