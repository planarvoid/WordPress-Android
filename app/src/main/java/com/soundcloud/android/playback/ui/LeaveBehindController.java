package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;

import android.view.View;
import android.view.ViewStub;

public class LeaveBehindController {

    public void show(final View trackView) {
        View leaveBehind = showLeaveBehind(trackView);
        View close = leaveBehind.findViewById(R.id.leave_behind_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide(trackView);
            }
        });
    }

    private View showLeaveBehind(View trackView) {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        if (leaveBehind == null) {
            ViewStub stub = (ViewStub) trackView.findViewById(R.id.leave_behind_stub);
            return stub.inflate();
        } else {
            leaveBehind.setVisibility(View.VISIBLE);
            return leaveBehind;
        }
    }

    public void hide(View trackView) {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.GONE);
    }

}
