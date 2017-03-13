package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImageUrlBuilderTest {

    private ImageUrlBuilder builder;

    @Mock private ApiUrlBuilder apiUrlBuilder;
    @Mock private ImageResource imageResource;

    private final String artworkTemplate = "https://sndcdn.com/path-{size}.jpg";
    private final String resolveEndpoint = "https://fake-resolve-endpoint";
    private final Urn trackUrn = Urn.forTrack(1);
    private final Urn userUrn = Urn.forUser(1);

    @Before
    public void setUp() throws Exception {
        when(apiUrlBuilder.build()).thenReturn(resolveEndpoint);
        builder = new ImageUrlBuilder(apiUrlBuilder);
    }

    @Test
    public void shouldUseArtworkFromUrlTemplateIfAvailable() {
        when(imageResource.getImageUrlTemplate()).thenReturn(Optional.of(artworkTemplate));

        String imageUrl = builder.buildUrl(imageResource, ApiImageSize.T500);

        assertThat(imageUrl).isEqualTo("https://sndcdn.com/path-t500x500.jpg");
    }

    @Test
    public void shouldUseResolveEndpointIfArtworkNotAvailable() {
        when(imageResource.getImageUrlTemplate()).thenReturn(Optional.absent());
        when(imageResource.getUrn()).thenReturn(trackUrn);
        when(apiUrlBuilder.from(ApiEndpoints.IMAGES, trackUrn, "t500x500")).thenReturn(apiUrlBuilder);

        String imageUrl = builder.buildUrl(imageResource, ApiImageSize.T500);

        assertThat(imageUrl).isEqualTo(resolveEndpoint);
    }

    @Test
    public void shouldNotUseResolveEndpointForUsersSinceTheyDontHaveFallbacks() {
        when(imageResource.getImageUrlTemplate()).thenReturn(Optional.absent());
        when(imageResource.getUrn()).thenReturn(userUrn);

        String imageUrl = builder.buildUrl(imageResource, ApiImageSize.T500);

        assertThat(imageUrl).isNull();
    }
}
