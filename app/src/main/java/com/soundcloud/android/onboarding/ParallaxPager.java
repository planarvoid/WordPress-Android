package com.soundcloud.android.onboarding;

import static com.soundcloud.android.utils.ViewUtils.allChildViewsOf;

import com.soundcloud.android.view.SafeViewPager;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParallaxPager extends SafeViewPager {

    private final List<ParallaxInfo> parallaxViews = new ArrayList<ParallaxInfo>();

    public ParallaxPager(Context context) {
        super(context);
    }

    public ParallaxPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public void setAdapter(final PagerAdapter adapter) {
        PagerAdapter adapterWrapper = new PagerAdapter() {
            @Override
            public int getCount() {
                return adapter.getCount();
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return adapter.isViewFromObject(view, o);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                Object page = adapter.instantiateItem(container, position);

                if (page instanceof ViewGroup) {
                    for (View view : allChildViewsOf((ViewGroup) page)) {
                        if ("parallax".equals(view.getTag())) {
                            parallaxViews.add(new ParallaxInfo(view, position));
                        }
                    }
                }

                return page;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                adapter.destroyItem(container, position, object);

                Set<ParallaxInfo> toRemove = new HashSet<ParallaxInfo>();

                for (ParallaxInfo info : parallaxViews) {
                    if (info.page == position) {
                        toRemove.add(info);
                    }
                }

                parallaxViews.removeAll(toRemove);
            }
        };

        super.setAdapter(adapterWrapper);
    }

    @Override
    @TargetApi(11)
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            for (ParallaxInfo info : parallaxViews) {
                float targetX = info.page * getWidth();
                float actualX = position * getWidth() + offsetPixels;

                info.view.setTranslationX(targetX - actualX);
            }
        }
    }

    class ParallaxInfo {
        public final View view;
        public final int page;

        ParallaxInfo(View view, int page) {
            this.view = view;
            this.page = page;
        }
    }
}
