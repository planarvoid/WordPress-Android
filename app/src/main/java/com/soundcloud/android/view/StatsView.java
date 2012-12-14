package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;

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
    private final int mItemPadding, mSeparatorWidth;
    private final int mFontOffset;

    private int mPlays, mLikes, mResposts, mComments;
    private Rect mBounds;

    private boolean mLiked;
    private boolean mReposted;

    int[] mOffsets;

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

        mPlaysIcon    = r.getDrawable(R.drawable.ic_stats_plays_states);
        mLikesIcon    = r.getDrawable(R.drawable.ic_stats_likes_states);
        mLikedIcon    = r.getDrawable(R.drawable.ic_stats_liked_states);
        mRepostsIcon  = r.getDrawable(R.drawable.ic_stats_reposts_states);
        mRepostedIcon = r.getDrawable(R.drawable.ic_stats_reposted_states);
        mCommentsIcon = r.getDrawable(R.drawable.ic_stats_comments_states);
        mSeparator    = r.getDrawable(R.drawable.stat_divider);

        mItemPadding    = (int) r.getDimension(R.dimen.stats_view_item_padding);
        mSeparatorWidth = (int) r.getDimension(R.dimen.stats_view_separator_width);

        mFontOffset = (int) r.getDimension(R.dimen.stats_view_font_offset);

        mTextColor    = r.getColor(R.color.listTxtValue);
        mPressedColor = r.getColor(R.color.listTxtSecondary);

        textPaint = new Paint();
        textPaint.setColor(mTextColor);
        textPaint.setAntiAlias(true );
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(r.getDimension(R.dimen.stats_view_item_text_size));

        mOffsets    =   new int[]{
                (int) r.getDimension(R.dimen.stats_view_play_icon_offset),
                (int) r.getDimension(R.dimen.stats_view_likes_icon_offset),
                (int) r.getDimension(R.dimen.stats_view_reposts_icon_offset),
                (int) r.getDimension(R.dimen.stats_view_comments_icon_offset)
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;

        int[]      counts   = {mPlays, mLikes, mResposts, mComments};
        Drawable[] icons    = {mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon};
        boolean    hasDrawn = false;

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;

            Drawable icon   = icons[i];
            String   string = Integer.toString(counts[i]);

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
        int[] counts   = {mPlays, mLikes, mResposts, mComments};
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
            String   string = Integer.toString(counts[i]);

            if (hasDrawn) {
                mSeparator.setBounds(x, 0, x + mSeparatorWidth, getHeight());
                mSeparator.draw(canvas);

                x += mSeparatorWidth;
                x += mItemPadding;
            }

            int iconHeight = icon.getIntrinsicHeight();
            int iconY      = (getHeight() - iconHeight) / 2 + mOffsets[i];

            icon.setBounds(x, iconY, x + icon.getIntrinsicWidth(), iconY + icon.getIntrinsicHeight());
            icon.draw(canvas);

            x += icon.getIntrinsicWidth();
            x += mItemPadding;

            int textY = (int) (getHeight() - (getHeight() - textPaint.getTextSize()) / 2) + mFontOffset;

            canvas.drawText(string, x, textY, textPaint);
            x += textPaint.measureText(string);
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

    public void updateWithTrack(Track track) {
        mPlays    = track.playback_count;
        mLikes    = track.likes_count;
        mResposts = track.reposts_count;
        mComments = track.comment_count;

        mReposted = track.user_repost;
        mLiked    = track.user_like;
    }

    public void setPlays(int plays) {
        mPlays = plays;
    }

    public void setLikes(int likes) {
        mLikes = likes;
    }

    public void setResposts(int resposts) {
        mResposts = resposts;
    }

    public void setComments(int comments) {
        mComments = comments;
    }

    public boolean isLiked() {
        return mLiked;
    }

    public boolean isReposted() {
        return mReposted;
    }
}
