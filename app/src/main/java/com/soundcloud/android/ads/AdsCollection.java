package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdsReceived;

public interface AdsCollection {

    AdsReceived toAdsReceived();

    String contentString();

}
