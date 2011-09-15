package com.soundcloud.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class PlayerTime extends RelativeLayout {
    protected TextView mCurrentTime;
    protected TextView mTotalTime;
    protected int mDuration;
    protected int mMaxTextWidth;
    protected int mMeasuredMaxWidth;

    private int mCommentingWidth;
    private int mDefaultHeight;
    private int mCommentingHeight;
    private int mBottomMargin;

    private Paint mBgPaint;
    private Paint mLinePaint;
    private int mArc;

    private int mPlayheadOffset;
    private boolean mPlayheadLeft;
    private int mPlayheadArrowWidth;
    private int mPlayheadArrowHeight;
    private boolean mCommenting;

    private TextView mCommentInstructions;
    private boolean mShowArrow;


    public PlayerTime(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_time, this);

        mCurrentTime = (TextView) findViewById(R.id.txt_current);
        mTotalTime = (TextView) findViewById(R.id.txt_total);

        mCommentInstructions = (TextView) findViewById(R.id.txt_comment_instructions);
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

        setShowArrow(false);
        setGravity(Gravity.CENTER_HORIZONTAL);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        final int pad = (int) (5 * getResources().getDisplayMetrics().density);

        if (mShowArrow) {
            setPadding(pad, 0, pad, mPlayheadArrowHeight + (int) (3 * getResources().getDisplayMetrics().density));
            lp.height = mDefaultHeight + mPlayheadArrowHeight;
            mBottomMargin = -mPlayheadArrowHeight;
        } else {
            setPadding(pad, 0, pad, (int) (3 * getResources().getDisplayMetrics().density));
            lp.height = mDefaultHeight;
        }

        setLayoutParams(lp);

    }

    private void init(AttributeSet attrs){
        TypedArray a=getContext().obtainStyledAttributes(attrs,R.styleable.PlayerTime);
        mShowArrow = a.getBoolean(R.styleable.PlayerTime_show_arrow, false);
        a.recycle();
    }

    public void setShowArrow(boolean showArrow){


    }

    public void setCurrentTime(long time, boolean commenting) {
        mCurrentTime.setText(CloudUtils.formatTimestamp(time));
        final RelativeLayout.LayoutParams lp = (LayoutParams) getLayoutParams();

        final int width = commenting ? mCommentingWidth : mMeasuredMaxWidth;
        final int height = commenting ? mCommentingHeight : mDefaultHeight;
        if (commenting && !mCommenting) {
            mCommenting = true;
            mCurrentTime.setTextColor(getResources().getColor(R.color.portraitPlayerCommentLine));
            mCommentInstructions.setVisibility(View.VISIBLE);
        } else if (!commenting && mCommenting) {
            mCommenting = false;
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
            lp.leftMargin = Math.round(parentWidth * (time / ((float) mDuration))) - mPlayheadOffset;
        } else {
            lp.leftMargin = 0;
        }


        lp.width = width;
        lp.height = mShowArrow ? height + mPlayheadArrowHeight : height;
        lp.bottomMargin = mBottomMargin;

        requestLayout();
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        drawBubbleOnCanvas(canvas, mBgPaint, mCommenting ? mLinePaint : null, getMeasuredWidth(),
                mShowArrow ? getMeasuredHeight() - mPlayheadArrowHeight : getMeasuredHeight(),
                mArc, mShowArrow ? mPlayheadArrowWidth : 0, mPlayheadArrowHeight, mPlayheadOffset);
        super.dispatchDraw(canvas);
    }

    private static void drawBubbleOnCanvas(Canvas c, Paint bgPaint, Paint linePaint, int width, int height, int arc, int arrowWidth, int arrowHeight, int arrowOffset){

        /*
             A ---- B
           I          C
           H ----G-E- D
                 F

         */

        final boolean arrowLeft = arrowOffset <= width/2;

        final int Ax = arc;
        final int Ay = 0;
        final int Bx = width - arc;
        final int By = 0;
        final int Cx = width;
        final int Cy = arc;
        final int Dx = width;
        final int Dy = height;
        final int Ex = arrowLeft ? arrowWidth + arrowOffset : arrowOffset;
        final int Ey = height;
        final int Fx = arrowOffset;
        final int Fy = height + arrowHeight;
        final int Gx = arrowLeft ? arrowOffset : arrowOffset - arrowWidth;
        final int Gy = height;
        final int Hx = 0;
        final int Hy = height;
        final int Ix = 0;
        final int Iy = arc;

        Path ctx = new Path();
        ctx.moveTo(Ax, Ay);
        ctx.lineTo(Bx, By);
        ctx.arcTo(new RectF(Bx, By, Cx, Cy), 270, 90); //B-C arc

        ctx.lineTo(Dx, Dy);

        if (arrowWidth > 0){
            ctx.lineTo(Ex, Ey);
            ctx.lineTo(Fx, Fy);
            ctx.lineTo(Gx, Gy);
        }


        ctx.lineTo(Hx, Hy);
        ctx.lineTo(Ix, Iy);
        ctx.arcTo(new RectF(Ax - arc, Ay, Ix + arc, Iy), 180, 90); //F-A arc
        c.drawPath(ctx, bgPaint);

        if (linePaint != null){
            c.drawLine(arrowOffset,height,arrowOffset,height+arrowOffset,linePaint);
        }
    }


    public void setByPercent(float seekPercent, boolean commenting) {
        setCurrentTime((long) (mDuration * seekPercent), commenting);
    }

    public void setDuration(int time) {
        final int digits = CloudUtils.getDigitsFromSeconds(time / 1000);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            sb.append("8");
            if (i % 2 == 1 && i < 5) sb.append(".");
        }
        mCurrentTime.setText(sb);
        mTotalTime.setText(sb);

        getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mMeasuredMaxWidth = getMeasuredWidth();
        getLayoutParams().width = mMeasuredMaxWidth;
        requestLayout();

        mDuration = time;
        mTotalTime.setText(CloudUtils.formatTimestamp(time));
        invalidate();
    }
}