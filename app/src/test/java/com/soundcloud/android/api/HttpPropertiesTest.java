package com.soundcloud.android.api;

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
        when(resources.getString(R.string.api_mobile_base_uri_path)).thenReturn("/baseuri");
        when(resources.getString(R.string.api_host)).thenReturn("host");
    }

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {

        expect(new HttpProperties(resources).getClientSecret())
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

    }

    @Test
    public void shouldReturnBaseUri(){
        expect(new HttpProperties(resources).getApiMobileBaseUriPath()).toBe("/baseuri");
    }

    @Test
    public void shouldReturnApiHostWtihHttpsScheme(){
        expect(new HttpProperties(resources).getPrivateApiHostWithHttpScheme()).toEqual("http://host/baseuri");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoBaseUriFound(){
        when(resources.getString(R.string.api_mobile_base_uri_path)).thenReturn("  ");
        new HttpProperties(resources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfApiHostNotFound(){
        when(resources.getString(R.string.api_host)).thenReturn(" ");
        new HttpProperties(resources);
    }

}
