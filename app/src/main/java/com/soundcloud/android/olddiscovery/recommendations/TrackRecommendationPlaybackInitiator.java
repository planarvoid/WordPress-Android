package com.soundcloud.android.olddiscovery.recommendations;

import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.byKind;
import static com.soundcloud.android.olddiscovery.recommendations.Recommendation.TO_TRACK_URN;
import static com.soundcloud.android.olddiscovery.recommendations.RecommendedTracksBucketItem.TO_RECOMMENDATIONS;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Iterables.find;
import static com.soundcloud.java.collections.Iterables.indexOf;
import static com.soundcloud.java.collections.Iterables.transform;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.java.functions.Predicate;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class TrackRecommendationPlaybackInitiator {
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;

    @Inject
    public TrackRecommendationPlaybackInitiator(Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                                PlaybackInitiator playbackInitiator) {
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.playbackInitiator = playbackInitiator;
    }

    @SuppressWarnings("unchecked")
    public void playFromReason(Urn seedUrn, Screen trackingScreen, List<OldDiscoveryItem> items) {
        int bucketPosition = indexOf(items, isForSeed(seedUrn));
        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) items.get(bucketPosition);
        final PlaySessionSource playSessionSource = PlaySessionSource.forRecommendations(
                trackingScreen, bucket.seedTrackQueryPosition(), bucket.queryUrn());

        final List<Urn> backQueue = discoveryItemsToRecommendedTrackUrns(items.subList(0, bucketPosition));
        final List<Urn> seed = singletonList(seedUrn);
        final List<Urn> forwardQueue = discoveryItemsToRecommendedTrackUrns(items.subList(bucketPosition,
                                                                                          items.size()));
        final List<Urn> playQueue = newArrayList(concat(backQueue, seed, forwardQueue));
        final int playPosition = playQueue.indexOf(bucket.seedTrackUrn());

        RxJava.toV1Observable(playbackInitiator.playTracks(playQueue, playPosition, playSessionSource))
              .subscribe(expandPlayerSubscriberProvider.get());

    }

    public void playFromRecommendation(Urn seedUrn, Urn trackUrn, Screen trackingScreen, List<OldDiscoveryItem> items) {
        final RecommendedTracksBucketItem bucket = (RecommendedTracksBucketItem) find(items, isForSeed(seedUrn));
        final Recommendation recommendation = find(bucket.recommendations(), isForTrack(trackUrn));
        final PlaySessionSource playSessionSource = PlaySessionSource.forRecommendations(
                trackingScreen, recommendation.getQueryPosition(), recommendation.getQueryUrn());

        final List<Urn> playQueue = discoveryItemsToRecommendedTrackUrns(items);
        final int playPosition = playQueue.indexOf(recommendation.getTrackUrn());

        RxJava.toV1Observable(playbackInitiator.playTracks(playQueue, playPosition, playSessionSource))
              .subscribe(expandPlayerSubscriberProvider.get());
    }

    private List<Urn> discoveryItemsToRecommendedTrackUrns(List<OldDiscoveryItem> oldDiscoveryItems) {
        Iterable<OldDiscoveryItem> buckets = filter(oldDiscoveryItems, byKind(Kind.RecommendedTracksItem));
        Iterable<Recommendation> recommendations = concat(transform(buckets, TO_RECOMMENDATIONS));
        return newArrayList(transform(recommendations, TO_TRACK_URN));
    }

    private static Predicate<OldDiscoveryItem> isForSeed(final Urn seedUrn) {
        return input -> input.getKind() == Kind.RecommendedTracksItem && ((RecommendedTracksBucketItem) input).seedTrackUrn() == seedUrn;
    }

    private Predicate<Recommendation> isForTrack(final Urn trackUrn) {
        return input -> input.getTrackUrn() == trackUrn;
    }
}
