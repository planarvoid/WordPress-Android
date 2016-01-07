package com.soundcloud.android.profile;

import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.optional.Optional;

public interface ApiEntityHolderSource {
    Optional<ApiEntityHolder> getEntityHolder();
}
