package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.MultiMap;
import org.junit.Test;

import android.net.Uri;

public class UriUtilsTest extends AndroidUnitTest {

    @Test
    public void shouldGetQueryParamsSingleParam(){
        MultiMap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99");
        assertThat(result.get("a")).containsExactly("99");
    }

    @Test
    public void shouldGetQueryParamsMultiParam(){
        MultiMap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99&b=765");
        assertThat(result.get("a")).containsExactly("99");
        assertThat(result.get("b")).containsExactly("765");
    }

    @Test
    public void shouldGetQueryParamsMultiParamWithRepeats(){
        MultiMap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99&b=765&a=321");
        assertThat(result.get("a")).containsExactly("99", "321");
        assertThat(result.get("b")).containsExactly("765");
    }

    @Test
    public void shouldGetPathWithQuery() {
        Uri uri = Uri.parse("http://soundcloud.com/some_path?some=query&and=another");
        String pathAndQuery = UriUtils.getPathWithQuery(uri);
        assertThat(pathAndQuery).isEqualTo("/some_path?some=query&and=another");
    }
}
