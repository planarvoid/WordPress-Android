package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdRequestEvent.AdsReceived;

public interface AdsCollection {

    AdsReceived toAdsReceived();

    String contentString();

}
