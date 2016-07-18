package com.soundcloud.android.view.snackbar;

import com.androidadvance.topsnackbar.TSnackbar;
import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class TopSnackBarWrapper implements SnackBarWrapper {

    @Inject
    public TopSnackBarWrapper() {
    }

    @Override
    public void show(View anchor, Feedback feedback) {
        final TSnackbar snackbar = createSnackBar(anchor, feedback.getMessage(), getSnackbarDuration(feedback));
        final WeakReference<View.OnClickListener> actionListenerRef = feedback.getActionListener();
        if (actionListenerRef != null) {
            final View.OnClickListener actionListener = actionListenerRef.get();
            if (actionListener != null) {
                snackbar.setAction(feedback.getActionResId(), actionListener);
            }
        }
        snackbar.show();
    }

    @Override
    public int getSnackbarDuration(Feedback feedback) {
        return feedback.getLength() == Feedback.LENGTH_LONG ? TSnackbar.LENGTH_LONG : TSnackbar.LENGTH_SHORT;
    }

    private TSnackbar createSnackBar(View anchor, int resId, int length) {
        final TSnackbar snackBar = TSnackbar.make(anchor, resId, length);
        final View view = snackBar.getView();
        final TextView textView = (TextView) view.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text);
        view.setBackgroundColor(view.getResources().getColor(R.color.snack_bar_bg_light));
        textView.setTextColor(Color.WHITE);
        textView.setSingleLine();
        return snackBar;
    }
}
