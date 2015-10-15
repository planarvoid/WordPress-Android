package com.soundcloud.android.analytics;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import javax.inject.Inject;

public class EngagementsTracking {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;

    @Inject
    public EngagementsTracking(EventBus eventBus, TrackRepository trackRepository) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
    }

    public void likeTrackUrn(Urn trackUrn, boolean addLike, String invokerScreen, String contextScreen,
                             String pageName, Urn pageUrn, PromotedSourceInfo promotedSourceInfo) {

        trackRepository.track(trackUrn)
                .map(likeEventFromTrack(trackUrn, addLike, invokerScreen, contextScreen,
                        pageName, pageUrn, promotedSourceInfo))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private Func1<PropertySet, UIEvent> likeEventFromTrack(final Urn trackUrn, final boolean addLike,
                                                           final String invokerScreen, final String contextScreen,
                                                           final String pageName, final Urn pageUrn,
                                                           final PromotedSourceInfo promotedSourceInfo) {
        return new Func1<PropertySet, UIEvent>() {
            @Override
            public UIEvent call(PropertySet track) {
                return UIEvent.fromToggleLike(addLike, invokerScreen, contextScreen, pageName,
                        trackUrn, pageUrn, promotedSourceInfo, PlayableMetadata.from(track));

            }
        };
    }

}
