package com.soundcloud.android.image;

import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.Locale;

class ImageEndpointBuilder {

    private static final String IMAGE_URL_FORMAT = "http://api.soundcloud.com%s/images/%s/%s";

    private final HttpProperties httpProperties;

    @Inject
    ImageEndpointBuilder(HttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    String imageUrl(Urn urn, ApiImageSize apiImageSize) {
        final String baseUrl = httpProperties.getApiMobileBaseUriPath();
        return String.format(Locale.US, IMAGE_URL_FORMAT, baseUrl, urn, apiImageSize.sizeSpec);
    }
}
