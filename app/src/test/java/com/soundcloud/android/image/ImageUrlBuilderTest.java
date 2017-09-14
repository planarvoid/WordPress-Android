package com.soundcloud.android.image;

import static com.soundcloud.android.image.ApiImageSize.T500;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImageUrlBuilderTest {

    private ImageUrlBuilder builder;

    @Mock private ApiUrlBuilder apiUrlBuilder;

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

        String imageUrl = builder.buildUrl(Optional.of(artworkTemplate), Urn.NOT_SET, T500);

        assertThat(imageUrl).isEqualTo("https://sndcdn.com/path-t500x500.jpg");
    }

    @Test
    public void shouldUseResolveEndpointIfArtworkNotAvailable() {
        when(apiUrlBuilder.from(ApiEndpoints.IMAGES, trackUrn, "t500x500")).thenReturn(apiUrlBuilder);

        String imageUrl = builder.buildUrl(Optional.absent(), trackUrn, T500);

        assertThat(imageUrl).isEqualTo(resolveEndpoint);
    }

    @Test
    public void shouldNotUseResolveEndpointForUsersSinceTheyDontHaveFallbacks() {
        String imageUrl = builder.buildUrl(Optional.absent(), userUrn, ApiImageSize.T500);

        assertThat(imageUrl).isNull();
    }

    @Test
    public void shouldBeNullWhenNoUrlTemplateOrUrnProvided() {
        String imageUrl = builder.buildUrl(Optional.absent(), Urn.NOT_SET, T500);

        assertThat(imageUrl).isNull();
    }
}
