package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.onboarding.auth.tasks.TaskResultKind.AGE_RESTRICTED;

import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class AgeRestrictionAuthResult extends AuthTaskResult {

    private final String minimumAge;

    private AgeRestrictionAuthResult(@NotNull TaskResultKind kind,
                                     AuthResponse authResponse,
                                     SignupVia signupVia,
                                     Exception exception,
                                     Bundle loginBundle,
                                     String errorMessage,
                                     String minimumAge) {
        super(kind, authResponse, signupVia, exception, loginBundle, errorMessage);
        this.minimumAge = minimumAge;
    }

    private AgeRestrictionAuthResult(String minimumAge) {
        this(AGE_RESTRICTED, null, null, null, null, null, minimumAge);
    }

    public static AuthTaskResult create(String minimumAge) {
        return new AgeRestrictionAuthResult(minimumAge);
    }

    public String getMinimumAge() {
        return minimumAge;
    }

    @Override
    public boolean wasAgeRestricted() {
        return true;
    }
}
