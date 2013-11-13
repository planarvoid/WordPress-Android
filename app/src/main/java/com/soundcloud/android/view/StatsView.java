package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class StatsView extends View {
    private final Paint textPaint;
    private final int mTextColor, mPressedColor;

    private final Drawable mPlaysIcon, mLikesIcon, mLikedIcon, mRepostsIcon, mRepostedIcon, mCommentsIcon, mSeparator;
    private final Drawable[] mIcons;
    private final int mItemPadding, mSeparatorWidth;
    private final int mFontOffset;
    private final Rect mBounds;
    private final int[] mOffsets;

    // track stats
    private int mPlays;
    private long mLikes;
    private long mReposts;
    private long mComments;
    private boolean mLiked;
    private boolean mReposted;

    @SuppressWarnings("UnusedDeclaration")
    public StatsView(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public StatsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public StatsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        Resources r = getResources();

        mBounds = new Rect();

        mPlaysIcon    = r.getDrawable(R.drawable.stats_plays);
        mLikesIcon    = r.getDrawable(R.drawable.stats_likes);
        mLikedIcon    = r.getDrawable(R.drawable.stats_liked);
        mRepostsIcon  = r.getDrawable(R.drawable.stats_stream_repost);
        mRepostedIcon = r.getDrawable(R.drawable.stats_stream_repost_done);
        mCommentsIcon = r.getDrawable(R.drawable.stats_comments);
        mIcons        = new Drawable[]{ mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon };

        mSeparator    = r.getDrawable(R.drawable.stat_divider);

        mItemPadding    = r.getDimensionPixelSize(R.dimen.stats_view_item_padding);
        mSeparatorWidth = r.getDimensionPixelSize(R.dimen.stats_view_separator_width);

        mFontOffset = r.getDimensionPixelSize(R.dimen.stats_view_font_offset);

        mTextColor    = r.getColor(R.color.statsColor);
        mPressedColor = r.getColor(R.color.statsColor);

        textPaint = new Paint();
        textPaint.setColor(mTextColor);
        textPaint.setAntiAlias(true );
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(r.getDimensionPixelSize(R.dimen.stats_view_item_text_size));

        mOffsets = new int[]{
                r.getDimensionPixelSize(R.dimen.stats_view_play_icon_offset),
                r.getDimensionPixelSize(R.dimen.stats_view_likes_icon_offset),
                r.getDimensionPixelSize(R.dimen.stats_view_reposts_icon_offset),
                r.getDimensionPixelSize(R.dimen.stats_view_comments_icon_offset)
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width  = 0;
        int height = 0;
        long[]      counts   = {mPlays, mLikes, mReposts, mComments};

        boolean    hasDrawn = false;

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        for (int i = 0; i < mIcons.length; i++) {
            if (counts[i] <= 0) continue;
            Drawable icon   = mIcons[i];
            String   string = Long.toString(counts[i]);
            if (hasDrawn) {
                width += mSeparatorWidth;
                width += mItemPadding;
            }

            textPaint.getTextBounds(string, 0, string.length(), mBounds);
            width += icon.getIntrinsicWidth();
            width += mItemPadding;
            width += mBounds.width();
            width += mItemPadding;

            height = Math.max(icon.getIntrinsicHeight() + mOffsets[i], Math.max(height, mBounds.height()));
            hasDrawn = true;
        }

        setMeasuredDimension(width + paddingLeft + paddingRight, height + paddingTop + paddingBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long[] counts   = {mPlays, mLikes, mReposts, mComments};
        Drawable[] icons = {
            mPlaysIcon,
            isLiked() ?    mLikedIcon    : mLikesIcon,
            isReposted() ? mRepostedIcon : mRepostsIcon,
            mCommentsIcon
        };

        int x = 0;
        boolean hasDrawn = false;
        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;

            Drawable icon   = icons[i];
            String   string = ScTextUtils.formatNumberWithCommas(counts[i]);

            if (hasDrawn) {
                mSeparator.setBounds(x, 0, x + mSeparatorWidth, getHeight());
                mSeparator.draw(canvas);

                x += mSeparatorWidth;
                x += mItemPadding;
            }

            final int iconHeight = icon.getIntrinsicHeight();
            final int iconWidth  = icon.getIntrinsicWidth();
            final int iconY      = (getHeight() - iconHeight) / 2 + mOffsets[i];

            icon.setBounds(x, iconY, x + iconWidth, iconY + iconHeight);
            icon.draw(canvas);

            x += iconWidth;
            x += mItemPadding;

            int textY = (int) (getHeight() - (getHeight() - textPaint.getTextSize()) / 2) + mFontOffset;

            canvas.drawText(string, x, textY, textPaint);
            textPaint.getTextBounds(string, 0, string.length(), mBounds);
            x += mBounds.width();
            x += mItemPadding;

            hasDrawn = true;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        boolean changed = false;
        for (Drawable drawable : new Drawable[]{mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon, mSeparator}) {
            if (drawable.isStateful()) {
                changed |= drawable.setState(getDrawableState());
            }
        }

        textPaint.setColor(isPressed() ? mPressedColor : mTextColor);

        if (changed) invalidate();
    }

    public void updateWithPlayable(Playable playable, boolean showFullStats) {

        if (showFullStats){
            mLikes    = playable.likes_count;
            mReposts  = playable.reposts_count;
            mReposted = playable.user_repost;
            mLiked    = playable.user_like;
        } else {
            mLikes = 0;
            mReposts = 0;
        }

        if (playable instanceof Track){
            final Track track = (Track) playable;
            mPlays = (int) track.playback_count;
            mComments = (showFullStats) ? track.comment_count : 0;
        } else {
            mPlays = 0;
            mComments = 0;
        }

        invalidate();
    }

    public void setPlays(int plays) {
        mPlays = plays;
        invalidate();
    }

    public void setLikes(int likes) {
        mLikes = likes;
        invalidate();
    }

    public void setReposts(int reposts) {
        mReposts = reposts;
        invalidate();
    }

    public void setComments(int comments) {
        mComments = comments;
        invalidate();
    }

    public boolean isLiked() {
        return mLiked;
    }

    public boolean isReposted() {
        return mReposted;
    }
}
