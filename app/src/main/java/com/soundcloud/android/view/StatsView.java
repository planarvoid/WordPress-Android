package com.soundcloud.android.view;

import android.content.Context;
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
    private final Drawable mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon, mSeparator;

    private final int mItemPadding;

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

        mPlaysIcon    = getResources().getDrawable(R.drawable.ic_stats_plays_states);
        mLikesIcon    = getResources().getDrawable(R.drawable.ic_stats_likes_states);
        mRepostsIcon  = getResources().getDrawable(R.drawable.ic_stats_reposts_states);
        mCommentsIcon = getResources().getDrawable(R.drawable.ic_stats_comments_states);
        mSeparator    = getResources().getDrawable(R.drawable.stat_divider);

        mItemPadding = (int) (6 * getContext().getResources().getDisplayMetrics().density);
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
                width += 2;
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
        Log.d("robb", "drawing");

        int x = 0;

        int[]      counts   = {mPlays, mLikes, mResposts, mComments};
        Drawable[] icons    = {mPlaysIcon, mLikesIcon, mRepostsIcon, mCommentsIcon};
        boolean    hasDrawn = false;

        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;

            Drawable icon   = icons[i];
            String   string = Integer.toString(counts[i]);

            if (hasDrawn) {
                mSeparator.setBounds(x, 0, x + 2, getHeight());
                mSeparator.draw(canvas);

                x += 2;
                x += mItemPadding;
            }

            int iconHeight = icon.getIntrinsicHeight();
            int iconY      = (getHeight() - iconHeight) / 2;

            icon.setBounds(x, iconY, x + icon.getIntrinsicWidth(), iconY + icon.getIntrinsicHeight());
            icon.draw(canvas);

            x += icon.getIntrinsicWidth();
            x += mItemPadding;

            int textY = (int) (getHeight() - (getHeight() - textPaint.getTextSize()) / 2);

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
