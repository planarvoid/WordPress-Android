package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;

@Implements(Fragment.class)
public class ShadowV4Fragment extends ShadowFragment {

    @Implementation
    public boolean isAdded() {
        return isAttached();
    }

    public LayoutInflater getLayoutInflater(Bundle savedInstanceState){
        return Robolectric.getShadowApplication().getLayoutInflater();
    }

}
