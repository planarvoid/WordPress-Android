package com.soundcloud.android.image;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.EnumSet;

public enum ApiImageSize {
    T500("t500x500", 500, 500),
    CROP("crop", 400, 400),
    T300("t300x300", 300, 300),
    LARGE("large", 100, 100),
    T67("t67x67", 67, 67),
    BADGE("badge", 47, 47),
    SMALL("small", 32, 32),
    TINY_ARTWORK("tiny", 20, 20),
    TINY_AVATAR("tiny", 18, 18),
    MINI("mini", 16, 16),
    Unknown("large", 100, 100);

    public final int width;
    public final int height;
    public final String sizeSpec;

    public static final EnumSet<ApiImageSize> SMALL_SIZES = EnumSet.of(
            ApiImageSize.BADGE,
            ApiImageSize.SMALL,
            ApiImageSize.TINY_ARTWORK,
            ApiImageSize.TINY_AVATAR,
            ApiImageSize.MINI
    );

    ApiImageSize(String sizeSpec, int width, int height) {
        this.sizeSpec = sizeSpec;
        this.width = width;
        this.height = height;
    }

    public static ApiImageSize fromString(String s) {
        for (ApiImageSize gs : values()) {
            if (gs.sizeSpec.equalsIgnoreCase(s)) {
                return gs;
            }
        }
        return Unknown;
    }

    @Deprecated // Use getListItemImageSize(Resources)
    public static ApiImageSize getListItemImageSize(Context c) {
        return getListItemImageSize(c.getResources());
    }

    public static ApiImageSize getListItemImageSize(Resources resources) {
        if (ImageUtils.isScreenXL(resources)) {
            return ApiImageSize.LARGE;
        } else {
            if (resources.getDisplayMetrics().density > 1) {
                return ApiImageSize.LARGE;
            } else {
                return ApiImageSize.BADGE;
            }
        }
    }

    public static String formatUriForNotificationLargeIcon(Context c, String uri) {
        return getNotificationLargeIconImageSize(c.getResources().getDisplayMetrics()).formatUri(uri);
    }

    public static ApiImageSize getNotificationLargeIconImageSize(Resources resources) {
        return getNotificationLargeIconImageSize(resources.getDisplayMetrics());
    }

    public static ApiImageSize getNotificationLargeIconImageSize(DisplayMetrics metrics) {
        if (metrics.density > 2) {
            return ApiImageSize.T300;
        } else {
            return ApiImageSize.LARGE;
        }
    }

    public static ApiImageSize getSearchSuggestionsListItemImageSize(Context c) {
        if (ImageUtils.isScreenXL(c.getResources())) {
            return ApiImageSize.T67;
        } else {
            if (c.getResources().getDisplayMetrics().density > 1) {
                return ApiImageSize.BADGE;
            } else {
                return ApiImageSize.SMALL;
            }
        }
    }

    public static ApiImageSize getFullImageSize(Resources resources) {
        ApiImageSize apiImageSize = ApiImageSize.fromString(resources.getString(R.string.full_image_size));
        if (apiImageSize != Unknown) {
            return apiImageSize;
        } else {
            return ApiImageSize.T500;
        }

    }

    public String formatUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }
        for (ApiImageSize size : ApiImageSize.values()) {
            if (uri.contains("-" + size.sizeSpec) && this != size) {
                return uri.replace("-" + size.sizeSpec, "-" + sizeSpec);
            }
        }
        Uri u = Uri.parse(uri);
        if (u.getPath().equals("/resolve/image")) {
            String size = u.getQueryParameter("size");
            if (size == null) {
                return u.buildUpon().appendQueryParameter("size", sizeSpec).toString();
            } else if (!size.equals(sizeSpec)) {
                return uri.replace(size, sizeSpec);
            }
        }
        return uri;
    }

    public static ApiImageSize getMinimumSizeFor(int width, int height, boolean fillDimensions) {
        ApiImageSize valid = null;
        for (ApiImageSize gs : values()) {
            if (fillDimensions) {
                if (gs.width >= width && gs.height >= height) {
                    valid = gs;
                } else {
                    break;
                }
            } else {
                if (gs.width >= width || gs.height >= height) {
                    valid = gs;
                } else {
                    break;
                }
            }

        }
        return valid == null ? Unknown : valid;
    }

}
