package com.soundcloud.android.service.beta;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class ContentTest {
    @Test
    public void testGetVersion() throws Exception {
        Content c12 = new Content();
        c12.key = "foo-12.apk";

        Content c13 = new Content();
        c13.key = "foo-13.apk";

        assertThat(new Content().getVersion(), is(-1));
        assertThat(c12.getVersion(), is(12));
        assertThat(c13.getVersion(), is(13));
        assertThat(c12.compareTo(c13), is(1));
        assertThat(c13.compareTo(c13), is(0));
        assertThat(c13.compareTo(c12), is(-1));

        List<Content> l =Arrays.asList(c12, c13);
        Collections.sort(l);
        assertThat(l.get(0).getVersion(), is(13));
    }

    @Test
    public void shouldGenerateJson() throws Exception {
        Content c = new Content();
        c.key = "foo";
        c.lastmodified = 1309961993000l;
        c.etag = "ETAG";
        c.size = 2000l;
        c.storageClass = "FOO";
        c.metadata.put("baz", "bar");

        assertThat(c.toJSON(), equalTo(
            "{\"key\":\"foo\",\"lastmodified\":1309961993000,\"etag\":\"ETAG\",\"size\":2000," +
                    "\"storageClass\":\"FOO\",\"metadata\":{\"baz\":\"bar\"}}"
        ));

        Content json = Content.fromJSON(c.toJSON());

        assertThat(json.key, equalTo(c.key));
        assertThat(json.lastmodified, is(c.lastmodified));
        assertThat(json.etag, equalTo(c.etag));
        assertThat(json.size, is(c.size));
        assertThat(json.storageClass, equalTo(c.storageClass));
        assertThat(json.metadata, equalTo(c.metadata));
    }
}
