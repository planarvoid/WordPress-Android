
package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class CommentBubbleArrow extends View {

    private Paint mArrowPaint;
    
    private Matrix mArrowMatrix;

    private Bitmap mRightArrow;
    
    private Bitmap mLeftArrow;
    
    private Bitmap mCurrentArrow;
    
    private static int LEFT_ARROW_MARGIN = 2;
    private static int RIGHT_ARROW_MARGIN = 11;

    public CommentBubbleArrow(Context context) {
        
        super(context);
        
        mArrowPaint = new Paint();
        mArrowPaint.setAntiAlias(false);
        mArrowPaint.setFilterBitmap(true);
        
        LEFT_ARROW_MARGIN *= getContext().getResources().getDisplayMetrics().density;
        RIGHT_ARROW_MARGIN *= getContext().getResources().getDisplayMetrics().density;
        
        mArrowMatrix = new Matrix();        
        mRightArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.comment_bubble_arrow_r);
        mLeftArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.comment_bubble_arrow_l);

        this.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, mRightArrow.getHeight()));
        ((RelativeLayout.LayoutParams)this.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mCurrentArrow, mArrowMatrix, mArrowPaint);
    }
    
    
    
    public void setPosition(float x) {
        x = Math.min(Math.max(CommentBubble.CORNER_MARGIN + LEFT_ARROW_MARGIN, x), CommentBubble.HARD_WIDTH - CommentBubble.CORNER_MARGIN - RIGHT_ARROW_MARGIN);
        
        if (x > CommentBubble.HARD_WIDTH/2 - RIGHT_ARROW_MARGIN){
            mCurrentArrow = mRightArrow;
            mArrowMatrix.setTranslate(x-RIGHT_ARROW_MARGIN, 0);
        } else {
            mCurrentArrow = mLeftArrow;
            mArrowMatrix.setTranslate(x - LEFT_ARROW_MARGIN, 0);
        }
        
        invalidate();
    }

}
