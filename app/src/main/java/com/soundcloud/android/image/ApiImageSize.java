package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.EnumSet;

public enum ApiImageSize {
    T500("t500x500", 500, 500),
    T300("t300x300", 300, 300),
    T120("t120x120", 120, 120),
    T47("t47x47", 47, 47),
    T1240x260("t1240x260", 1240, 260),
    T2480x520("t2480x520", 2480, 520),
    Unknown("t120x120", 120, 120);

    public final int width;
    public final int height;
    public final String sizeSpec;

    public final static int RESOLUTION_1440 = 1440; // xhdpi, WXGA
    public final static int RESOLUTION_960 = 960; // xhdpi, WXGA
    public final static int RESOLUTION_480 = 480; // hdpi, WVGA

    public static final EnumSet<ApiImageSize> SMALL_SIZES = EnumSet.of(
            ApiImageSize.T47
    );

    ApiImageSize(String sizeSpec, int width, int height) {
        this.sizeSpec = sizeSpec;
        this.width = width;
        this.height = height;
    }

    public static ApiImageSize getListItemImageSize(Resources resources) {
        if (ImageUtils.isScreenXL(resources)) {
            return ApiImageSize.T120;
        } else {
            if (resources.getDisplayMetrics().density > 1) {
                return ApiImageSize.T120;
            } else {
                return ApiImageSize.T47;
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
            return ApiImageSize.T120;
        }
    }

    public static ApiImageSize getFullImageSize(Resources resources) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int maxResolution = Math.max(metrics.heightPixels, metrics.widthPixels);

        if (maxResolution >= RESOLUTION_960) {
            return ApiImageSize.T500;
        } else if (maxResolution >= RESOLUTION_480) {
            return ApiImageSize.T300;
        } else {
            return ApiImageSize.T120;
        }
    }

    public static ApiImageSize getFullBannerSize(Resources resources) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int maxResolution = Math.max(metrics.heightPixels, metrics.widthPixels);

        if (maxResolution >= RESOLUTION_1440) {
            return ApiImageSize.T2480x520;
        } else {
            return ApiImageSize.T1240x260;
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
}
