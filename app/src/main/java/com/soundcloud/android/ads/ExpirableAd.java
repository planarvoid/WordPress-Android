package com.soundcloud.android.ads;

interface ExpirableAd {

    long createdAt();

    int expiryInMins();

}
