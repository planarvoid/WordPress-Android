package com.soundcloud.android.view;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.FrameLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.play.PlayerTrackView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlayerTrackPager extends ViewPager {
    private LinkedList<FrameLayout> mViews = new LinkedList<FrameLayout>();
    private @Nullable OnTrackPageListener mTrackPagerScrollListener;
    private int mScrollState = SCROLL_STATE_IDLE;
    private List<PlayerTrackView> mPlayerTrackViews = new ArrayList<PlayerTrackView>();

    private int mPartialScreen = -1;

    public interface OnTrackPageListener {
        void onPageDrag();
        void onPageSettling();
    }

    public PlayerTrackPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setAdapter(mPageViewAdapter);
        setOnPageChangeListener(new OnPageChangeListener() {

            private int mDirection;

            private static final int LEFT  = 0;
            private static final int RIGHT = 2;

            @Override
            public void onPageSelected(int position) {
                mDirection = position;
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


                PlayQueueManager playQueueManager = CloudPlaybackService.getPlaylistManager();
                final long queueLength = playQueueManager == null ? 1 : playQueueManager.length();

                if (state == ViewPager.SCROLL_STATE_IDLE && !mViews.isEmpty()) {
                    switch (mDirection) {
                        case LEFT: {
                            PlayerTrackView currentView = (PlayerTrackView) mViews.getFirst().getChildAt(0);
                            if (currentView.getPlayPosition() > 0) {
                                // move the last trackview to the beginning
                                PlayerTrackView lastView = (PlayerTrackView) mViews.getLast().getChildAt(0);
                                mViews.getLast().removeAllViews();
                                for (int i = mViews.size() - 1; i > 0; i--) {
                                    View view = mViews.get(i - 1).getChildAt(0);
                                    mViews.get(i - 1).removeAllViews();
                                    mViews.get(i).addView(view);
                                }
                                mViews.getFirst().addView(lastView);

                                final int pos = currentView.getPlayPosition() - 1;
                                Track track = playQueueManager == null ? null : playQueueManager.getTrackAt(pos);
                                if (track != null) lastView.setTrack(track, pos, true, false);

                                lastView.setOnScreen(false);
                                mPlayerTrackViews.add(0, mPlayerTrackViews.remove(mPlayerTrackViews.size() - 1));
                                setCurrentItem(1, false);
                            }
                            break;
                        }
                        case RIGHT: {
                            PlayerTrackView currentView = (PlayerTrackView) mViews.getLast().getChildAt(0);
                            if (currentView.getPlayPosition() < queueLength - 1) {
                                // move the first trackview to the end
                                PlayerTrackView firstView = (PlayerTrackView) mViews.getFirst().getChildAt(0);
                                mViews.getFirst().removeAllViews();
                                for (int i = 0; i < mViews.size() - 1; i++) {
                                    View view = mViews.get(i + 1).getChildAt(0);
                                    mViews.get(i + 1).removeAllViews();
                                    mViews.get(i).addView(view);
                                }
                                mViews.getLast().addView(firstView);

                                final int pos = currentView.getPlayPosition() + 1;
                                Track track = playQueueManager == null ? null : playQueueManager.getTrackAt(pos);
                                if (track != null) firstView.setTrack(track, pos, true, false);

                                firstView.setOnScreen(false);
                                mPlayerTrackViews.add(mPlayerTrackViews.remove(0));
                                setCurrentItem(1, false);
                            }
                        }
                    }
                }
            }
        });
    }

    public void setListener(OnTrackPageListener listener) {
        mTrackPagerScrollListener = listener;
    }

    public void configureFromTrack(ScPlayer player, @NotNull Track track, int playPosition) {
        mPlayerTrackViews.clear();

        final PlayerTrackView ptv;
        if (mViews.size() > 0) {
            ptv = ((PlayerTrackView) mViews.get(0).getChildAt(0));
            for (int i = 0; i < mViews.size() - 1; i++) mViews.removeLast();
        } else {
            ptv = new PlayerTrackView(player);
            mViews.add(wrapPlayerTrackView(ptv));
        }
        mPlayerTrackViews.add(ptv);

        ptv.setOnScreen(true);
        ptv.setTrack(track, playPosition,true,true);
        mPageViewAdapter.notifyDataSetChanged();
        setCurrentItem(0);
    }

    public void configureFromService(ScPlayer player, @Nullable PlayQueueManager playQueueManager, int playPosition) {
        final long queueLength = playQueueManager == null ? 1 : playQueueManager.length();
        if (playPosition == -1) playPosition = playQueueManager == null ? 0 : playQueueManager.getPosition();

        mPlayerTrackViews.clear();

        // setup initial workspace, reusing them if possible
        int workspaceIndex = 0;

        final boolean onLastTrack = playPosition == queueLength - 1;
        final int start = onLastTrack ? Math.max(0, playPosition - 2) : Math.max(0, playPosition - 1);
        for (int pos = start; pos < Math.min(start + 3, queueLength); pos++) {
            final PlayerTrackView ptv;
            if (mViews.size() > workspaceIndex) {
                ptv = ((PlayerTrackView) mViews.get(workspaceIndex).getChildAt(0));
            } else {
                ptv = new PlayerTrackView(player);
                mViews.add(wrapPlayerTrackView(ptv));
            }

            mPlayerTrackViews.add(ptv);

            final boolean priority = pos == playPosition;
            ptv.setOnScreen(priority);

            final long trackId = playQueueManager == null ? CloudPlaybackService.getCurrentTrackId() : playQueueManager.getTrackIdAt(pos);
            final Track track = SoundCloudApplication.MODEL_MANAGER.getTrack(trackId);

            if (track != null) {
                ptv.setTrack(track, pos, false, priority);
                workspaceIndex++;
            }
        }

        if (queueLength < mViews.size()) {
            final long toRemove = mViews.size() - queueLength;
            for (int i = 0; i < toRemove; i++) mViews.removeLast();
        }
        mPageViewAdapter.notifyDataSetChanged();

        setCurrentItem(playPosition == 0 ? 0 : // beginning
                onLastTrack ? mViews.size() - 1 : // end
                1, // middle
                false); // no animate
    }

    public @Nullable PlayerTrackView getCurrentTrackView() {
        final int currentItem = getCurrentItem();
        if (currentItem < 0 || currentItem >= mPlayerTrackViews.size()) return null;
        return mPlayerTrackViews.get(currentItem);
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

    public List<PlayerTrackView> playerTrackViews() {
        return mPlayerTrackViews;
    }

    private FrameLayout wrapPlayerTrackView(PlayerTrackView ptv) {
        FrameLayout frameLayout = new FrameLayout(this.getContext());
        frameLayout.addView(ptv);
        return frameLayout;
    }

    private @Nullable PlayerTrackView getTrackViewAt(int i){
        return mViews.size() > i && i >= 0 ? (PlayerTrackView) mViews.get(i).getChildAt(0) : null;
    }

    private PagerAdapter mPageViewAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return mViews == null || mViews.isEmpty() ? 1  : mViews.size();
        }

        @Override @SuppressWarnings("deprecation")
        public Object instantiateItem(View collection, int position) {
            final View v = (mViews == null || position >= mViews.size()) ?
                    View.inflate(getContext(), R.layout.empty_player, null) : mViews.get(position);
            ((ViewGroup) collection).addView(v);
            return v;
        }

        @Override @SuppressWarnings("deprecation")
        public void destroyItem(View collection, int position, Object view) {
            ((ViewManager) collection).removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public int getItemPosition(Object object) {
            // this will force the empty view to get tossed out if we dont need it anymore
            return mViews == null || mViews.indexOf(object) == -1 ? POSITION_NONE : POSITION_UNCHANGED;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    };
   }