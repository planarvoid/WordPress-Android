package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class HttpPropertiesTest {

    @Mock
    private Resources resources;

    @Before
    public void setUp(){
        when(resources.getString(R.string.api_mobile_base_uri_path)).thenReturn("baseuri");
    }

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        // live
        expect(new HttpProperties(resources).getClientSecret())
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

    }

    @Test
    public void shouldReturnBaseUri(){
        expect(new HttpProperties(resources).getApiMobileBaseUriPath()).toBe("baseuri");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoBaseUriFound(){
        when(resources.getString(R.string.api_mobile_base_uri_path)).thenReturn("  ");
        new HttpProperties(resources).getApiMobileBaseUriPath();
    }
}
