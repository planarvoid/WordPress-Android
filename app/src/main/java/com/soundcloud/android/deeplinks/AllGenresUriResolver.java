package com.soundcloud.android.deeplinks;

import com.soundcloud.android.api.model.ChartCategory;

import android.net.Uri;

class AllGenresUriResolver {

    static ChartCategory resolveUri(Uri uri) {
        String category = uri.toString().replace("soundcloud://charts:", "");
        return ChartCategory.from(category);
    }
}
