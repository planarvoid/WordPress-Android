package com.soundcloud.android.collections;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.VisibleForTesting;
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

    private List<String> displayItems;

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
        this.displayItems = displayItems;
        if (getMeasuredWidth() > 0) {
            setCollectionText();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            setCollectionText();
        }
    }


    private void setCollectionText() {
        if (displayItems != null) {
            setTextFromCollection(getMeasuredWidth());
        }
    }

    @VisibleForTesting
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    void setTextFromCollection(float maxWidth) {

        final int numItems = displayItems.size();
        if (numItems == 1) {
            setText(displayItems.get(0));
        } else {
            StringBuilder displayNamesBuilder = new StringBuilder(); // lazy init

            final Paint paint = getPaint();
            paint.setSubpixelText(true);

            String candidate = null;
            if (numItems == 2) {
                candidate = getResources().getString(R.string.and_conjunction, displayItems.get(0), displayItems.get(1));
                if (paint.measureText(candidate) > maxWidth) {
                    // change to "xxx and 1 other"
                    displayNamesBuilder.append(displayItems.get(0));
                    candidate = appendRemaining(displayNamesBuilder, 1);
                }

            } else if (numItems > 2) {

                // try to fit the format xxx, yyy and z others
                boolean fits = false;
                for (int i = 0; i < displayItems.size() && !fits; i++) {
                    for (int j = i + 1; j < displayItems.size() && !fits; j++) {
                        displayNamesBuilder.setLength(0);
                        displayNamesBuilder.append(displayItems.get(i)).append(", ")
                                .append(displayItems.get(j));
                        candidate = appendRemaining(displayNamesBuilder, numItems - 2);
                        fits = paint.measureText(candidate) <= maxWidth;
                    }
                }

                // try to fit the format xxx and z others
                if (!fits) {
                    for (int i = 0; i < displayItems.size() && !fits; i++) {
                        displayNamesBuilder.setLength(0);
                        displayNamesBuilder.append(displayItems.get(i));
                        candidate = appendRemaining(displayNamesBuilder, numItems - 1);
                        fits = paint.measureText(candidate) <= maxWidth;
                    }
                }

            }
            setText(candidate);
        }
    }

    private String appendRemaining(StringBuilder displayNamesBuilder, int moreItems) {
        displayNamesBuilder.append(' ').append(getResources().getQuantityString(R.plurals.number_of_others, moreItems, moreItems));
        return displayNamesBuilder.toString();
    }

}
