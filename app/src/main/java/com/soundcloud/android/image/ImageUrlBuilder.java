package com.soundcloud.android.image;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

class ImageUrlBuilder {

    private static final String SIZE_TOKEN = "\\{size\\}";

    private final ApiUrlBuilder apiUrlBuilder;

    @Inject
    ImageUrlBuilder(ApiUrlBuilder apiUrlBuilder) {
        this.apiUrlBuilder = apiUrlBuilder;
    }

    @Nullable
    String buildUrl(ImageResource imageResource, ApiImageSize apiImageSize) {
        final Optional<String> urlTemplate = imageResource.getImageUrlTemplate();
        if (urlTemplate.isPresent()) {
            return buildUrlFromTemplate(apiImageSize, urlTemplate.get());
        } else if (!imageResource.getUrn().isUser()) {
            return imageResolverUrl(imageResource.getUrn(), apiImageSize);
        } else {
            return null;
        }
    }

    @Nullable
    String buildUrl(Optional<String> urlTemplate, ApiImageSize apiImageSize) {
        if (urlTemplate.isPresent()) {
            return buildUrlFromTemplate(apiImageSize, urlTemplate.get());
        } else {
            return null;
        }
    }

    @NonNull
    String imageResolverUrl(Urn urn, ApiImageSize apiImageSize) {
        return apiUrlBuilder
                .from(ApiEndpoints.IMAGES, urn, apiImageSize.sizeSpec)
                .build();
    }

    private String buildUrlFromTemplate(ApiImageSize apiImageSize, String urlTemplate) {
        return urlTemplate.replaceAll(SIZE_TOKEN, apiImageSize.sizeSpec);
    }
}
