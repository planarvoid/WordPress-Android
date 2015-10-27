package com.soundcloud.android.sync.activities;

import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserHolder;

interface ApiActivity extends UserHolder, Timestamped {

    Urn getUserUrn();

}
