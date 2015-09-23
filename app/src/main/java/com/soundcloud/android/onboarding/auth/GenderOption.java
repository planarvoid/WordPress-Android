package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

public enum GenderOption {

    FEMALE(R.string.onboarding_gender_option_female, "female"),
    MALE(R.string.onboarding_gender_option_male, "male"),
    CUSTOM(R.string.onboarding_gender_option_custom, null),
    NO_PREF(R.string.onboarding_gender_option_nopref, null);

    private final int resId;
    @Nullable private final String apiKey;

    GenderOption(int resId, @Nullable String apiKey) {
        this.resId = resId;
        this.apiKey = apiKey;
    }

    public int getResId() {
        return resId;
    }

    @Nullable
    public String getApiValue(@Nullable String customGender) {
        if (this == CUSTOM && Strings.isNotBlank(customGender)) {
            return customGender;
        }
        return apiKey;
    }
}
