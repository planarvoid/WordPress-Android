package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayerTimeView extends RelativeLayout {
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private int mDuration;
    private int mMeasuredMaxWidth;

    private int mCommentingWidth;
    private int mDefaultHeight;
    private int mCommentingHeight;

    private Paint mBgPaint;
    private Paint mLinePaint;
    private int mArc;

    private int mPlayheadOffset;
    private int mPlayheadArrowWidth;
    private int mPlayheadArrowHeight;
    private boolean mCommenting;

    private TextView mCommentInstructions;
    private boolean mShowArrow;
    private boolean mRoundTop = true;
    private int mTargetOffsetX;
    private int mAdjustedHeight;


    public PlayerTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.player_time, this);

        mCurrentTime = (TextView) findViewById(R.id.txt_current);
        mTotalTime = (TextView) findViewById(R.id.txt_total);

        mCommentInstructions = (TextView) findViewById(R.id.txt_comment_instructions);
        mCommentingWidth = context.getResources().getDimensionPixelSize(R.dimen.player_time_comment_width);

        mDefaultHeight = context.getResources().getDimensionPixelSize(R.dimen.player_time_height);
        mCommentingHeight = context.getResources().getDimensionPixelSize(R.dimen.player_time_comment_height);

        mBgPaint = new Paint();
        mBgPaint.setColor(0xFFFFFFFF);
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setMaskFilter(new EmbossMaskFilter(new float[]{0, .5f, 1}, 0.85f, 10, 1f));

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.portraitPlayerCommentLine));
        mLinePaint.setStyle(Paint.Style.STROKE);

        mArc = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowWidth = getResources().getDimensionPixelSize(R.dimen.player_time_width);
        mPlayheadArrowHeight = -1 * getResources().getDimensionPixelSize(R.dimen.player_time_bottom_margin);

        setGravity(Gravity.CENTER_HORIZONTAL);
        setShowArrow(getContext().getResources().getBoolean(R.bool.player_time_show_arrow));
    }

    public void setCommenting(boolean commenting) {
        if (commenting && !mCommenting) {
            mCommenting = true;
            mCurrentTime.setTextColor(getResources().getColor(R.color.portraitPlayerCommentLine));
            mCommentInstructions.setVisibility(View.VISIBLE);
            getLayoutParams().width = mCommentingWidth;
            getLayoutParams().height = mCommentingHeight;

        } else if (!commenting && mCommenting) {
            mCommenting = false;
            mCurrentTime.setTextColor(Color.BLACK);
            mCommentInstructions.setVisibility(View.GONE);
            getLayoutParams().width = mMeasuredMaxWidth;
            getLayoutParams().height = mDefaultHeight;
        }
        requestLayout();
        invalidate();
    }

    public void setRoundTop(boolean roundTop) {
        mRoundTop = roundTop;
        invalidate();
    }

    public void setCommentingHeight(int commentingHeight){
        mCommentingHeight = commentingHeight;
        setAdjustedHeight();
        invalidate();
    }

    private void setAdjustedHeight() {
        int arrowOffset = mShowArrow ? mPlayheadArrowHeight : 0;
        mAdjustedHeight = (mCommenting ? mCommentingHeight : mDefaultHeight) + arrowOffset ;
        getLayoutParams().height = mAdjustedHeight;
    }

    public void setCurrentTime(long time) {
        mCurrentTime.setText(ScTextUtils.formatTimestamp(time));

        if (getParent() == null) return;

        final int parentWidth = ((RelativeLayout) this.getParent()).getWidth();
        final int width = getLayoutParams().width;
        final int playheadX = Math.round(parentWidth * (time / ((float) mDuration)));

        if (playheadX < width / 2) {
            mPlayheadOffset = playheadX;
        } else if (playheadX > parentWidth - width / 2) {
            mPlayheadOffset = playheadX - (parentWidth - width);
        } else {
            mPlayheadOffset = (int) (.5 * width);
        }

        mTargetOffsetX = mDuration == 0 ? 0 : Math.round(parentWidth * (time / ((float) mDuration))) - mPlayheadOffset;
        offsetLeftAndRight(mTargetOffsetX - getLeft());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed){
            offsetLeftAndRight(mTargetOffsetX);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int commentHeight = mCommentInstructions.getMeasuredHeight();
        if (commentHeight > 0) {
            final int lineCount = commentHeight > 3 * mCommentInstructions.getLineHeight() ? 3 : commentHeight / mCommentInstructions.getLineHeight();
            if (lineCount < mCommentInstructions.getLineCount()) {
                mCommentInstructions.setMaxLines(lineCount);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int measuredHeight = getMeasuredHeight();
        ImageUtils.drawBubbleOnCanvas(canvas, mBgPaint, mCommenting ? mLinePaint : null, getMeasuredWidth(),
                mShowArrow ? measuredHeight - mPlayheadArrowHeight : measuredHeight,
                mRoundTop ? mArc : 0, mShowArrow ? mPlayheadArrowWidth : 0, mPlayheadArrowHeight, mPlayheadOffset);

        super.dispatchDraw(canvas);
    }

    public void setByPercent(float seekPercent) {
        setCurrentTime((long) (mDuration * seekPercent));
    }

    public void setDuration(int time) {
        final CharSequence placeholder = formatTime(time);
        mCurrentTime.setText(placeholder);
        mTotalTime.setText(placeholder);

        getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mMeasuredMaxWidth = getMeasuredWidth();
        getLayoutParams().width = mMeasuredMaxWidth;
        requestLayout();


        setCurrentTime(0l);
        mDuration = time;
        mTotalTime.setText(ScTextUtils.formatTimestamp(time));
        invalidate();
    }

    private void setShowArrow(boolean showArrow){
        mShowArrow = showArrow;

        final int paddingBottomExtra = mShowArrow ? mPlayheadArrowHeight : 0;
        setPadding(
                getResources().getDimensionPixelOffset(R.dimen.player_time_padding_left_right), 0,
                getResources().getDimensionPixelOffset(R.dimen.player_time_padding_left_right),
                getResources().getDimensionPixelOffset(R.dimen.player_time_padding_top_bottom) + paddingBottomExtra
        );
    }

    private CharSequence formatTime(int time) {
        int result;
        if (time / 1000 < 600) result = 3;
        else if (time / 1000 < 3600) result = 4;
        else if (time / 1000 < 36000) result = 5;
        else result = 6;

        final int digits = result;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            sb.append("8");
            if (i % 2 == 1 && i < 5) sb.append(".");
        }
        return sb;
    }
}