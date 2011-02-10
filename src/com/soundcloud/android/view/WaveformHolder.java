
package com.soundcloud.android.view;

import com.soundcloud.android.objects.Comment;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class WaveformHolder extends RelativeLayout {

    private static final String TAG = "WaveformHolder";
    
    private ArrayList<Comment> mComments;

    public WaveformHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setComments(ArrayList<Comment> comments){
        mComments = comments;
    }
    

}
