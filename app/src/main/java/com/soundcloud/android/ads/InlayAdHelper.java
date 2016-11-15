package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class InlayAdHelper {

    final static int EARLIEST_POSITION_FOR_AD = 4;
    final static int MIN_DISTANCE_BETWEEN_ADS = 4;
    final static int MAX_SEARCH_DISTANCE = 5;

    private final StreamAdapter adapter;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;

    @VisibleForTesting
    int minimumVisibleIndex = -1;
    @VisibleForTesting
    int maximumVisibleIndex = -1;

    @Inject
    InlayAdHelper(StreamAdapter adapter, CurrentDateProvider dateProvider, EventBus eventBus) {
        this.adapter = adapter;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
    }

    public boolean insertAd(StaggeredGridLayoutManager layoutManager, AppInstallAd ad, boolean wasScrollingUp) {
        final int position = wasScrollingUp ? findValidAdPosition(firstVisibleItemPosition(layoutManager), -1)
                                            : findValidAdPosition(lastVisibleItemPosition(layoutManager), 1);
        if (position != Consts.NOT_SET) {
            adapter.addItem(position, StreamItem.forAppInstall(ad));
            return true;
        }
        return false;
    }

    public void onScroll(StaggeredGridLayoutManager layoutManager) {
        final Date now = dateProvider.getCurrentDate();

        minimumVisibleIndex = firstVisibleItemPosition(layoutManager);
        maximumVisibleIndex = lastVisibleItemPosition(layoutManager);

        for (Pair<Integer, AppInstallAd> positionAndAd : adsOnScreenWithPosition(layoutManager)) {
            final AppInstallAd ad = positionAndAd.second();

            if (!ad.hasReportedImpression()) {
                eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(positionAndAd.first(), ad, now));
            }
        }
    }

    public boolean isOnScreen(int position) {
        return 0 <= minimumVisibleIndex && minimumVisibleIndex <= position && position <= maximumVisibleIndex;
    }

    private List<Pair<Integer, AppInstallAd>> adsOnScreenWithPosition(StaggeredGridLayoutManager layoutManager) {
        return adsInRangeWithPosition(firstVisibleItemPosition(layoutManager), lastVisibleItemPosition(layoutManager));
    }

    private List<Pair<Integer, AppInstallAd>> adsInRangeWithPosition(int minInclusive, int maxInclusive) {
        final List<Pair<Integer, AppInstallAd>> result = new ArrayList<>(1); // expect at most one ad on screen
        final int startInclusive = Math.max(minInclusive, 0);
        final int endInclusive = Math.min(maxInclusive, getNumberOfStreamItems() - 1);

        for (int position = startInclusive; position <= endInclusive; position++) {
            if (position != Consts.NOT_SET && adapter.getItem(position).isAd()) {
                result.add(Pair.of(position, ((StreamItem.AppInstall) adapter.getItem(position)).appInstall()));
            }
        }

        return result;
    }

    private int lastVisibleItemPosition(StaggeredGridLayoutManager layoutManager) {
        int lastVisible = Consts.NOT_SET;
        final int[] spansArray = new int[layoutManager.getSpanCount()];

        for (int positionForSpan : layoutManager.findLastVisibleItemPositions(spansArray)) {
            lastVisible = Math.max(lastVisible, positionForSpan);
        }
        return lastVisible;
    }

    private int firstVisibleItemPosition(StaggeredGridLayoutManager layoutManager) {
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
}
