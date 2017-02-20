package com.soundcloud.android.view.snackbar;

import com.soundcloud.java.optional.Optional;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class SnackBarWrapper {

    private final int bgColor;
    private final int textColor;
    private Optional<Snackbar> snackbar = Optional.absent();

    @Inject
    public SnackBarWrapper(Resources resources) {
        this.bgColor = resources.getColor(R.color.snack_bar_bg_dark);
        this.textColor = Color.WHITE;
    }

    public void show(View anchor, Feedback feedback) {
        if (snackbar.isPresent() && snackbar.get().isShown()) {
            dismissSnackbar(anchor, feedback);
        } else {
            showSnackbar(anchor, feedback);
        }
    }

    private void dismissSnackbar(final View anchor, final Feedback feedback) {
        snackbar.get().setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                showSnackbar(anchor, feedback);
            }
        });
        snackbar.get().dismiss();
    }

    private void showSnackbar(View anchor, Feedback feedback) {
        snackbar = Optional.of(createSnackBar(anchor, feedback.getMessage(), getSnackbarDuration(feedback)));
        final View.OnClickListener actionListenerRef = feedback.getActionListener();
        if (actionListenerRef != null) {
            snackbar.get().setAction(feedback.getActionResId(), actionListenerRef);
        }
        snackbar.get().show();
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
