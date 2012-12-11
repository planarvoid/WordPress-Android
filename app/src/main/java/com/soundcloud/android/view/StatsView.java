package com.soundcloud.android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Launch;

import java.util.ArrayList;

import static java.lang.StrictMath.max;

public class StatsView extends View {
    private final Paint textPaint;
    private final Drawable mPlaysIcon, mLikesIcon, mLikedIcon, mRepostsIcon, mRepostedIcon, mCommentsIcon, mSeparator;
    private final int mPlayIconOffset, mLikesIconOffset, mRepostsIconOffset, mCommentsIconOffset;
    private final int mItemPadding, mSeparatorWidth;
    private final int mFontOffset;

    private int mPlays, mLikes, mResposts, mComments;

    private boolean mLiked;
    private boolean mReposted;

    public StatsView(Context context) {
        super(context);
    }

    public StatsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(23);

        Resources r = getResources();

        mPlaysIcon    = r.getDrawable(R.drawable.ic_stats_plays_states);
        mLikesIcon    = r.getDrawable(R.drawable.ic_stats_likes_states);
        mLikedIcon    = r.getDrawable(R.drawable.ic_stats_liked_states);
        mRepostsIcon  = r.getDrawable(R.drawable.ic_stats_reposts_states);
        mRepostedIcon = r.getDrawable(R.drawable.ic_stats_reposted_states);
        mCommentsIcon = r.getDrawable(R.drawable.ic_stats_comments_states);
        mSeparator    = r.getDrawable(R.drawable.stat_divider);

        mPlayIconOffset     = (int) r.getDimension(R.dimen.stats_view_play_icon_offset);
        mLikesIconOffset    = (int) r.getDimension(R.dimen.stats_view_likes_icon_offset);
        mRepostsIconOffset  = (int) r.getDimension(R.dimen.stats_view_reposts_icon_offset);
        mCommentsIconOffset = (int) r.getDimension(R.dimen.stats_view_comments_icon_offset);

        mItemPadding    = (int) r.getDimension(R.dimen.stats_view_item_padding);
        mSeparatorWidth = (int) r.getDimension(R.dimen.stats_view_separator_width);

        mFontOffset = (int) r.getDimension(R.dimen.stats_view_font_offset);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;

        int[]      counts   = {mPlays, mLikes, mResposts, mComments};
        Drawable[] icons    = {mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon};
        boolean    hasDrawn = false;

        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;

            Drawable icon   = icons[i];
            String   string = Integer.toString(counts[i]);

            if (hasDrawn) {
                width += mSeparatorWidth;
                width += mItemPadding;
            }

            width += icon.getIntrinsicWidth();
            width += mItemPadding;
            width += textPaint.measureText(string);
            width += mItemPadding;

            hasDrawn = true;
        }

        setMeasuredDimension(width, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int[] counts  = {mPlays,          mLikes,           mResposts,          mComments};
        int[] offsets = {mPlayIconOffset, mLikesIconOffset, mRepostsIconOffset, mCommentsIconOffset};

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
            int iconY      = (getHeight() - iconHeight) / 2 + offsets[i];

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

        textPaint.setColor(isPressed() ? 0x66FFFFFF : 0xFF828282);

        if (changed) invalidate();
    }

    public Typeface getTypeface() {
        return textPaint.getTypeface();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
    }

    public int getPlays() {
        return mPlays;
    }

    public void setPlays(int plays) {
        mPlays = plays;
    }

    public int getLikes() {
        return mLikes;
    }

    public void setLikes(int likes) {
        mLikes = likes;
    }

    public int getResposts() {
        return mResposts;
    }

    public void setResposts(int resposts) {
        mResposts = resposts;
    }

    public int getComments() {
        return mComments;
    }

    public void setComments(int comments) {
        mComments = comments;
    }

    public boolean isLiked() {
        return mLiked;
    }

    public void setLiked(boolean liked) {
        mLiked = liked;
    }

    public boolean isReposted() {
        return mReposted;
    }

    public void setReposted(boolean reposted) {
        mReposted = reposted;
    }
}
