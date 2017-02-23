package com.soundcloud.android.cast;

import com.soundcloud.android.R;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteChooserDialog;
import android.support.v7.app.MediaRouteChooserDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;

public class CustomMediaRouteDialogFactory extends MediaRouteDialogFactory {

    @NonNull
    @Override
    public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
        return new CastMediaRouteChooserDialogFragment();
    }

    public static class CastMediaRouteChooserDialogFragment extends MediaRouteChooserDialogFragment {
        @Override
        public MediaRouteChooserDialog onCreateChooserDialog(Context context, Bundle savedInstanceState) {
            return new MediaRouteChooserDialog(context, R.style.CastDialogStyle);
        }
    }
}