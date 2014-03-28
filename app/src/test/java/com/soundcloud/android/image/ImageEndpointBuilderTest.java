package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.HttpProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        final String imageUrl = builder.imageUrl("soundcloud:tracks:1", ImageSize.LARGE);
        expect(imageUrl).toEqual("http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large");
    }
}
