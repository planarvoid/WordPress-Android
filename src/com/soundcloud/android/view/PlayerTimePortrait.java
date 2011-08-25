package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class PlayerTimePortrait extends PlayerTime {

    private int mDefaultWidth;
    private int mCommentingWidth;

    private int mDefaultHeight;
    private int mCommentingHeight;

    private Paint mBgPaint;
    private Paint mLinePaint;
    private int mArc;

    private int mPlayheadOffset;
    private boolean mPlayheadLeft;
    private int mPlayheadArrowWidth;
    private int mPlayheadArrowHeight;
    private boolean mCommenting;

    private TextView mCommentInstructions;


    public PlayerTimePortrait(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCommentInstructions = (TextView) findViewById(R.id.txt_comment_instructions);

        mDefaultWidth = (int) context.getResources().getDimension(R.dimen.player_time_width);
        mCommentingWidth = (int) context.getResources().getDimension(R.dimen.player_time_comment_width);

        mDefaultHeight = (int) context.getResources().getDimension(R.dimen.player_time_height);
        mCommentingHeight = (int) context.getResources().getDimension(R.dimen.player_time_comment_height);

        mBgPaint = new Paint();
        mBgPaint.setColor(0xFFFFFFFF);
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);
        //mBgPaint.setMaskFilter(new EmbossMaskFilter(new float[] { 0, 1, 1 },0.9f, 10, 1f));
        mBgPaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.INNER));
        //mBgPaint.setShadowLayer(-2, -2, 2, Color.BLACK);

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.portraitPlayerCommentLine));
        mLinePaint.setStyle(Paint.Style.STROKE);

        mArc = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowWidth = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowHeight = (int) (getResources().getDisplayMetrics().density * 10);

        final int pad = (int) (5 * getResources().getDisplayMetrics().density);
        setPadding((int) (5 * getResources().getDisplayMetrics().density),
                0,
                (int) (5 * getResources().getDisplayMetrics().density),
                mPlayheadArrowHeight + (int) (3 * getResources().getDisplayMetrics().density));

        setGravity(Gravity.CENTER_HORIZONTAL);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = -mPlayheadArrowHeight;
        lp.width = mDefaultWidth;
        lp.height = mDefaultHeight;
        setLayoutParams(lp);
    }

    @Override
    public void setWaveHeight(int height) {
    }


    @Override
    public void setCurrentTime(long time, boolean commenting) {
        super.setCurrentTime(time, commenting);

        final int width = commenting ? mCommentingWidth : mDefaultWidth;
        if (commenting && !mCommenting) {
            mCommenting = true;
            getLayoutParams().width = width;
            getLayoutParams().height = mCommentingHeight;
            mCurrentTime.setTextColor(getResources().getColor(R.color.portraitPlayerCommentLine));
            mCommentInstructions.setVisibility(View.VISIBLE);
        } else if (!commenting && mCommenting) {
            mCommenting = false;
            getLayoutParams().width = width;
            getLayoutParams().height = mDefaultHeight;
            mCurrentTime.setTextColor(getResources().getColor(R.color.black));
            mCommentInstructions.setVisibility(View.GONE);
        }

        final int parentWidth = ((RelativeLayout) this.getParent()).getWidth();
        final int playheadX = Math.round(parentWidth * (time / ((float) mDuration)));

        if (playheadX < width / 2) {
            mPlayheadOffset = playheadX;
            mPlayheadLeft = true;
        } else if (playheadX > parentWidth - width / 2) {
            mPlayheadOffset = playheadX - (parentWidth - width);
            mPlayheadLeft = false;
        } else {
            mPlayheadOffset = (int) (.5 * width);
            mPlayheadLeft = true;
        }

        if (mDuration > 0) {
            ((RelativeLayout.LayoutParams) getLayoutParams()).leftMargin = Math.round(parentWidth * (time / ((float) mDuration))) - mPlayheadOffset;
        } else {
            ((RelativeLayout.LayoutParams) getLayoutParams()).leftMargin = 0;
        }
        ((RelativeLayout.LayoutParams) getLayoutParams()).bottomMargin = -mPlayheadArrowHeight;
        requestLayout();
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        /*
             A ---- B
           I          C
           H ----G-E- D
                 F

         */

        final int w = getMeasuredWidth();
        final int h = getMeasuredHeight() - mPlayheadArrowHeight;

        final int Ax = mArc;
        final int Ay = 0;
        final int Bx = w - mArc;
        final int By = 0;
        final int Cx = w;
        final int Cy = mArc;
        final int Dx = w;
        final int Dy = h;
        final int Ex = mPlayheadLeft ? mPlayheadArrowWidth + mPlayheadOffset : mPlayheadOffset;
        final int Ey = h;
        final int Fx = mPlayheadOffset;
        final int Fy = h + mPlayheadArrowHeight;
        final int Gx = mPlayheadLeft ? mPlayheadOffset : mPlayheadOffset - mPlayheadArrowWidth;
        final int Gy = h;
        final int Hx = 0;
        final int Hy = h;
        final int Ix = 0;
        final int Iy = mArc;

        Path ctx = new Path();
        ctx.moveTo(Ax, Ay);
        ctx.lineTo(Bx, By);
        ctx.arcTo(new RectF(Bx, By, Cx, Cy), 270, 90); //B-C arc

        ctx.lineTo(Dx, Dy);
        ctx.lineTo(Ex, Ey);
        ctx.lineTo(Fx, Fy);
        ctx.lineTo(Gx, Gy);

        ctx.lineTo(Hx, Hy);
        ctx.lineTo(Ix, Iy);
        ctx.arcTo(new RectF(Ax - mArc, Ay, Ix + mArc, Iy), 180, 90); //F-A arc
        canvas.drawPath(ctx, mBgPaint);

        if (mCommenting){
            canvas.drawLine(mPlayheadOffset,h,mPlayheadOffset,h+mPlayheadOffset,mLinePaint);
        }

        super.dispatchDraw(canvas);
    }


}