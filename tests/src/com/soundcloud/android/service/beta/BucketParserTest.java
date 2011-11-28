package com.soundcloud.android.service.beta;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.io.IOException;
import java.util.List;


public class BucketParserTest {
    @Test
    public void testBucketParsing() throws Exception {
        List<Beta> content = BucketParser.getContent(getClass().getResourceAsStream("bucket_contents.xml"));

        assertThat(content, notNullValue());
        assertThat(content.size(), is(2));
        Beta first =  content.get(0);
        assertThat(first.key, equalTo("com.soundcloud.android-27.apk"));
        assertThat(first.lastmodified, equalTo(1309961993000l));
        assertThat(first.etag, equalTo("94156b52d785aec62dc65f489c9d51b3"));
        assertThat(first.size, equalTo(2015887l));
        assertThat(first.storageClass, equalTo("STANDARD"));

        Beta second =  content.get(1);
        assertThat(second.key, equalTo("com.soundcloud.android-28.apk"));
    }


    @Test(expected = IOException.class)
    public void testBucketParsingWithInvalidContent() throws Exception {
        BucketParser.getContent(getClass().getResourceAsStream("bucket_contents_invalid.xml"));
    }
}
