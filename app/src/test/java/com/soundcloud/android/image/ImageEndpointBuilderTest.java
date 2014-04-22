package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SoundCloudTestRunner.class)
public class ImageEndpointBuilderTest {

    private ImageEndpointBuilder builder;

    @Mock
    private HttpProperties httpProperties;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        builder = new ImageEndpointBuilder(httpProperties);
    }

    @Test
    public void shouldBuildImageResolverUrlForConfiguredBaseUrl() {
        when(httpProperties.getApiMobileBaseUriPath()).thenReturn("/app/mobileapps");
        final String imageUrl = builder.imageUrl(Urn.parse("soundcloud:tracks:1"), ImageSize.LARGE);
        expect(imageUrl).toEqual("http://api.soundcloud.com/app/mobileapps/images/soundcloud:sounds:1/large");
    }
}
