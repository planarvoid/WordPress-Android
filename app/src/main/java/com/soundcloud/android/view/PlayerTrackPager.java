package com.soundcloud.android.view;

import com.soundcloud.android.adapter.player.PlayerTrackPagerAdapter;
import com.soundcloud.android.view.play.PlayerTrackView;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class PlayerTrackPager extends ViewPager {
    private @Nullable OnTrackPageListener mTrackPagerScrollListener;
    private int mScrollState = SCROLL_STATE_IDLE;

    private int mPartialScreen = -1;

    public interface OnTrackPageListener {
        void onPageDrag();
        void onPageSettling();
    }

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                PlayerTrackView trackView;
                if (position == getCurrentItem() && positionOffset > 0 && mPartialScreen != position + 1) {
                    mPartialScreen = position + 1;
                    trackView = getAdapter().getPlayerTrackViewByPosition(mPartialScreen);
                    if (trackView != null) trackView.setOnScreen(true);
                } else if (position == getCurrentItem() - 1 && mPartialScreen != position) {
                    mPartialScreen = position;
                    trackView = getAdapter().getPlayerTrackViewByPosition(mPartialScreen);
                    if (trackView != null) trackView.setOnScreen(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mScrollState = state;

                if (mScrollState == SCROLL_STATE_DRAGGING && mTrackPagerScrollListener != null) {
                    mTrackPagerScrollListener.onPageDrag();
                } else if (mScrollState == SCROLL_STATE_SETTLING && mTrackPagerScrollListener != null) {
                    mTrackPagerScrollListener.onPageSettling();
                }
            }
        });
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (adapter instanceof PlayerTrackPagerAdapter){
            super.setAdapter(adapter);
        } else {
            throw new IllegalArgumentException("PlayerTrackPager can only be used with PlayerTrackPagerAdapter");
        }
    }

    @Override
    public PlayerTrackPagerAdapter getAdapter() {
        return (PlayerTrackPagerAdapter) super.getAdapter();
    }

    public void refreshAdapter() {
        // set to null first as it will force item reinstantiation of objects
        final PagerAdapter adapter = getAdapter();
        super.setAdapter(null);
        super.setAdapter(adapter);
    }

    public void setListener(OnTrackPageListener listener) {
        mTrackPagerScrollListener = listener;
    }

    public boolean prev() {
        final int currentItem = getCurrentItem();
        if (currentItem > 0) {
            setCurrentItem(currentItem - 1, true);
            return true;
        } else {
            return false;
        }
    }

    public boolean next() {
        final int currentItem = getCurrentItem();
        final PlayerTrackPagerAdapter adapter = getAdapter();
        if (adapter != null && currentItem < adapter.getCount() - 1) {
            setCurrentItem(currentItem + 1, true);
            return true;
        } else {
            return false;
        }
    }

    public boolean isScrolling() {
        return mScrollState != SCROLL_STATE_IDLE;
    }
   }