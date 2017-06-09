package com.soundcloud.android.analytics;

import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import io.reactivex.functions.Function;

import javax.inject.Inject;

public class EngagementsTracking {

    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final EventTracker eventTracker;

    private static Function<Track, UIEvent> likeEventFromTrack(final Urn trackUrn, final boolean addLike,
                                                               final EventContextMetadata eventMetadata,
                                                               final PromotedSourceInfo promotedSourceInfo) {
        return track -> UIEvent.fromToggleLike(addLike,
                                               trackUrn,
                                               eventMetadata,
                                               promotedSourceInfo,
                                               EntityMetadata.from(track));
    }

    private static Function<User, UIEvent> followEventFromUser(final boolean isFollow,
                                                               final EventContextMetadata eventContextMetadata) {
        return user -> UIEvent.fromToggleFollow(isFollow,
                                                EntityMetadata.fromUser(user),
                                                eventContextMetadata);
    }

    @Inject
    public EngagementsTracking(TrackRepository trackRepository,
                               UserRepository userRepository,
                               EventTracker eventTracker) {
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
        this.eventTracker = eventTracker;
    }

    public void likeTrackUrn(Urn trackUrn, boolean addLike, EventContextMetadata eventMetadata,
                             PromotedSourceInfo promotedSourceInfo) {

        trackRepository.track(trackUrn)
                       .map(likeEventFromTrack(trackUrn, addLike, eventMetadata, promotedSourceInfo))
                       .toObservable()
                       .subscribe(eventTracker.trackEngagementSubscriber());
    }

    public void followUserUrn(Urn userUrn, boolean isFollow, EventContextMetadata eventContextMetadata) {
        userRepository.userInfo(userUrn)
                      .map(followEventFromUser(isFollow, eventContextMetadata))
                      .toObservable()
                      .subscribe(eventTracker.trackEngagementSubscriber());
    }
}
