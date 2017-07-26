package com.soundcloud.android.deeplinks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.ads.AdsStorage;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.utils.ErrorUtils;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;

public class ResolveActivity extends RootActivity {

    @Inject ReferrerResolver referrerResolver;
    @Inject Navigator navigator;
    @Inject AdsStorage adsStorage;

    public ResolveActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static boolean accept(Uri data, Resources resources) {
        return DeepLink.SOUNDCLOUD_SCHEME.equalsIgnoreCase(data.getScheme())
                || (data.getHost() != null && data.getHost().contains(resources.getString(R.string.host_name)));
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Uri uri = getIntent().getData();
        String referrer = null;
        try {
            referrer = referrerResolver.getReferrerFromIntent(getIntent(), getResources());
        } catch (UriResolveException e) {
            ErrorUtils.handleSilentException(e);
        }
        adsStorage.preventPrestitialFetchForTimeInterval(); // prevent prestitial from launching when DeepLink
        navigator.navigateTo(NavigationTarget.forExternalDeeplink(uri.toString(), referrer));
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
