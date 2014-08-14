package com.soundcloud.android.view.RecyclingPager;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;

/**
 * https://github.com/JakeWharton/salvage/tree/master/salvage/src/main/java/com/jakewharton/salvage
 *
 * The RecycleBin facilitates reuse of views across layouts. The RecycleBin has two levels of
 * storage: ActiveViews and ScrapViews. ActiveViews are those views which were onscreen at the
 * start of a layout. By construction, they are displaying current information. At the end of
 * layout, all views in ActiveViews are demoted to ScrapViews. ScrapViews are old views that
 * could potentially be used by the adapter to avoid allocating views unnecessarily.
 * <p>
 * This class was taken from Android's implementation of {@link android.widget.AbsListView} which
 * is copyrighted 2006 The Android Open Source Project.
 */
public class RecycleBin {

    /** Unsorted views that can be used by the adapter as a convert view. */
    private SparseArray<View>[] scrapViews;

    private int viewTypeCount;

    private SparseArray<View> currentScrapViews;

    public void setViewTypeCount(int viewTypeCount) {
        if (viewTypeCount < 1) {
            throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
        }
        //noinspection unchecked
        SparseArray<View>[] scrapViews = new SparseArray[viewTypeCount];
        for (int i = 0; i < viewTypeCount; i++) {
            scrapViews[i] = new SparseArray<View>();
        }
        this.viewTypeCount = viewTypeCount;
        currentScrapViews = scrapViews[0];
        this.scrapViews = scrapViews;
    }

    /** @return A view from the ScrapViews collection. These are unordered. */
    View getScrapView(int position, int viewType) {
        if (viewTypeCount == 1) {
            return retrieveFromScrap(currentScrapViews, position);
        } else if (viewType >= 0 && viewType < scrapViews.length) {
            return retrieveFromScrap(scrapViews[viewType], position);
        }
        return null;
    }

    /**
     * Put a view into the ScrapViews list. These views are unordered.
     *
     * @param scrap The view to add
     */
    @TargetApi(14)
    void addScrapView(View scrap, int position, int viewType) {
        if (viewTypeCount == 1) {
            currentScrapViews.put(position, scrap);
        } else {
            scrapViews[viewType].put(position, scrap);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            scrap.setAccessibilityDelegate(null);
        }
    }

    static View retrieveFromScrap(SparseArray<View> scrapViews, int position) {
        int size = scrapViews.size();
        if (size > 0) {
            // See if we still have a view for this position.
            for (int i = 0; i < size; i++) {
                int fromPosition = scrapViews.keyAt(i);
                View view = scrapViews.get(fromPosition);
                if (fromPosition == position) {
                    scrapViews.remove(fromPosition);
                    return view;
                }
            }
            int index = size - 1;
            View r = scrapViews.valueAt(index);
            scrapViews.remove(scrapViews.keyAt(index));
            return r;
        } else {
            return null;
        }
    }
}