package com.soundcloud.android.properties;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class LocalConfig {

    @Inject
    LocalConfig() {}

    boolean getFlagValue(Flag flag) {
        return flag.featureValue();
    }
}
