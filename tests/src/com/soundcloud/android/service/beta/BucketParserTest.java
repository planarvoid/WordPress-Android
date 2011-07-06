package com.soundcloud.android.service.beta;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.io.IOException;
import java.util.List;


public class BucketParserTest {
    @Test
    public void testBucketParsing() throws Exception {
        List<Content> content = BucketParser.getContent(getClass().getResourceAsStream("bucket_contents.xml"));

        assertThat(content, notNullValue());
        assertThat(content.size(), is(2));
        Content first =  content.get(0);
        assertThat(first.key, equalTo("soundcloud-1.3.4-BETA.apk"));
        assertThat(first.lastmodified, equalTo(1309961993000l));
        assertThat(first.etag, equalTo("94156b52d785aec62dc65f489c9d51b3"));
        assertThat(first.size, equalTo(2015887l));
        assertThat(first.storageClass, equalTo("STANDARD"));

        Content second =  content.get(1);
        assertThat(second.key, equalTo("soundcloud-1.3.5-BETA.apk"));
    }


    @Test(expected = IOException.class)
    public void testBucketParsingWithInvalidContent() throws Exception {
        BucketParser.getContent(getClass().getResourceAsStream("bucket_contents_invalid.xml"));
    }
}
