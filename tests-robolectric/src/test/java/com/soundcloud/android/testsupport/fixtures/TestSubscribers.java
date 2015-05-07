package com.soundcloud.android.testsupport.fixtures;

import static org.mockito.Mockito.mock;

import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;

import android.content.Context;

import javax.inject.Provider;

public class TestSubscribers {

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber() {
        return new Provider<ExpandPlayerSubscriber>() {
            @Override
            public ExpandPlayerSubscriber get() {
                return new ExpandPlayerSubscriber(new TestEventBus(), mock(PlaybackToastHelper.class));
            }
        };
    }

}
