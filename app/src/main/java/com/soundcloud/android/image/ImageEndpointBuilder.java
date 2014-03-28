package com.soundcloud.android.image;

import com.soundcloud.android.api.http.HttpProperties;

import javax.inject.Inject;
import java.util.Locale;

class ImageEndpointBuilder {

    private static final String IMAGE_URL_FORMAT = "http://api.soundcloud.com%s/images/%s/%s";

    private final HttpProperties mHttpProperties;

    @Inject
    ImageEndpointBuilder(HttpProperties httpProperties) {
        mHttpProperties = httpProperties;
    }

    String imageUrl(String urn, ImageSize imageSize) {
        final String baseUrl = mHttpProperties.getApiMobileBaseUriPath();
        return String.format(Locale.US, IMAGE_URL_FORMAT, baseUrl, urn, imageSize.sizeSpec);
    }
}
