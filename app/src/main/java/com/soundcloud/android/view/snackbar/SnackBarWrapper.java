package com.soundcloud.android.view.snackbar;


import com.soundcloud.android.feedback.Feedback;

import android.view.View;

public interface SnackBarWrapper {
    void show(View anchor, Feedback feedback);

    int getSnackbarDuration(Feedback feedback);
}
