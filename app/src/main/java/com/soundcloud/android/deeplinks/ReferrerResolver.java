package com.soundcloud.android.deeplinks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.java.strings.Strings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

class ReferrerResolver {
    private static final String FACEBOOK_PKG_NAME = "com.facebook.application.";
    private static final String TWITTER_PKG_NAME = "com.twitter.android";
    private static final String GOOGLE_PLUS_PKG_NAME = "com.google.android.apps.plus";

    private static final String EXTRA_BROWSER_APPLICATION_ID = "com.android.browser.application_id";
    private static final String EXTRA_INTENT_ANCESTOR = "intent.extra.ANCESTOR";
    private static final String EXTRA_ANDROID_BROWSER_HEADERS = "com.android.browser.headers";
    private static final String EXTRA_BROWSER_REFERER = "Referer";
    private static final String EXTRA_FACEBOOK_APP_ID = "app_id";

    private static final String ANDROID_INTENT_EXTRA_REFERRER = "android.intent.extra.REFERRER";
    private static final String ANDROID_INTENT_EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";
    private static final String GOOGLE_APPCRAWLER_PACKAGE_NAME = "com.google.appcrawler";
    private static final String ANDROID_APP_SCHEME = "android-app";

    private static final String PARAM_ORIGIN = "origin";

    @Inject
    ReferrerResolver() {
        // for dagger
    }

    public String getReferrerFromIntent(Intent intent, Resources resources) {
        try {
            if (Referrer.hasReferrer(intent)) {
                return Referrer.fromIntent(intent).get();
            } else if (isOriginIntent(intent)) {
                return referrerFromOrigin(intent).get();
            } else if (isFacebookIntent(intent, resources)) {
                return Referrer.FACEBOOK.get();
            } else if (isTwitterIntent(intent)) {
                return Referrer.TWITTER.get();
            } else if (isGooglePlusIntent(intent)) {
                return Referrer.GOOGLE_PLUS.get();
            } else if (isGoogleCrawlerIntent(intent)) {
                return Referrer.GOOGLE_CRAWLER.get();
            } else if (isBrowserIntent(intent)) {
                return referrerFromBrowser(intent);
            } else {
                return Referrer.OTHER.get();
            }
        } catch(ClassCastException exception) {
            return Referrer.OTHER.get();
        }
    }

    public boolean isFacebookAction(Intent intent, Resources resources) {
        return getActionForSoundCloud(resources).equals(intent.getAction());
    }

    private boolean isOriginIntent(Intent intent) {
        return Strings.isNotBlank(getOrigin(intent));
    }

    private Referrer referrerFromOrigin(Intent intent) {
        String origin = getOrigin(intent);

        if (origin != null) {
            return Referrer.fromOrigin(origin);
        } else {
            return Referrer.OTHER;
        }
    }

    @Nullable
    private String getOrigin(Intent intent) {
        Uri data = intent.getData();

        if (data != null && !data.isOpaque()) {
            return data.getQueryParameter(PARAM_ORIGIN);
        } else {
            return null;
        }
    }

    private boolean isFacebookIntent(Intent intent, Resources resources) {
        if (isFacebookAction(intent, resources)) {
            return true;
        } else if (intent.hasExtra(EXTRA_FACEBOOK_APP_ID)) {
            long extraAppId = Long.parseLong(getFacebookAppId(resources), 10);
            return extraAppId == intent.getLongExtra(EXTRA_FACEBOOK_APP_ID, Consts.NOT_SET);
        } else {
            return false;
        }
    }

    @Nullable
    private String getBrowserReferrer(Intent intent) {
        Object maybeBundle = intent.getExtras().get(EXTRA_ANDROID_BROWSER_HEADERS);

        if (maybeBundle instanceof Bundle) {
            return ((Bundle) maybeBundle).getString(EXTRA_BROWSER_REFERER);
        } else {
            return null;
        }
    }

    private boolean isBrowserIntent(Intent intent) {
        return intent.hasExtra(EXTRA_ANDROID_BROWSER_HEADERS);
    }

    private String referrerFromBrowser(Intent intent) {
        return Referrer.fromUrl(getBrowserReferrer(intent));
    }

    private boolean isGooglePlusIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_BROWSER_APPLICATION_ID)) {
            String applicationId = intent.getStringExtra(EXTRA_BROWSER_APPLICATION_ID);
            if (applicationId != null) {
                if (GOOGLE_PLUS_PKG_NAME.equals(applicationId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isTwitterIntent(Intent intent) {
        ComponentName ancestorComponent = getAncestorComponent(intent);

        if (ancestorComponent != null) {
            if (TWITTER_PKG_NAME.equals(ancestorComponent.getPackageName())) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private ComponentName getAncestorComponent(Intent intent) {
        if (intent.hasExtra(EXTRA_INTENT_ANCESTOR)) {
            Object maybeIntent = intent.getExtras().get(EXTRA_INTENT_ANCESTOR);
            if (maybeIntent instanceof Intent) {
                return ((Intent) maybeIntent).getComponent();
            }
        }

        return null;
    }

    private String getActionForSoundCloud(Resources resources) {
        return FACEBOOK_PKG_NAME + getFacebookAppId(resources);
    }

    private String getFacebookAppId(Resources resources) {
        return resources.getString(R.string.production_facebook_app_id);
    }

    private boolean isGoogleCrawlerIntent(Intent intent) {
        Uri referrerUri = getAndroidReferrer(intent);

        return ANDROID_APP_SCHEME.equals(referrerUri.getScheme())
                && GOOGLE_APPCRAWLER_PACKAGE_NAME.equals(referrerUri.getHost());
    }

    private Uri getAndroidReferrer(Intent intent) {
        if (intent.hasExtra(ANDROID_INTENT_EXTRA_REFERRER)) {
            return intent.getParcelableExtra(ANDROID_INTENT_EXTRA_REFERRER);
        } else if (intent.hasExtra(ANDROID_INTENT_EXTRA_REFERRER_NAME)) {
            return Uri.parse(intent.getStringExtra(ANDROID_INTENT_EXTRA_REFERRER_NAME));
        } else {
            return Uri.EMPTY;
        }
    }
}
