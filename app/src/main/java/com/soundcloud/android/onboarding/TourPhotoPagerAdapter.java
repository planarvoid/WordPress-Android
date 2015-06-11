package com.soundcloud.android.onboarding;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.soundcloud.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.soundcloud.android.utils.AnimUtils.hideView;
import static com.soundcloud.android.utils.AnimUtils.showView;
import static com.soundcloud.android.utils.ViewUtils.allChildViewsOf;

class TourPhotoPagerAdapter extends PagerAdapter {
    private static final String FOREGROUND_TAG = "foreground";
    private static final String PARALLAX_TAG = "parallax";

    private List<TourLayout> photoPages;

    TourPhotoPagerAdapter(Context hostActivity) {
        photoPages = new ArrayList<>();
        photoPages.add(new TourLayout(hostActivity, R.layout.tour_page_1, R.drawable.tour_image_1));
        photoPages.add(new TourLayout(hostActivity, R.layout.tour_page_2, R.drawable.tour_image_2));
        photoPages.add(new TourLayout(hostActivity, R.layout.tour_page_3, R.drawable.tour_image_3));

        // randomize for variety
        Collections.shuffle(photoPages);
    }

    @Override
    public int getCount() {
        return photoPages.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = photoPages.get(position);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        container.addView(v);
        return v;
    }

    @Override
    public void destroyItem(View collection, int position, Object view) {
        ((ViewPager) collection).removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return object == view;
    }

    public void load(Context context, PhotoLoadHandler handler) {
        TourLayout.load(context, photoPages.toArray(new TourLayout[photoPages.size()]));
        photoPages.get(0).setLoadHandler(handler);
    }

    public void onDestroy() {
        for (TourLayout layout : photoPages) {
            layout.recycle();
        }
    }

    public void hideViewsOfLayout(Context context, int currentItem) {
        final TourLayout tourLayout = photoPages.get(currentItem);
        for (View view : allChildViewsOf(tourLayout)) {
            if (isForegroundView(view)) {
                showView(context, view, false);
            }
        }
    }

    public void showViewsOfLayout(Context context, int currentItem, boolean animated) {
        final TourLayout tourLayout = photoPages.get(currentItem);
        for (View view : allChildViewsOf(tourLayout)) {
            if (isForegroundView(view)) {
                hideView(context, view, animated);
            }
        }
    }

    private static boolean isForegroundView(View view) {
        final Object tag = view.getTag();
        return FOREGROUND_TAG.equals(tag) || PARALLAX_TAG.equals(tag);
    }
}
