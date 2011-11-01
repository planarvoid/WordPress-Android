package com.soundcloud.android.view.quickaction;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.soundcloud.android.utils.CloudUtils;

public class BubbleDrawable extends Drawable {

    private static final String TAG = BubbleDrawable.class.getSimpleName();
    private ColorFilter cf;

    private Paint mBgPaint;
    private Paint mLinePaint;

    private int mArrowOffsetX;
    private boolean mArrowTop;

    private int mArc;
    private int mArrowWidth;
    private int mArrowHeight;

    private int[] mBgGradientColors;
    private float[] mBgGradientPositions;

    @Override
    public void draw(Canvas canvas) {

        /*      C
            A --B-D-- E
          N            F
          M            G
            L -K-I--- H
                 J
         */

        Rect b = getBounds();

        final int width = b.right - b.left;
        final int height = b.bottom - b.top;
        final boolean arrowLeft = mArrowOffsetX <= width / 2;

        final int Ax = mArc;
        final int Ay = mArrowHeight;
        final int Bx = arrowLeft ? mArrowOffsetX : mArrowOffsetX - mArrowWidth;
        final int By = mArrowHeight;
        final int Cx = mArrowOffsetX;
        final int Cy = 0;
        final int Dx = arrowLeft ? mArrowOffsetX + mArrowWidth : mArrowOffsetX;
        final int Dy = mArrowHeight;
        final int Ex = width - mArc;
        final int Ey = mArrowHeight;
        final int Fx = width;
        final int Fy = mArrowHeight + mArc;
        final int Gx = width;
        final int Gy = height - mArrowHeight - mArc;
        final int Hx = width - mArc;
        final int Hy = height - mArrowHeight;
        final int Ix = arrowLeft ? mArrowOffsetX + mArrowWidth : mArrowOffsetX;
        final int Iy = height - mArrowHeight;
        final int Jx = mArrowOffsetX;
        final int Jy = height;
        final int Kx = arrowLeft ? mArrowOffsetX : mArrowOffsetX - mArrowWidth;
        final int Ky = height - mArrowHeight;
        final int Lx = mArc;
        final int Ly = height - mArrowHeight;
        final int Mx = 0;
        final int My = height - mArrowHeight - mArc;
        final int Nx = 0;
        final int Ny = mArrowHeight + mArc;

        Path ctx = new Path();
        ctx.moveTo(Ax, Ay);

        if (mArrowTop) {
            ctx.lineTo(Bx, By);
            ctx.lineTo(Cx, Cy);
            ctx.lineTo(Dx, Dy);
        }

        ctx.lineTo(Ex, Ey);
        ctx.arcTo(new RectF(Ex, Ey, Fx, Fy), 270, 90);
        ctx.lineTo(Gx, Gy);
        ctx.arcTo(new RectF(Gx - mArc, Gy, Hx + mArc, Hy), 0, 90);

        if (!mArrowTop) {
            ctx.lineTo(Ix, Iy);
            ctx.lineTo(Jx, Jy);
            ctx.lineTo(Kx, Ky);
        }
        ctx.lineTo(Lx, Ly);
        ctx.arcTo(new RectF(Mx, My, Lx, Ly), 90, 90); //B-C arc

        ctx.lineTo(Nx, Ny);
        ctx.arcTo(new RectF(Nx, Ay, Ax, Ny), 180, 90); //F-A arc
        ctx.close();

        if (mBgGradientColors != null){
            mBgPaint.setShader(new LinearGradient(0,0,0,height,mBgGradientColors,mBgGradientPositions,Shader.TileMode.CLAMP));
        }

        canvas.drawPath(ctx, mBgPaint);
        canvas.drawPath(ctx, mLinePaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.cf = cf;
    }

    public void setLinePaint(Paint paint) {
        this.mLinePaint = paint;
    }

    public void setBgPaint(Paint mBgPaint) {
        this.mBgPaint = mBgPaint;
    }

    public void setBgGradientColors(int[] colors){
        mBgGradientColors = colors;
    }

    public void setBgGradientPositions(float[] positions){
        mBgGradientPositions = positions;
    }

    public void setArrowOffsetX(int mArrowOffsetX) {
        this.mArrowOffsetX = mArrowOffsetX;
    }

    public void setArrowTop(boolean mArrowTop) {
        this.mArrowTop = mArrowTop;
    }

    public void setArc(int mArc) {
        this.mArc = mArc;
    }

    public void setArrowWidth(int mArrowWidth) {
        this.mArrowWidth = mArrowWidth;
    }

    public void setArrowHeight(int mArrowHeight) {
        this.mArrowHeight = mArrowHeight;
    }
}
