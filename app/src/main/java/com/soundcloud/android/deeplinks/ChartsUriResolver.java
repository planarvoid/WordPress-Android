package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.utils.ScTextUtils.toResourceKey;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;

import javax.inject.Inject;

public class ChartsUriResolver {
    private static final String ALL_WEB = "all";
    private static final String ALL_API = "all-music";

    private final Context context;
    private final Resources resources;

    @Inject
    ChartsUriResolver(Context context,
                      Resources resources) {
        this.context = context;
        this.resources = resources;
    }

    ChartDetails resolveUri(Uri uri) {
        if (DeepLink.isWebScheme(uri)) {
            return getChartDetailsFromWebScheme(uri);
        } else if (DeepLink.isSoundCloudScheme(uri)) {
            return getChartDetailsFromSoundCloudScheme(uri);
        } else {
            throw new IllegalArgumentException("Invalid schema for charts deeplink");
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
                type = typeFromString(deeplinkParts[0]);
            } else if (deeplinkParts.length == 2) {
                type = typeFromString(deeplinkParts[0]);
                genre = deeplinkParts[1];
                if (genre == null || genre.equals(ALL_WEB)) {
                    genre = ALL_API;
                }
            }

            return ChartDetails.create(type, Urn.forGenre(genre), ChartCategory.NONE, titleForGenre(genre));
        } else {
            return ChartDetails.create(ChartType.TRENDING, Urn.forGenre(ALL_API), ChartCategory.NONE, Optional.absent());
        }
    }

    @NonNull
    private ChartDetails getChartDetailsFromWebScheme(Uri uri) {
        ChartType type = typeFromString(uri.getPath().replace("/charts/", ""));
        String genre = uri.getQueryParameter("genre");
        if (genre == null || genre.equals(ALL_WEB)) {
            genre = ALL_API;
        }

        return ChartDetails.create(type, Urn.forGenre(genre), ChartCategory.NONE, titleForGenre(genre));
    }

    private static ChartType typeFromString(String type) {
        switch (type) {
            case "new": return ChartType.TRENDING;
            case "top": return ChartType.TOP;
            default: return ChartType.NONE;
        }
    }

    private Optional<String> titleForGenre(String genre) {
        return appendCharts(headingFor(genre));
    }

    private Optional<String> headingFor(String genre) {
        String headingKey = toResourceKey("charts_", genre);
        int headingResourceId = resources.getIdentifier(headingKey, "string", context.getPackageName());
        if (headingResourceId == 0) {
            return Optional.absent();
        }
        return Optional.of(resources.getString(headingResourceId));
    }

    private Optional<String> appendCharts(Optional<String> heading) {
        if (heading.isPresent()) {
            return Optional.of(resources.getString(R.string.charts_page_header, heading.get()));
        }
        return heading;
    }
}
