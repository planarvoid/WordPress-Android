package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

public class ActivityReferringEventProvider extends DefaultActivityLightCycle<Activity> {
    private final ReferringEventProvider referringEventProvider;

    @Inject
    public ActivityReferringEventProvider(ReferringEventProvider referringEventProvider) {
        this.referringEventProvider = referringEventProvider;
    }

    @Override
    public void onCreate(Activity activity, @Nullable Bundle bundle) {
        referringEventProvider.setupReferringEvent();
    }

    @Override
    public void onRestoreInstanceState(Activity activity, Bundle bundle) {
        referringEventProvider.restoreReferringEvent(bundle);
    }

    @Override
    public void onSaveInstanceState(Activity activity, Bundle bundle) {
        referringEventProvider.saveReferringEvent(bundle);
    }

    public Optional<ReferringEvent> getReferringEvent() {
        return referringEventProvider.getReferringEvent();
    }
}
