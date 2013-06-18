package com.soundcloud.android.view;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.List;


/**
 * A TextView that accepts a collection of Strings to display in one of the corresponding formats
 * xxx
 * xxx and yyy
 * xxx and z other(s)
 * xxx, yyy and z other(s)
 * <p/>
 * It is currently not very intelligent about how it finds ideal candidates. If we use this with large lists
 * we might want to re-visit this. Also it will just default to the last tried value if no valid candidate is found
 */
public class SingleLineCollectionTextView extends TextView {

    List<String> mDisplayItems;
    StringBuilder mDisplayNamesBuilder;

    public SingleLineCollectionTextView(Context context) {
        super(context);
    }

    public SingleLineCollectionTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SingleLineCollectionTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDisplayItems(List<String> displayItems) {
        mDisplayItems = displayItems;
        if (getMeasuredWidth() > 0) setCollectionText();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) setCollectionText();
    }


    private void setCollectionText() {
        if (mDisplayItems != null) {
            setMultiUserSubtext(getMeasuredWidth());
        }
    }

    @VisibleForTesting
    void setMultiUserSubtext(float maxWidth) {

        final int numUsers = mDisplayItems.size();
        if (numUsers == 1) {
            setText(mDisplayItems.get(0));
        } else {
            if (mDisplayNamesBuilder == null) {
                mDisplayNamesBuilder = new StringBuilder(); // lazy init
            } else {
                mDisplayNamesBuilder.setLength(0);
            }

            final Paint paint = getPaint();
            paint.setSubpixelText(true);

            String subTextCandidate = null;
            if (numUsers == 2) {
                subTextCandidate = getResources().getString(R.string.and_conjunction, mDisplayItems.get(0), mDisplayItems.get(1));
                if (paint.measureText(subTextCandidate) > maxWidth) {
                    // change to "xxx and 1 other"
                    mDisplayNamesBuilder.append(mDisplayItems.get(0));
                    subTextCandidate = appendSubtitleUsers(1);
                }

            } else if (numUsers > 2) {

                // try to fit the format xxx, yyy and z others
                boolean fits = false;
                for (int i = 0; i < mDisplayItems.size() && !fits; i++) {
                    for (int j = i + 1; j < mDisplayItems.size() && !fits; j++) {
                        mDisplayNamesBuilder.setLength(0);
                        mDisplayNamesBuilder.append(mDisplayItems.get(i)).append(", ")
                                .append(mDisplayItems.get(j));
                        subTextCandidate = appendSubtitleUsers(numUsers - 2);
                        fits = paint.measureText(subTextCandidate) <= maxWidth;
                    }
                }

                // try to fit the format xxx and z others
                if (!fits) {
                    for (int i = 0; i < mDisplayItems.size() && !fits; i++) {
                        mDisplayNamesBuilder.setLength(0);
                        mDisplayNamesBuilder.append(mDisplayItems.get(i));
                        subTextCandidate = appendSubtitleUsers(numUsers - 1);
                        fits = paint.measureText(subTextCandidate) <= maxWidth;
                    }
                }

            }
            setText(subTextCandidate);
        }
    }

    private String appendSubtitleUsers(int moreUsers) {
        mDisplayNamesBuilder.append(" ").append(getResources().getQuantityString(R.plurals.number_of_others, moreUsers, moreUsers));
        return mDisplayNamesBuilder.toString();
    }

}