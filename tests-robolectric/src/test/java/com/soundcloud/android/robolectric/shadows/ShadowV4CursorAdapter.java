package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowCursorAdapter;

import android.support.v4.widget.CursorAdapter;

@Implements(CursorAdapter.class)
public class ShadowV4CursorAdapter extends ShadowCursorAdapter {
}
