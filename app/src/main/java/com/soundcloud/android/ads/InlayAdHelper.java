package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class InlayAdHelper {

    final static int EARLIEST_POSITION_FOR_AD = 4;
    final static int MIN_DISTANCE_BETWEEN_ADS = 4;
    final static int MAX_SEARCH_DISTANCE = 5;

    private final static float MINIMUM_VIDEO_VIEWABLE_PERCENTAGE = 50.0f;

    private final StaggeredGridLayoutManager layoutManager;
    private final StreamAdapter adapter;
    private final VideoAdItemRenderer videoAdItemRenderer;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;

    @VisibleForTesting int minimumVisibleIndex = -1;
    @VisibleForTesting int maximumVisibleIndex = -1;

    InlayAdHelper(StaggeredGridLayoutManager layoutManager,
                  StreamAdapter adapter,
                  @Provided VideoAdItemRenderer videoAdItemRenderer,
                  @Provided CurrentDateProvider dateProvider,
                  @Provided EventBus eventBus) {
        this.layoutManager = layoutManager;
        this.adapter = adapter;
        this.videoAdItemRenderer = videoAdItemRenderer;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
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

    public void onScroll() {
        final Date now = dateProvider.getCurrentDate();
        minimumVisibleIndex = firstVisibleItemPosition();
        maximumVisibleIndex = lastVisibleItemPosition();
        forAdsOnScreen(now, adsOnScreenWithPosition());
    }

    private void forAdsOnScreen(Date now, List<Pair<Integer, AdData>> adsOnScreenWithPosition) {
        Optional<VideoOnScreen> mostViewableVideo = Optional.absent();

        for (Pair<Integer, AdData> positionAndAd : adsOnScreenWithPosition) {
            final AdData adData = positionAndAd.second();
            if (adData instanceof AppInstallAd) {
                forAppInstallOnScreen(now, positionAndAd.first(), (AppInstallAd) adData);
            } else if (adData instanceof VideoAd) {
                mostViewableVideo = mostViewableVideoOnScreen(mostViewableVideo, positionAndAd);
            }
        }

        if (mostViewableVideo.isPresent()) {
            final VideoOnScreen videoOnScreen = mostViewableVideo.get();
            publishInlayAdEvent(InlayAdEvent.OnScreen.create(videoOnScreen.position(), videoOnScreen.adData(), now));
        } else {
            publishInlayAdEvent(InlayAdEvent.NoVideoOnScreen.create(now));
        }
    }

    private Optional<VideoOnScreen> mostViewableVideoOnScreen(Optional<VideoOnScreen> currentMostViewable, Pair<Integer, AdData> currentAd) {
        final float viewablePercentage = viewablePercentageForVideoView(currentAd.first());
        final VideoOnScreen currentVideo = VideoOnScreen.create(viewablePercentage, currentAd.first(), (VideoAd) currentAd.second());

        if (viewablePercentage > MINIMUM_VIDEO_VIEWABLE_PERCENTAGE && currentVideo.isMoreViewable(currentMostViewable)) {
            return Optional.of(currentVideo);
        } else {
            return currentMostViewable;
        }
    }

    private float viewablePercentageForVideoView(int position) {
        final View videoView = videoAdItemRenderer.getVideoView(layoutManager.findViewByPosition(position));
        return ViewUtils.calculateViewablePercentage(videoView);
    }

    private void forAppInstallOnScreen(Date now, int position, AppInstallAd adData) {
        if (!adData.hasReportedImpression()) {
            publishInlayAdEvent(InlayAdEvent.OnScreen.create(position, adData, now));
        }
    }

    private void publishInlayAdEvent(InlayAdEvent event) {
       eventBus.publish(EventQueue.INLAY_AD, event);
    }

    public boolean isOnScreen(int position) {
        return 0 <= minimumVisibleIndex && minimumVisibleIndex <= position && position <= maximumVisibleIndex;
    }

    private List<Pair<Integer, AdData>> adsOnScreenWithPosition() {
        return adsInRangeWithPosition(firstVisibleItemPosition(), lastVisibleItemPosition());
    }

    private List<Pair<Integer, AdData>> adsInRangeWithPosition(int minInclusive, int maxInclusive) {
        final List<Pair<Integer, AdData>> result = new ArrayList<>(3);
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
}