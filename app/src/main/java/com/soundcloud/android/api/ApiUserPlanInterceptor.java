package com.soundcloud.android.api;

import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeDetector;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;

import java.io.IOException;

class ApiUserPlanInterceptor implements Interceptor {

    private static final String USER_PLAN_HEADER = "SC-Mob-UserPlan";

    private final PlanChangeDetector planChangeDetector;

    ApiUserPlanInterceptor(PlanChangeDetector planChangeDetector) {
        this.planChangeDetector = planChangeDetector;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Response response = chain.proceed(chain.request());
        final Plan remotePlan = Plan.fromId(response.header(USER_PLAN_HEADER));
        planChangeDetector.handleRemotePlan(remotePlan);
        return response;
    }
}
