package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.NotificationItem;

public class SuggestedCreatorsNotificationItem extends NotificationItem {
    public static final Urn URN = new Urn("soundcloud:notifications:suggested-creators");

    @Override
    public Urn getUrn() {
        return URN;
    }

}
