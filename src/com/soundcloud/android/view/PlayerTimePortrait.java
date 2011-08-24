package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.RelativeLayout;
import com.soundcloud.android.R;

public class PlayerTimePortrait extends PlayerTime {

    private int mDefaultWidth;
    private int mDragWidth;

    private Paint mBgPaint;
    private Paint mLinePaint;
    private int mArc;

    private int mPlayheadOffset;
    private boolean mPlayheadLeft;
    private int mPlayheadArrowWidth;
    private int mPlayheadArrowHeight;
    private boolean mSeeking;




    public PlayerTimePortrait(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDefaultWidth = (int) context.getResources().getDimension(R.dimen.player_time_width);
        mDragWidth = (int) context.getResources().getDimension(R.dimen.player_time_drag_width);

        mBgPaint = new Paint();
        mBgPaint.setColor(0xFFFFFFFF);
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint();
        mLinePaint.setColor(0xFF000000);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);

        mArc = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowWidth = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowHeight = (int) (getResources().getDisplayMetrics().density * 10);

        final int pad = (int) (5 * getResources().getDisplayMetrics().density);
        setPadding(pad, 0, pad, mPlayheadArrowHeight);
        setGravity(Gravity.CENTER_HORIZONTAL);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.width = mDefaultWidth;
        lp.height = mDefaultWidth;
        setLayoutParams(lp);
    }

    @Override
    public void setWaveHeight(int height) {
        if (getPaddingBottom() != height){
            final int pad = (int) (5 * getResources().getDisplayMetrics().density);
            setPadding(pad,0,pad,height);
            requestLayout();
        }

    }


    @Override
    public void setCurrentTime(long time, boolean seeking) {
        super.setCurrentTime(time, seeking);

        final int width = seeking ? mDragWidth : mDefaultWidth;
        if (seeking && !mSeeking){
            mSeeking = true;
            ((RelativeLayout.LayoutParams) getLayoutParams()).width = width;
        } else if (!seeking && mSeeking){
            mSeeking = false;
            ((RelativeLayout.LayoutParams) getLayoutParams()).width = width;
        }

        final int parentWidth = ((RelativeLayout) this.getParent()).getWidth();
        final int playheadX = Math.round (parentWidth * (time / ((float) mDuration)));

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
            ((RelativeLayout.LayoutParams) getLayoutParams()).leftMargin = Math.round (parentWidth * (time / ((float) mDuration))) - mPlayheadOffset;
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
        final int Fy = h + (mSeeking ? mPlayheadArrowHeight : 150);
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
        super.dispatchDraw(canvas);
    }


}