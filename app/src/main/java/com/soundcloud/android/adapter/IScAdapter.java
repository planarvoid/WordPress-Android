package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;

public interface IScAdapter {
    long getItemId(int position);

    Content getContent();
}
