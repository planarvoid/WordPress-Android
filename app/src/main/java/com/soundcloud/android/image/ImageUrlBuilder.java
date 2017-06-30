package com.soundcloud.android.image;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.NonNull;

import javax.inject.Inject;

class ImageUrlBuilder {

    private static final String SIZE_TOKEN = "\\{size\\}";

    private final ApiUrlBuilder apiUrlBuilder;

    @Inject
    ImageUrlBuilder(ApiUrlBuilder apiUrlBuilder) {
        this.apiUrlBuilder = apiUrlBuilder;
    }

    @Nullable
    String buildUrl(Optional<String> urlTemplate, Optional<Urn> urn, ApiImageSize apiImageSize) {
        if (urlTemplate.isPresent()) {
            return buildUrlFromTemplate(apiImageSize, urlTemplate.get());
        } else if (urn.isPresent() && !urn.get().isUser()) {
            return imageResolverUrl(urn.get(), apiImageSize);
        } else {
            return null;
        }
    }

    @NonNull
    private String imageResolverUrl(Urn urn, ApiImageSize apiImageSize) {
        return apiUrlBuilder
                .from(ApiEndpoints.IMAGES, urn, apiImageSize.sizeSpec)
                .build();
    }

    private String buildUrlFromTemplate(ApiImageSize apiImageSize, String urlTemplate) {
        return urlTemplate.replaceAll(SIZE_TOKEN, apiImageSize.sizeSpec);
    }
}
