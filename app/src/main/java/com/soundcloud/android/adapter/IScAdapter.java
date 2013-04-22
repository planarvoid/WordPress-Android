package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.quickaction.QuickAction;

public interface IScAdapter {
    long getItemId(int position);

    Content getContent();

    QuickAction getQuickActionMenu();
}
