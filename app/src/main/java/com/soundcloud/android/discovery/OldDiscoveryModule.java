package com.soundcloud.android.discovery;

import dagger.Module;
import dagger.Provides;

import java.util.Random;

@Module
public class OldDiscoveryModule {

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
