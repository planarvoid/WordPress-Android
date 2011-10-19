package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class UserlistLayout extends RelativeLayout {
    private static final String TAG = "UserlistLayout";

    private final WorkspaceView mWorkspaceView;
    private final RelativeLayout mLabelHolder;

    private final List<TabLabel> tabLabels;

    private int mHolderWidth;
    private final int mHolderPad;
    private int mMiddleLow;
    private int mMiddleHigh;

    private final View mLeftArrow;
    private final View mRightArrow;

    public UserlistLayout(Context context, AttributeSet attrs) {
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
        if (mHolderWidth == 0) return;

        for (TabLabel tl : tabLabels){
             if (tl.index < screenFraction - 1.5 || tl.index > screenFraction + 1.5){
                 tl.hide();
             } else {
                 tl.setPosition(screenFraction,mHolderWidth,mHolderPad, mMiddleLow, mMiddleHigh);
             }
        }

        mLeftArrow.setVisibility(screenFraction < 1.5 ? INVISIBLE : VISIBLE);
        mRightArrow.setVisibility(screenFraction > tabLabels.size() - 2.5 ? INVISIBLE : VISIBLE);
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
        mWorkspaceView.setOnScreenChangeListener(listener, false);
    }

    public void setCurrentScreenByTag(String tag) {
        for (TabLabel tl : tabLabels) {
            if (tl.tag.contentEquals(tag)) {
                mWorkspaceView.setCurrentScreenNow((int) tl.index, true);
            }
        }
    }

    public String getCurrentTag(){
        return tabLabels.get(mWorkspaceView.getCurrentScreen()).tag;

    }

    public Object getCurrentWorkspaceView() {
        return mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen());
    }

    public int getCurrentWorkspaceIndex() {
        return mWorkspaceView.getCurrentScreen();
    }

    private static class TabLabel {

        public final String tag;
        public final float index;

        private int mMarginOffset;
        private final TextView mTextView;
        private final String mLabel;
        private int mCurrentPosition;
        private boolean mBold = true;

        public TabLabel(TextView textView, String label, String tag, int index){
            this.mTextView = textView;
            this.mLabel = label;
            this.tag = tag;
            this.index = index;

            textView.setText(label);
            computeMarginOffset();
        }

        private void computeMarginOffset(){
            mMarginOffset = (int) (-(mTextView.getPaint().measureText(mTextView.getText().toString()))/2);
        }

        public void setPosition(float screenFraction, int holderWidth, int holderPad, int middleLow, int middleHigh){
            final int middlePos = (int) (holderWidth / 2 +
                    (((index - screenFraction) / 1.5) * holderWidth));

            if (middlePos > middleLow && middlePos < middleHigh) {
                if (!mBold) {
                    mTextView.setTypeface(null, Typeface.BOLD);
                    computeMarginOffset();
                    mBold = true;
                }
            } else if (mBold) {
                mTextView.setTypeface(null, Typeface.NORMAL);
                computeMarginOffset();
                mBold = false;
            }

            ((LayoutParams) mTextView.getLayoutParams()).leftMargin = Math.min(holderWidth + mMarginOffset * 2 - holderPad,
                    Math.max(holderPad, middlePos + mMarginOffset));
            mTextView.setVisibility(View.VISIBLE);
            mTextView.requestLayout();
        }

        public void hide(){
           mTextView.setVisibility(View.GONE);
        }
    }
}