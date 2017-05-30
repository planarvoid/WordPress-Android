package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.*;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.AdPlaybackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.support.annotation.Nullable;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class InlayAdHelper {

    final static int EARLIEST_POSITION_FOR_AD = 4;
    final static int MIN_DISTANCE_BETWEEN_ADS = 4;
    final static int MAX_SEARCH_DISTANCE = 5;

    private final static float MINIMUM_VIDEO_VIEWABLE_PERCENTAGE = 50.0f;

    private final StaggeredGridLayoutManager layoutManager;
    private final StreamAdapter adapter;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;
    private final VideoAdItemRenderer videoAdItemRenderer;

    @VisibleForTesting int minimumVisibleIndex = -1;
    @VisibleForTesting int maximumVisibleIndex = -1;

    InlayAdHelper(StaggeredGridLayoutManager layoutManager,
                  StreamAdapter adapter,
                  @Provided CurrentDateProvider dateProvider,
                  @Provided EventBus eventBus) {
        this.layoutManager = layoutManager;
        this.adapter = adapter;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
        this.videoAdItemRenderer = adapter.getVideoAdItemRenderer();
    }

    Subscription subscribe() {
        return eventBus.queue(EventQueue.AD_PLAYBACK)
                       .filter(AdPlaybackEvent::forStateTransition)
                       .cast(AdPlayStateTransition.class)
                       .subscribe(new InlayStateTransitionSubscriber());
    }

    boolean insertAd(AdData ad, boolean wasScrollingUp) {
        final int position = wasScrollingUp ? findValidAdPosition(firstVisibleItemPosition(), -1)
                                            : findValidAdPosition(lastVisibleItemPosition(), 1);
        if (position != Consts.NOT_SET) {
            final Optional<StreamItem> streamItem = itemForAd(ad);
            if (streamItem.isPresent()) {
                adapter.addItem(position, streamItem.get());
                return true;
            }
        }
        return false;
    }

    private Optional<StreamItem> itemForAd(AdData ad) {
        if (ad instanceof AppInstallAd) {
            return Optional.of(StreamItem.forAppInstall((AppInstallAd) ad));
        } else if (ad instanceof VideoAd) {
            return Optional.of(StreamItem.forVideoAd((VideoAd) ad));
        } else {
            return Optional.absent();
        }
    }

    // Called when recycler view becomes focused or scrolled
    void onChangeToAdsOnScreen(boolean shouldRebindVideoViews) {
        final Date now = dateProvider.getCurrentDate();
        minimumVisibleIndex = firstVisibleItemPosition();
        maximumVisibleIndex = lastVisibleItemPosition();
        forAdsOnScreen(now, adsOnScreenWithPosition(), shouldRebindVideoViews);
    }

    private void forAdsOnScreen(Date now, List<Pair<Integer, AdData>> adsOnScreenWithPosition, boolean shouldRebindVideoViews) {
        Optional<VideoOnScreen> mostViewableVideo = Optional.absent();

        for (Pair<Integer, AdData> positionAndAd : adsOnScreenWithPosition) {
            final AdData adData = positionAndAd.second();
            if (adData instanceof AppInstallAd) {
                forAppInstallOnScreen(now, positionAndAd.first(), (AppInstallAd) adData);
            } else if (adData instanceof VideoAd) {
                final View itemView = layoutManager.findViewByPosition(positionAndAd.first());
                mostViewableVideo = mostViewableVideoOnScreen(mostViewableVideo, positionAndAd, itemView);
                if (shouldRebindVideoViews) {
                    bindVideoSurface(itemView, (VideoAd) adData);
                }
            }
        }

        if (mostViewableVideo.isPresent()) {
            final VideoOnScreen videoOnScreen = mostViewableVideo.get();
            publishInlayAdEvent(InlayAdEvent.forOnScreen(videoOnScreen.position(), videoOnScreen.adData(), now));
        } else {
            publishInlayAdEvent(NoVideoOnScreen.create(now, true));
        }
    }

    private void bindVideoSurface(@Nullable View itemView, VideoAd adData) {
        if (itemView != null) {
            videoAdItemRenderer.bindVideoSurface(itemView, adData);
        }
    }

    private Optional<VideoOnScreen> mostViewableVideoOnScreen(Optional<VideoOnScreen> currentMostViewable, Pair<Integer, AdData> currentAd, View itemView) {
        final float viewablePercentage = viewablePercentageForVideoView(itemView);
        final VideoOnScreen currentVideo = VideoOnScreen.create(viewablePercentage, currentAd.first(), (VideoAd) currentAd.second());

        if (viewablePercentage > MINIMUM_VIDEO_VIEWABLE_PERCENTAGE && currentVideo.isMoreViewable(currentMostViewable)) {
            return Optional.of(currentVideo);
        } else {
            return currentMostViewable;
        }
    }

    private float viewablePercentageForVideoView(@Nullable View itemView) {
        return itemView != null ? ViewUtils.calculateViewablePercentage(videoAdItemRenderer.getVideoView(itemView))
                                : 0.0f;
    }

    private void forAppInstallOnScreen(Date now, int position, AppInstallAd adData) {
        if (!adData.hasReportedImpression()) {
            publishInlayAdEvent(InlayAdEvent.forOnScreen(position, adData, now));
        }
    }

    private void publishInlayAdEvent(AdPlaybackEvent event) {
       eventBus.publish(EventQueue.AD_PLAYBACK, event);
    }

    public boolean isOnScreen(int position) {
        return 0 <= minimumVisibleIndex && minimumVisibleIndex <= position && position <= maximumVisibleIndex;
    }

    private List<Pair<Integer, AdData>> adsOnScreenWithPosition() {
        return adsInRangeWithPosition(firstVisibleItemPosition(), lastVisibleItemPosition());
    }

    private List<Pair<Integer, AdData>> adsInRangeWithPosition(int minInclusive, int maxInclusive) {
        final List<Pair<Integer, AdData>> result = new ArrayList<>(AdConstants.MAX_INLAYS_ON_SCREEN);
        final int startInclusive = Math.max(minInclusive, 0);
        final int endInclusive = Math.min(maxInclusive, getNumberOfStreamItems() - 1);

        for (int position = startInclusive; position <= endInclusive; position++) {
            if (position != Consts.NOT_SET) {
                final Optional<AdData> data = adapter.getItem(position).getAdData();
                if (data.isPresent()) {
                    result.add(Pair.of(position, data.get()));
                }
            }
        }
        return result;
    }

    private int lastVisibleItemPosition() {
        int lastVisible = Consts.NOT_SET;
        final int[] spansArray = new int[layoutManager.getSpanCount()];

        for (int positionForSpan : layoutManager.findLastVisibleItemPositions(spansArray)) {
            lastVisible = Math.max(lastVisible, positionForSpan);
        }
        return lastVisible;
    }

    private int firstVisibleItemPosition() {
        int firstVisible = Consts.NOT_SET;
        final int[] spansArray = new int[layoutManager.getSpanCount()];

        for (int positionForSpan : layoutManager.findFirstVisibleItemPositions(spansArray)) {
            firstVisible = firstVisible >= 0 ? Math.min(firstVisible, positionForSpan)
                                             : positionForSpan;
        }
        return firstVisible;
    }

    private int findValidAdPosition(int basePosition, int increment) {
        int possiblePosition = basePosition + increment;

        while (Math.abs(basePosition - possiblePosition) < MAX_SEARCH_DISTANCE) {
            if (isValidAdPosition(possiblePosition)) {
                return possiblePosition;
            }
            possiblePosition += increment;
        }
        return Consts.NOT_SET;
    }

    private boolean isValidAdPosition(int position) {
        return position > EARLIEST_POSITION_FOR_AD &&
                position < getNumberOfStreamItems() &&
                isNotUpsell(position) &&
                hasNoAdsWithinMinDistance(position);
    }

    private boolean hasNoAdsWithinMinDistance(int position) {
        int lowerBound = Math.max(0, position - MIN_DISTANCE_BETWEEN_ADS);
        int upperBound = Math.min(position + MIN_DISTANCE_BETWEEN_ADS, getNumberOfStreamItems() - 1);

        for (int i = lowerBound; i <= upperBound; i++)  {
           if (adapter.getItem(i).isAd()) {
               return false;
           }
        }
        return true;
    }

    private boolean isNotUpsell(int position) {
        return !adapter.getItem(position).isUpsell();
    }

    private int getNumberOfStreamItems() {
        return adapter.getItems().size();
    }

    @AutoValue
    abstract static class VideoOnScreen {

        abstract int position();
        abstract float viewablePercentage();
        abstract VideoAd adData();

        static VideoOnScreen create(float viewablePercentage, int position, VideoAd videoAd) {
            return new AutoValue_InlayAdHelper_VideoOnScreen(position, viewablePercentage, videoAd);
        }

        boolean isMoreViewable(Optional<VideoOnScreen> that) {
            return this.viewablePercentage() > (that.isPresent() ? that.get().viewablePercentage() : 0.0f);
        }
    }

    private class InlayStateTransitionSubscriber extends DefaultSubscriber<AdPlayStateTransition> {

        final HashMap<VideoAd, Integer> positionCache = new HashMap<>(AdConstants.MAX_INLAYS_ON_SCREEN);

        @Override
        public void onNext(AdPlayStateTransition event) {
            final VideoAd video = event.videoAd();

            if (positionCache.containsKey(video)) {
                if (isVideoAdAtPosition(positionCache.get(video), video)) {
                    // Cache position is still accurate
                    forwardTransitionToPresenter(event, positionCache.get(video));
                } else {
                    positionCache.remove(video);
                    searchAndForwardTransition(event, video);
                }
            } else {
                searchAndForwardTransition(event, video);
            }
        }

        private void searchAndForwardTransition(AdPlayStateTransition transition, VideoAd video) {
            final boolean cacheSuccessfullyUpdated = updatePositionCache(video);
            if (cacheSuccessfullyUpdated) {
               forwardTransitionToPresenter(transition, positionCache.get(video));
            }
        }

        private void forwardTransitionToPresenter(AdPlayStateTransition transition, int position) {
            final View videoView = layoutManager.findViewByPosition(position);
            if (videoView != null) {
                videoAdItemRenderer.setPlayState(videoView, transition.stateTransition(), transition.isMuted());
            }
        }

        private boolean updatePositionCache(VideoAd video) {
            for (int i = firstVisibleItemPosition(); i <= lastVisibleItemPosition(); i++) {
                if (isVideoAdAtPosition(i, video)) {
                    positionCache.put(video, i);
                    return true;
                }
            }
            return false;
        }

        private boolean isVideoAdAtPosition(int position, VideoAd videoAd) {
            if (position > 0 && position < getNumberOfStreamItems()) {
                final StreamItem item = adapter.getItem(position);
                if (item instanceof StreamItem.Video) {
                    final StreamItem.Video videoItem = (StreamItem.Video) item;
                    return videoItem.video().equals(videoAd);
                }
            }
            return false;
        }
    }
}
