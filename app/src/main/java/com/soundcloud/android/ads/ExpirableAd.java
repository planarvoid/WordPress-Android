package com.soundcloud.android.ads;

interface ExpirableAd {

    long getCreatedAt();

    int getExpiryInMins();

}
