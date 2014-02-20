package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.R;
import android.support.v4.app.ListFragment;
import android.widget.ListView;

@Implements(ListFragment.class)
public class ShadowV4ListFragment extends ShadowV4Fragment {

    @Implementation
    public ListView getListView() {
        return (ListView) getView().findViewById(R.id.list);
    }

}
