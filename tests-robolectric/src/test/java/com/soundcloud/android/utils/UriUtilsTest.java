package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Multimap;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class UriUtilsTest {

    @Test
    public void shouldGetQueryParamsSingleParam(){
        Multimap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99");
        expect(result.get("a")).toContainExactly("99");
    }

    @Test
    public void shouldGetQueryParamsMultiParam(){
        Multimap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99&b=765");
        expect(result.get("a")).toContainExactly("99");
        expect(result.get("b")).toContainExactly("765");
    }

    @Test
    public void shouldGetQueryParamsMultiParamWithRepeats(){
        Multimap<String,String> result = UriUtils.getQueryParameters("http://www.blah.com?a=99&b=765&a=321");
        expect(result.get("a")).toContainExactly("99", "321");
        expect(result.get("b")).toContainExactly("765");
    }

    @Test
    public void shouldGetPathWithQuery() {
        Uri uri = Uri.parse("http://soundcloud.com/some_path?some=query&and=another");
        String pathAndQuery = UriUtils.getPathWithQuery(uri);
        expect(pathAndQuery).toEqual("/some_path?some=query&and=another");
    }
}
