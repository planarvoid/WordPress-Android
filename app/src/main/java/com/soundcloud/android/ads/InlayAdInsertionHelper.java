package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.Consts;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;

class InlayAdInsertionHelper {

    final static int EARLIEST_POSITION_FOR_AD = 4;
    final static int MIN_DISTANCE_BETWEEN_ADS = 4;
    final static int MAX_SEARCH_DISTANCE = 5;

    private final StaggeredGridLayoutManager layoutManager;
    private final StreamAdapter adapter;
    private final int[] spansArray;

    InlayAdInsertionHelper(StaggeredGridLayoutManager layoutManager, StreamAdapter adapter) {
        this.layoutManager = layoutManager;
        this.adapter = adapter;
        spansArray = new int[layoutManager.getSpanCount()];
    }

    public boolean insertAd(AppInstallAd ad, boolean wasScrollingUp) {
        final int position = wasScrollingUp ? findValidAdPosition(firstVisibleItemPosition(), -1)
                                            : findValidAdPosition(lastVisibleItemPosition(), 1);
        if (position != Consts.NOT_SET) {
            adapter.addItem(position, StreamItem.forAppInstall(ad));
            return true;
        }
        return false;
    }

    private int lastVisibleItemPosition() {
        int lastVisible = Consts.NOT_SET;

        for (int positionForSpan : layoutManager.findLastVisibleItemPositions(spansArray)) {
            lastVisible = Math.max(lastVisible, positionForSpan);
        }
        return lastVisible;
    }

    private int firstVisibleItemPosition() {
        int firstVisible = Consts.NOT_SET;

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
