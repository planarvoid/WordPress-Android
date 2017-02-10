package com.soundcloud.android.view.snackbar;


import com.soundcloud.android.feedback.Feedback;

import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class SnackBarWrapper {

    private final int bgColor;
    private final int textColor;

    public SnackBarWrapper(int bgColor, int textColor) {
        this.bgColor = bgColor;
        this.textColor = textColor;
    }

    public void show(View anchor, Feedback feedback) {
        final Snackbar snackbar = createSnackBar(anchor, feedback.getMessage(), getSnackbarDuration(feedback));
        final WeakReference<View.OnClickListener> actionListenerRef = feedback.getActionListener();
        if (actionListenerRef != null) {
            final View.OnClickListener actionListener = actionListenerRef.get();
            if (actionListener != null) {
                snackbar.setAction(feedback.getActionResId(), actionListener);
            }
        }
        snackbar.show();
    }

    public int getSnackbarDuration(Feedback feedback) {
        return feedback.getLength() == Feedback.LENGTH_LONG ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT;
    }

    private Snackbar createSnackBar(View anchor, int resId, int length) {
        final Snackbar snackBar = Snackbar.make(anchor, resId, length);
        final View view = snackBar.getView();
        TextView textView = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        view.setBackgroundColor(bgColor);
        textView.setTextColor(textColor);
        textView.setSingleLine();
        return snackBar;
    }
}
