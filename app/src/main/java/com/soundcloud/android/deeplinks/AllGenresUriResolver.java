package com.soundcloud.android.deeplinks;

import com.soundcloud.android.api.model.ChartCategory;

import android.net.Uri;

public final class AllGenresUriResolver {

    public static ChartCategory resolveUri(Uri uri) throws UriResolveException {
        try {
            String category = uri.toString().replace("soundcloud://charts:", "");
            return ChartCategory.from(category);
        } catch (Exception e) {
            throw new UriResolveException("Genres could not be resolved for " + uri, e);
        }
    }

    private AllGenresUriResolver() {
    }
}
