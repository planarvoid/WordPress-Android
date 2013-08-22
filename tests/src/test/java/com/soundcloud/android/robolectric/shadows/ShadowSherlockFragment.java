package com.soundcloud.android.robolectric.shadows;

import com.actionbarsherlock.app.SherlockFragment;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowFragment;

@Implements(SherlockFragment.class)
public class ShadowSherlockFragment extends ShadowFragment {

    @Implementation
    public boolean isAdded() {
        return isAttached();
    }

}
