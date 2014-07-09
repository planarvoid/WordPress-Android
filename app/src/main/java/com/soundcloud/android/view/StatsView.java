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
    private final int textColor, pressedColor;

    private final Drawable playsIcon, likesIcon, likedIcon, repostsIcon, repostedIcon, commentsIcon, separator;
    private final Drawable[] icons;
    private final int itemPadding, separatorWidth;
    private final int fontOffset;
    private final Rect bounds;
    private final int[] offsets;

    // track stats
    private int plays;
    private long likes;
    private long reposts;
    private long comments;
    private boolean liked;
    private boolean reposted;

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

        bounds = new Rect();

        playsIcon = r.getDrawable(R.drawable.stats_plays);
        likesIcon = r.getDrawable(R.drawable.stats_likes);
        likedIcon = r.getDrawable(R.drawable.stats_liked);
        repostsIcon = r.getDrawable(R.drawable.stats_stream_repost);
        repostedIcon = r.getDrawable(R.drawable.stats_stream_repost_done);
        commentsIcon = r.getDrawable(R.drawable.stats_comments_legacy);
        icons = new Drawable[]{playsIcon, likesIcon, repostsIcon, commentsIcon};

        separator = r.getDrawable(R.drawable.stat_divider);

        itemPadding = r.getDimensionPixelSize(R.dimen.stats_view_item_padding);
        separatorWidth = r.getDimensionPixelSize(R.dimen.stats_view_separator_width);

        fontOffset = r.getDimensionPixelSize(R.dimen.stats_view_font_offset);

        textColor = r.getColor(R.color.stats_color);
        pressedColor = r.getColor(R.color.stats_color);

        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true );
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(r.getDimensionPixelSize(R.dimen.stats_view_item_text_size));

        offsets = new int[]{
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
        long[]      counts   = {plays, likes, reposts, comments};

        boolean    hasDrawn = false;

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();
        int textHeight = fm.descent - fm.ascent + fm.leading;

        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;
            Drawable icon   = icons[i];
            String   string = Long.toString(counts[i]);
            if (hasDrawn) {
                width += separatorWidth;
                width += itemPadding;
            }

            textPaint.getTextBounds(string, 0, string.length(), bounds);
            width += icon.getIntrinsicWidth();
            width += itemPadding;
            width += bounds.width();
            width += itemPadding;

            height = Math.max(height, Math.max(icon.getIntrinsicHeight() + offsets[i], textHeight));
            hasDrawn = true;
        }

        setMeasuredDimension(width + paddingLeft + paddingRight, height + paddingTop + paddingBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long[] counts   = {plays, likes, reposts, comments};
        Drawable[] icons = {
                playsIcon,
            isLiked() ? likedIcon : likesIcon,
            isReposted() ? repostedIcon : repostsIcon,
                commentsIcon
        };

        int x = 0;
        boolean hasDrawn = false;
        for (int i = 0; i < icons.length; i++) {
            if (counts[i] <= 0) continue;

            Drawable icon   = icons[i];
            String   string = ScTextUtils.formatNumberWithCommas(counts[i]);

            if (hasDrawn) {
                separator.setBounds(x, 0, x + separatorWidth, getHeight());
                separator.draw(canvas);

                x += separatorWidth;
                x += itemPadding;
            }

            final int iconHeight = icon.getIntrinsicHeight();
            final int iconWidth  = icon.getIntrinsicWidth();
            final int iconY      = (getHeight() - iconHeight) / 2 + offsets[i];

            icon.setBounds(x, iconY, x + iconWidth, iconY + iconHeight);
            icon.draw(canvas);

            x += iconWidth;
            x += itemPadding;

            int textY = (int) (getHeight() - (getHeight() - textPaint.getTextSize()) / 2) + fontOffset;

            canvas.drawText(string, x, textY, textPaint);
            textPaint.getTextBounds(string, 0, string.length(), bounds);
            x += bounds.width();
            x += itemPadding;

            hasDrawn = true;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        boolean changed = false;
        for (Drawable drawable : new Drawable[]{playsIcon, likesIcon, repostsIcon, commentsIcon, separator}) {
            if (drawable.isStateful()) {
                changed |= drawable.setState(getDrawableState());
            }
        }

        textPaint.setColor(isPressed() ? pressedColor : textColor);

        if (changed) invalidate();
    }

    public void updateWithPlayable(Playable playable, boolean showFullStats) {

        if (showFullStats){
            likes = playable.likes_count;
            reposts = playable.reposts_count;
            reposted = playable.user_repost;
            liked = playable.user_like;
        } else {
            likes = 0;
            reposts = 0;
        }

        if (playable instanceof Track){
            final Track track = (Track) playable;
            plays = (int) track.playback_count;
            comments = (showFullStats) ? track.comment_count : 0;
        } else {
            plays = 0;
            comments = 0;
        }

        invalidate();
    }

    public void setPlays(int plays) {
        this.plays = plays;
        invalidate();
    }

    public void setLikes(int likes) {
        this.likes = likes;
        invalidate();
    }

    public void setReposts(int reposts) {
        this.reposts = reposts;
        invalidate();
    }

    public void setComments(int comments) {
        this.comments = comments;
        invalidate();
    }

    public boolean isLiked() {
        return liked;
    }

    public boolean isReposted() {
        return reposted;
    }
}
