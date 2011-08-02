package com.soundcloud.android.view;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.InputObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class UserlistBrowser extends RelativeLayout {
    private static final String TAG = "UserlistBrowser";

    private WorkspaceView mWorkspaceView;
    private RelativeLayout mLabelHolder;
    //private ArrayList<TextView mLabels;

    private List<TabLabel> tabLabels;

    private static class TabLabel {
        public String tag;
        public float index;
        public TextView textView;
        public int marginOffset;

    }

    public UserlistBrowser(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.user_list_browser, this);

        mLabelHolder = (RelativeLayout) findViewById(R.id.label_holder);

        tabLabels = new ArrayList<TabLabel>();

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
        mWorkspaceView.setOnScrollListener(new WorkspaceView.OnScrollListener() {
            @Override
            public void onScroll(float screenFraction) {
                setLabels(screenFraction);
            }
        }, true);


    }

    public void addView(View view, String label){
        mWorkspaceView.addView(view);

        TextView labelTxt = new TextView(getContext());
        labelTxt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.FILL_PARENT));
        labelTxt.setGravity(Gravity.CENTER);
        labelTxt.setText(label);
        mLabelHolder.addView(labelTxt);
        labelTxt.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(mLabelHolder.getHeight(), MeasureSpec.EXACTLY));


        TabLabel tabLabel = new TabLabel();
        tabLabel.textView = labelTxt;
        tabLabel.marginOffset = -labelTxt.getMeasuredWidth()/2;
        tabLabel.index = (float) mLabelHolder.getChildCount() - 1;
        tabLabels.add(tabLabel);

    }

    private void measureView(View child) {
           ViewGroup.LayoutParams p = child.getLayoutParams();
           if (p == null) {
               p = new ViewGroup.LayoutParams(
                       ViewGroup.LayoutParams.FILL_PARENT,
                       ViewGroup.LayoutParams.WRAP_CONTENT);
           }



       }


    public void initWorkspace(int initialScreen){
        mWorkspaceView.initWorkspace(initialScreen);
    }

    private void setLabels(float screenFraction){

        final int w = getMeasuredWidth();
        for (TabLabel tl : tabLabels){
             if (tl.index < screenFraction - 1.5 || tl.index > screenFraction + 1.5){
                 tl.textView.setVisibility(View.GONE);
             } else {
                 ((RelativeLayout.LayoutParams) tl.textView.getLayoutParams()).leftMargin = (int) (w/2 +
                         tl.marginOffset + (((tl.index - screenFraction)/1.5)*w));
                 tl.textView.setVisibility(View.VISIBLE);
                 tl.textView.requestLayout();
             }
        }

        //Log.i("asdf", "Set Labels " + screenFraction);
    }

}