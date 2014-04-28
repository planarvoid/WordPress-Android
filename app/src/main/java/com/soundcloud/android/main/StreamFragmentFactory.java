package com.soundcloud.android.main;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamFragment;

import android.net.Uri;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

class StreamFragmentFactory {

    private final FeatureFlags featureFlags;

    @Inject
    public StreamFragmentFactory(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public Fragment create(boolean onboardingResult) {
        if (featureFlags.isEnabled(Feature.NEW_STREAM)) {
            return new SoundStreamFragment();
        } else {
            final Uri contentUri = onboardingResult ?
                    Content.ME_SOUND_STREAM.uri :
                    Content.ME_SOUND_STREAM.uri.buildUpon()
                            .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
            return ScListFragment.newInstance(contentUri, R.string.side_menu_stream, Screen.SIDE_MENU_STREAM);
        }
    }

}
