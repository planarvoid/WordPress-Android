package com.soundcloud.android.playback.views;

import com.soundcloud.android.playback.PlayerTrackPagerAdapter;
import com.soundcloud.android.view.SafeViewPager;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;

public class PlayerTrackPager extends SafeViewPager {

    private PageChangeListener mPageChangeListener;

    public interface OnTrackPageListener {
        void onPageDrag();
        void onPageChanged();
    }

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPageChangeListener = new PageChangeListener(this);
        setOnPageChangeListener(mPageChangeListener);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (adapter instanceof PlayerTrackPagerAdapter) {
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
        mPageChangeListener.setTrackPageListener(listener);
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
        return mPageChangeListener.mScrollState != SCROLL_STATE_IDLE;
    }

    static class PageChangeListener implements OnPageChangeListener {

        private PlayerTrackPager mPlayerTrackPager;
        private int mPartialScreen = -1;
        private OnTrackPageListener mTrackPageListener;
        private int mScrollState = SCROLL_STATE_IDLE;

        PageChangeListener(PlayerTrackPager playerTrackPager) {
            this.mPlayerTrackPager = playerTrackPager;
        }

        void setTrackPageListener(OnTrackPageListener trackPageListener) {
            mTrackPageListener = trackPageListener;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            PlayerTrackView trackView;
            if (position == mPlayerTrackPager.getCurrentItem() && positionOffset > 0 && mPartialScreen != position + 1) {
                mPartialScreen = position + 1;
                trackView = (PlayerTrackView) mPlayerTrackPager.getAdapter().getPlayerTrackViewByPosition(mPartialScreen);
                if (trackView != null) trackView.setOnScreen(true);
            } else if (position == mPlayerTrackPager.getCurrentItem() - 1 && mPartialScreen != position) {
                mPartialScreen = position;
                trackView = (PlayerTrackView) mPlayerTrackPager.getAdapter().getPlayerTrackViewByPosition(mPartialScreen);
                if (trackView != null) trackView.setOnScreen(true);
            }
        }

        @Override
        public void onPageSelected(int position) {}

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == SCROLL_STATE_DRAGGING && mTrackPageListener != null) {
                mTrackPageListener.onPageDrag();
            } else if ((state == SCROLL_STATE_SETTLING || (state == SCROLL_STATE_IDLE && mScrollState == SCROLL_STATE_DRAGGING))
                    && mTrackPageListener != null) {
                mTrackPageListener.onPageChanged();
            }
            mScrollState = state;
        }
    }
}