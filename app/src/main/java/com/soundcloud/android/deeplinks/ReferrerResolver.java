package com.soundcloud.android.deeplinks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.utils.ScTextUtils;

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

    private static final String PARAM_ORIGIN = "origin";

    @Inject
    ReferrerResolver() {
        // for dagger
    }

    public Referrer getReferrerFromIntent(Intent intent, Resources resources) {
        if (isOriginIntent(intent)) {
            return referrerFromOrigin(intent);
        } else if (isFacebookIntent(intent, resources)) {
            return Referrer.FACEBOOK;
        } else if (isTwitterIntent(intent)) {
            return Referrer.TWITTER;
        } else if (isGooglePlusIntent(intent)) {
            return Referrer.GOOGLE_PLUS;
        } else if (isBrowserIntent(intent)) {
            return referrerFromBrowser(intent);
        } else {
            return Referrer.OTHER;
        }
    }

    public boolean isFacebookAction(Intent intent, Resources resources) {
        return getActionForSoundCloud(resources).equals(intent.getAction());
    }

    private boolean isOriginIntent(Intent intent) {
        return ScTextUtils.isNotBlank(getOrigin(intent));
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

        if (data != null) {
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

    private Referrer referrerFromBrowser(Intent intent) {
        String browserReferrer = getBrowserReferrer(intent);

        if (browserReferrer != null) {
            Uri uri = Uri.parse(browserReferrer);
            String host = uri.getHost();

            if (host != null) {
                return Referrer.fromHost(uri.getHost());
            }
        }

        return Referrer.OTHER;
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
}
