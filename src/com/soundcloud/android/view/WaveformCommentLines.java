
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import java.util.List;

public class WaveformCommentLines extends View {

    private static final String TAG = "WaveformHolder";

    private Paint mPaint;
    private Paint mActiveLinePaint;
    
    private long mDuration;
    private List<Comment> mComments;
    private Comment mCurrentComment;

    public WaveformCommentLines(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.commentLine));
        
        mActiveLinePaint= new Paint();
        mActiveLinePaint.setColor(getResources().getColor(R.color.activeCommentLine));
        
    }
    
    public void setTrackData(long duration, List<Comment> comments){
        mDuration = duration;
        mComments = comments;
    }
    
    public void clearTrackData(){
        mComments = null;
        mCurrentComment = null;
        invalidate();
    }
    
    public void setCurrentComment(Comment c){
        mCurrentComment = c;
        invalidate();
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);
        
        if (changed && mComments != null){
            for (Comment comment : mComments){
                if (comment.xPos == -1) comment.calculateXPos(getWidth(), mDuration);
            }
        }
        
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (mComments != null)
        for (Comment comment : mComments){
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), mPaint);
        }
        
        if (mCurrentComment != null){
            canvas.drawLine(mCurrentComment.xPos, 0, mCurrentComment.xPos, getHeight(), mActiveLinePaint);
        }
    }
    

}
