package com.soundcloud.android.discovery;

import dagger.Module;
import dagger.Provides;

import java.util.Random;

@Module
public class DiscoveryModule {

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
