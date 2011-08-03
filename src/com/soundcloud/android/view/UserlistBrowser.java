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
import android.graphics.LinearGradient;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
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
import java.util.zip.Inflater;

public class UserlistBrowser extends RelativeLayout {
    private static final String TAG = "UserlistBrowser";

    private WorkspaceView mWorkspaceView;
    private RelativeLayout mLabelHolder;
    //private ArrayList<TextView mLabels;

    private List<TabLabel> tabLabels;

    private int mHolderWidth;
    private int mHolderPad;
    private int mMiddleLow;
    private int mMiddleHigh;
    private float mCurrentFraction = -1f;

    private View mLeftArrow;
    private View mRightArrow;

    public UserlistBrowser(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.user_list_browser, this);

        mLabelHolder = (RelativeLayout) findViewById(R.id.label_holder);
        tabLabels = new ArrayList<TabLabel>();

        mLeftArrow = findViewById(R.id.left_arrow);
        mRightArrow = findViewById(R.id.right_arrow);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
        mWorkspaceView.setOnScrollListener(new WorkspaceView.OnScrollListener() {
            @Override
            public void onScroll(float screenFraction) {
                setLabels(screenFraction);
            }
        }, true);
        mHolderPad = (int) (5 * getResources().getDisplayMetrics().density);

    }





    public void addView(View view, String label, String tag){
        mWorkspaceView.addView(view);
        TextView labelTxt = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.user_list_browser_label_txt,null);
        labelTxt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
        mLabelHolder.addView(labelTxt);
        tabLabels.add(new TabLabel(labelTxt,label,tag, mLabelHolder.getChildCount() - 1));
    }


    public void initWorkspace(int initialScreen){
        mWorkspaceView.initWorkspace(initialScreen);
        setLabels(initialScreen);
    }

    private void setLabels(float screenFraction){
        if (screenFraction == mCurrentFraction || mHolderWidth == 0) return;

        for (TabLabel tl : tabLabels){
             if (tl.index < screenFraction - 1.5 || tl.index > screenFraction + 1.5){
                 tl.hide();
             } else {
                 tl.setPosition(screenFraction,mHolderWidth,mHolderPad, mMiddleLow, mMiddleHigh);
             }
        }

        mLeftArrow.setVisibility(screenFraction < .5 ? INVISIBLE : VISIBLE);
        mRightArrow.setVisibility(screenFraction > tabLabels.size() - 1.5 ? INVISIBLE : VISIBLE);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            mHolderWidth = mLabelHolder.getMeasuredWidth();
            mMiddleLow = (int) (mHolderWidth/2 - 10 * getResources().getDisplayMetrics().density);
            mMiddleHigh = (int) (mHolderWidth/2 + 10 * getResources().getDisplayMetrics().density);
            setLabels(mWorkspaceView.getCurrentScreen());
        }
        super.onLayout(changed, l, t, r, b);
    }

    public void setOnScreenChangedListener(WorkspaceView.OnScreenChangeListener listener){
        mWorkspaceView.setOnScreenChangeListener(listener);
    }

    public void setCurrentScreenByTag(String tag) {
        for (TabLabel tl : tabLabels) {
            if (tl.tag.contentEquals(tag)) {
                mWorkspaceView.setCurrentScreenNow((int) tl.index, true);
            }
        }
    }

    private class TabLabel {
        public String tag;
        public float index;

        private int marginOffset;
        private TextView textView;
        private String label;
        private String boldLabel;
        private int currentPosition;
        private boolean bold = true;

        public TabLabel(TextView textView, String label, String tag, int index){
            this.textView = textView;
            this.label = label;
            this.tag = tag;
            this.index = index;

            boldLabel = "<b>" + label + "</b>";

            textView.setText(label);
            computeMarginOffset();
        }

        private void computeMarginOffset(){
            marginOffset = (int) (-(textView.getPaint().measureText(textView.getText().toString()))/2);
        }

        public void setPosition(float screenFraction, int holderWidth, int holderPad, int middleLow, int middleHigh){

            final int middlePos = (int) (holderWidth / 2 +
                    (((index - screenFraction) / 1.5) * holderWidth));

            if (middlePos > middleLow && middlePos < middleHigh) {
                if (!bold) {
                    textView.setTypeface(null, Typeface.BOLD);
                    computeMarginOffset();
                    bold = true;
                }
            } else if (bold) {
                textView.setTypeface(null, Typeface.NORMAL);
                computeMarginOffset();
                bold = false;
            }

            ((LayoutParams) textView.getLayoutParams()).leftMargin = Math.min(holderWidth + marginOffset * 2 - holderPad,
                    Math.max(holderPad, middlePos + marginOffset));
            textView.setVisibility(View.VISIBLE);
            textView.requestLayout();
        }

        public void hide(){
           textView.setVisibility(View.GONE);
        }
    }

}