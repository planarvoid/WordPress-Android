package com.soundcloud.android.view;

import android.graphics.drawable.Drawable;
import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.soundcloud.android.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class UserlistLayout extends RelativeLayout {
    private final WorkspaceView mWorkspaceView;
    private final RelativeLayout mLabelHolder;

    private final List<TabLabel> tabLabels;

    private int mHolderWidth;
    private final int mHolderPad;
    private int mMiddleLow;
    private int mMiddleHigh;

    private final View mLeftArrow;
    private final View mRightArrow;

    private final int mSpacer;

    public UserlistLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.user_list_browser, this);

        mSpacer = (int) (context.getResources().getDisplayMetrics().density * 10);

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
        mHolderPad = (int) (3 * getResources().getDisplayMetrics().density);
    }

    public void addView(View view, String label, Drawable drawable, String tag) {
        mWorkspaceView.addView(view);
        TextView labelTxt = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.user_list_browser_label_txt, null);
        labelTxt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
        labelTxt.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        labelTxt.setCompoundDrawablePadding((int) (getResources().getDisplayMetrics().density * 5));
        AndroidUtils.setTextShadowForGrayBg(labelTxt);
        mLabelHolder.addView(labelTxt);
        tabLabels.add(new TabLabel(labelTxt, label, tag, mLabelHolder.getChildCount() - 1));
    }

    public void initWorkspace(int initialScreen){
        mWorkspaceView.initWorkspace(initialScreen);
        setLabels(initialScreen);
    }

    private void setLabels(float screenFraction){
        if (mHolderWidth == 0) return;

        int leftCutoff = -1;
        int rightCutoff = -1;
        for (TabLabel tl : tabLabels){
            if (Math.abs(tl.index - screenFraction) <= .7){
                tl.setPosition(screenFraction,mHolderWidth,mHolderPad, mMiddleLow, mMiddleHigh, -1, -1, mSpacer);
                if (tl.index > screenFraction){
                    rightCutoff = tl.getRight();
                } else if (tl.index <= screenFraction){
                    leftCutoff = tl.getLeft();
                }
            }
        }
        for (TabLabel tl : tabLabels){
             if (tl.index < screenFraction - 1.7 || tl.index > screenFraction + 1.7){
                 tl.hide();
             } else if (Math.abs(tl.index - screenFraction) > .7) {
                 tl.setPosition(screenFraction,mHolderWidth,mHolderPad, mMiddleLow, mMiddleHigh, leftCutoff, rightCutoff, mSpacer);
             }
        }

        mLeftArrow.setVisibility(screenFraction < 1.7 ? INVISIBLE : VISIBLE);
        mRightArrow.setVisibility(screenFraction > tabLabels.size() - 2.7 ? INVISIBLE : VISIBLE);
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
        TabLabel label = findLabel(tag);
        if (label != null) {
            mWorkspaceView.setCurrentScreenNow(label.index, true);
            final View currentScreen = mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen());
            if (currentScreen instanceof ScTabView){
                ((ScTabView) currentScreen).setToTop();
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

    private TabLabel findLabel(String tag) {
        for (TabLabel tl : tabLabels) {
            if (tl != null && tl.tag != null && tl.tag.equals(tag)) {
                return tl;
            }
        }
        return null;
    }

    public void initByTag(String tag) {
        TabLabel label = findLabel(tag);
        if (label != null) {
            mWorkspaceView.initWorkspace(label.index);
        }
    }

    private static class TabLabel {
        public final String tag;
        public final int index;

        private int mMarginOffset;
        private final TextView mTextView;
        private boolean mBold = false;

        private int mCurrentLeftMargin;
        private int mCurrentWidth;

        public TabLabel(TextView textView, String label, String tag, int index) {
            this.mTextView = textView;
            this.tag = tag;
            this.index = index;

            textView.setText(label);
            computeMarginOffset();
        }

        private void computeMarginOffset() {
            Drawable drawable = mTextView.getCompoundDrawables()[0];
            mCurrentWidth = (int) (mTextView.getPaint().measureText(mTextView.getText().toString()) +
                    drawable.getMinimumWidth() + mTextView.getCompoundDrawablePadding());
            mMarginOffset = -mCurrentWidth/2;
        }

        public void setPosition(float screenFraction,
                                int holderWidth,
                                int holderPad,
                                int middleLow,
                                int middleHigh,
                                int cutoffLeft,
                                int cutoffRight,
                                int spacer) {

            final int middlePos = (int) (holderWidth / 2 + (((index - screenFraction) / 1.7) * holderWidth));
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

            mCurrentLeftMargin = Math.min(holderWidth + mMarginOffset * 2 - holderPad,
                    Math.max(holderPad, middlePos + mMarginOffset));

            if (cutoffLeft != -1 && mCurrentLeftMargin <= cutoffLeft){
                mCurrentLeftMargin = Math.min(cutoffLeft - mCurrentWidth - spacer, mCurrentLeftMargin);
            } else if (cutoffRight != -1 && mCurrentLeftMargin + mCurrentWidth >= cutoffRight){
                mCurrentLeftMargin = Math.max(cutoffRight + spacer, mCurrentLeftMargin);
            }

            if (mCurrentLeftMargin != ((LayoutParams) mTextView.getLayoutParams()).leftMargin || (mTextView.getVisibility() != View.VISIBLE)){
                ((LayoutParams) mTextView.getLayoutParams()).leftMargin = mCurrentLeftMargin;
                mTextView.setVisibility(View.VISIBLE);
                mTextView.requestLayout();
            }
        }

        public int getLeft(){
            return mCurrentLeftMargin;
        }

        public int getRight(){
            return mCurrentLeftMargin + mCurrentWidth;
        }

        public void hide(){
           mTextView.setVisibility(View.GONE);
        }
    }
}