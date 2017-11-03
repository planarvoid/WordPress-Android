package com.soundcloud.android.testsupport.fixtures;

import static org.mockito.Mockito.mock;

import com.soundcloud.android.playback.ExpandPlayerCommand;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;

import javax.inject.Provider;

public class TestSubscribers {

    public static Provider<ExpandPlayerSingleObserver> expandPlayerObserver() {
        return () -> new ExpandPlayerSingleObserver(mock(ExpandPlayerCommand.class));
    }

}
