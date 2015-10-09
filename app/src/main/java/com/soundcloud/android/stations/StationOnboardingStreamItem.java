package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.NotificationItem;

public class StationOnboardingStreamItem extends NotificationItem {
    public static final Urn URN = new Urn("soundcloud:notifications:stations-onboarding");

    @Override
    public Urn getEntityUrn() {
        return URN;
    }

}
