package com.soundcloud.android.service.beta;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.io.IOException;
import java.util.List;


public class BucketParserTest {
    @Test
    public void testBucketParsing() throws Exception {
        List<Beta> content = BucketParser.getContent(getClass().getResourceAsStream("bucket_contents.xml"));

        expect(content).not.toBeNull();
        expect(content.size()).toEqual(2);
        Beta first =  content.get(0);
        expect(first.key).toEqual("com.soundcloud.android-27.apk");
        expect(first.lastmodified).toEqual(1309961993000l);
        expect(first.etag).toEqual("94156b52d785aec62dc65f489c9d51b3");
        expect(first.size).toEqual(2015887l);
        expect(first.storageClass).toEqual("STANDARD");

        Beta second =  content.get(1);
        expect(second.key).toEqual("com.soundcloud.android-28.apk");
    }


    @Test(expected = IOException.class)
    public void testBucketParsingWithInvalidContent() throws Exception {
        BucketParser.getContent(getClass().getResourceAsStream("bucket_contents_invalid.xml"));
    }
}
