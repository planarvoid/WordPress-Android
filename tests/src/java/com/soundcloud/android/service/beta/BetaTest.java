package com.soundcloud.android.service.beta;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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

        expect(new Beta().getVersionCode()).toEqual(-1);
        expect(c12.getVersionCode()).toEqual(12);
        expect(c13.getVersionCode()).toEqual(13);
        expect(c12.compareTo(c13)).toEqual(1);
        expect(c13.compareTo(c13)).toEqual(0);
        expect(c13.compareTo(c12)).toEqual(-1);

        List<Beta> l =Arrays.asList(c12, c13);
        Collections.sort(l);
        expect(l.get(0).getVersionCode()).toEqual(13);
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


        expect(c.toJSON()).toEqual(
            "{\"key\":\"foo\",\"lastmodified\":1309961993000,\"etag\":\"ETAG\",\"size\":2000," +
                    "\"storageClass\":\"FOO\",\"metadata\":{\"baz\":\"bar\"}}"
        );

        Beta json = Beta.fromJSON(c.toJSON());

        expect(json.key).toEqual(c.key);
        expect(json.lastmodified).toEqual(c.lastmodified);
        expect(json.etag).toEqual(c.etag);
        expect(json.size).toEqual(c.size);
        expect(json.storageClass).toEqual(c.storageClass);
        expect(json.metadata).toEqual(c.metadata);
    }

    @Test
    public void shouldParseInfoFromMetadata() throws Exception {
        Beta c = new Beta();
        c.metadata.put("android-versionname", "1.4-BETA");
        c.metadata.put("android-versioncode", "23");
        c.metadata.put("git-sha1", "GIT");

        expect(c.getVersionName()).toEqual("1.4-BETA");
        expect(c.getGitSha1()).toEqual("GIT");
        expect(c.getVersionCode()).toEqual(23);

        c.metadata.put("android-versioncode", "UNPARSEABLE");
        expect(c.getVersionCode()).toEqual(-1);
    }

    @Test
    public void shouldTestUpdate() throws Exception {

        assertTrue(Beta.isUptodate(10, "foo", 10, "foo"));
        assertTrue(Beta.isUptodate(15, "foo", 10, "foo"));
        assertFalse(Beta.isUptodate(9, "foo", 10, "foo"));

        assertFalse(Beta.isUptodate(10, "foo-1", 10, "foo-2"));
    }
}
