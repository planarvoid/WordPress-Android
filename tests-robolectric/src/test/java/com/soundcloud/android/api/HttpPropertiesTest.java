package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class HttpPropertiesTest {

    @Mock private Resources resources;

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        expect(new HttpProperties(resources).getClientSecret()).toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");
    }

    @Test
    public void shouldReturnApiHostWtihHttpsScheme() {
        when(resources.getString(R.string.mobile_api_base_url)).thenReturn("https://host/baseuri");
        expect(new HttpProperties(resources).getMobileApiHttpUrl()).toEqual("http://host/baseuri");
    }
}
