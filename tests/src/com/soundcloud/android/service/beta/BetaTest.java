package com.soundcloud.android.service.beta;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class BetaTest {
    @Test
    public void testGetVersion() throws Exception {
        Beta c12 = new Beta();
        c12.key = "foo-12.apk";

        Beta c13 = new Beta();
        c13.key = "foo-13.apk";

        assertThat(new Beta().getVersionCode(), is(-1));
        assertThat(c12.getVersionCode(), is(12));
        assertThat(c13.getVersionCode(), is(13));
        assertThat(c12.compareTo(c13), is(1));
        assertThat(c13.compareTo(c13), is(0));
        assertThat(c13.compareTo(c12), is(-1));

        List<Beta> l =Arrays.asList(c12, c13);
        Collections.sort(l);
        assertThat(l.get(0).getVersionCode(), is(13));
    }

    @Test
    public void shouldGenerateJson() throws Exception {
        Beta c = new Beta();
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

        Beta json = Beta.fromJSON(c.toJSON());

        assertThat(json.key, equalTo(c.key));
        assertThat(json.lastmodified, is(c.lastmodified));
        assertThat(json.etag, equalTo(c.etag));
        assertThat(json.size, is(c.size));
        assertThat(json.storageClass, equalTo(c.storageClass));
        assertThat(json.metadata, equalTo(c.metadata));
    }

    @Test
    public void shouldParseInfoFromMetadata() throws Exception {
        Beta c = new Beta();
        c.metadata.put("android-versionname", "1.4-BETA");
        c.metadata.put("android-versioncode", "23");
        c.metadata.put("git-sha1", "GIT");

        assertThat(c.getVersionName(), equalTo("1.4-BETA"));
        assertThat(c.getGitSha1(), equalTo("GIT"));
        assertThat(c.getVersionCode(), is(23));

        c.metadata.put("android-versioncode", "UNPARSEABLE");
        assertThat(c.getVersionCode(), is(-1));
    }

    @Test
    public void shouldTestUpdate() throws Exception {

        assertTrue(Beta.isUptodate(10, "foo", 10, "foo"));
        assertTrue(Beta.isUptodate(15, "foo", 10, "foo"));
        assertFalse(Beta.isUptodate(9, "foo", 10, "foo"));

        assertFalse(Beta.isUptodate(10, "foo-1", 10, "foo-2"));
    }
}
