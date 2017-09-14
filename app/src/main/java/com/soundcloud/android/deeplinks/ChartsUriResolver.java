package com.soundcloud.android.deeplinks;

import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

import android.net.Uri;
import android.support.annotation.NonNull;

import javax.inject.Inject;

public class ChartsUriResolver {
    private static final String ALL_WEB = "all";
    private static final String ALL_API = "all-music";
    private static final String ALL_AUDIO = "all-audio";

    @Inject
    ChartsUriResolver() {
    }

    public ChartDetails resolveUri(Uri uri) throws UriResolveException {
        try {
            if (DeepLink.isWebScheme(uri)) {
                return getChartDetailsFromWebScheme(uri);
            } else if (DeepLink.isHierarchicalSoundCloudScheme(uri)) {
                return getChartDetailsFromSoundCloudScheme(uri);
            } else {
                throw new IllegalArgumentException("Invalid schema for charts deeplink");
            }
        } catch (Exception e) {
            throw new UriResolveException("Charts Uri " + uri + " could not be resolved", e);
        }
    }

    @NonNull
    private ChartDetails getChartDetailsFromSoundCloudScheme(Uri uri) {
        String deeplink = uri.toString().replace("soundcloud://charts", "");

        if (deeplink.startsWith(":")) {
            String[] deeplinkParts = deeplink.substring(1).split(":");

            ChartType type = ChartType.TRENDING;
            String genre = ALL_API;

            if (deeplinkParts.length == 1) {
                String part = deeplinkParts[0];
                if ("audio".equals(part)) {
                    return ChartDetails.create(ChartType.TOP, Urn.forGenre(ALL_AUDIO));
                }
                if ("music".equals(part)) {
                    return ChartDetails.create(ChartType.TOP, Urn.forGenre(ALL_API));
                }

                type = typeFromString(part);
            } else if (deeplinkParts.length == 2) {
                type = typeFromString(deeplinkParts[0]);
                genre = deeplinkParts[1];
                if (genre == null || genre.equals(ALL_WEB)) {
                    genre = ALL_API;
                }
            }

            return ChartDetails.create(type, Urn.forGenre(genre));
        } else if (deeplink.startsWith("/")) {
            return getChartDetailsFromWebScheme(uri);
        } else {
            return ChartDetails.create(ChartType.TRENDING, Urn.forGenre(ALL_API));
        }
    }

    @NonNull
    private ChartDetails getChartDetailsFromWebScheme(Uri uri) {
        ChartType type = typeFromString(uri.getPath().replace("/charts/", ""));
        String genre = uri.getQueryParameter("genre");
        if (genre == null || genre.equals(ALL_WEB)) {
            genre = ALL_API;
        }

        return ChartDetails.create(type, Urn.forGenre(genre));
    }

    private static ChartType typeFromString(String type) {
        String chartType = type.startsWith("/") ? type.replaceFirst("/", "") : type;

        switch (chartType) {
            case "new": return ChartType.TRENDING;
            case "top": return ChartType.TOP;
            default: return ChartType.NONE;
        }
    }

}
