package com.soundcloud.android.utils;

import javax.inject.Inject;
import java.util.UUID;

public class UuidProvider {

    @Inject
    public UuidProvider() {
        // stab
    }

    public String getRandomUuid() {
        return UUID.randomUUID().toString();
    }

}
