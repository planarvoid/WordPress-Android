package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.play.PlayerTrackView;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.LinkedList;

public class PlayerTrackPager extends ViewPager {
    private LinkedList<FrameLayout> mViews = new LinkedList<FrameLayout>();
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
                    trackView = getTrackViewAt(mPartialScreen);
                    if (trackView != null) trackView.setOnScreen(true);
                } else if (position == getCurrentItem() - 1 && mPartialScreen != position) {
                    mPartialScreen = position;
                    trackView = getTrackViewAt(mPartialScreen);
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



    public void setListener(OnTrackPageListener listener) {
        mTrackPagerScrollListener = listener;
    }

    @Deprecated
    public void configureFromService(ScPlayer player, @Nullable PlayQueueManager playQueueManager, int playPosition) {
        final long queueLength = playQueueManager == null ? 1 : playQueueManager.length();
        if (playPosition == -1) playPosition = playQueueManager == null ? 0 : playQueueManager.getPosition();

        final boolean onLastTrack = playPosition == queueLength - 1;

        setCurrentItem(playPosition == 0 ? 0 : // beginning
                onLastTrack ? mViews.size() - 1 : // end
                1, // middle
                false); // no animate
    }

    public void prev() {
        final int currentItem = getCurrentItem();
        if (currentItem > 0) {
            setCurrentItem(currentItem - 1, true);
        }
    }

    public void next() {
        final int currentItem = getCurrentItem();
        if (currentItem < mViews.size() - 1) {
            setCurrentItem(currentItem + 1, true);
        }
    }

    public boolean isScrolling() {
        return mScrollState != SCROLL_STATE_IDLE;
    }

    private FrameLayout wrapPlayerTrackView(PlayerTrackView ptv) {
        FrameLayout frameLayout = new FrameLayout(this.getContext());
        frameLayout.addView(ptv);
        return frameLayout;
    }

    private @Nullable PlayerTrackView getTrackViewAt(int i){
        return mViews.size() > i && i >= 0 ? (PlayerTrackView) mViews.get(i).getChildAt(0) : null;
    }

   }