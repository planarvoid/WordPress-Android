package com.soundcloud.android.api;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeDetector;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.net.HttpHeaders;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import java.io.IOException;

class ApiUserPlanInterceptor implements Interceptor {

    private static final String USER_PLAN_HEADER = "SC-Mob-UserPlan";
    private static final String TAG = ConfigurationOperations.TAG;

    private final PlanChangeDetector planChangeDetector;

    @Inject
    ApiUserPlanInterceptor(PlanChangeDetector planChangeDetector) {
        this.planChangeDetector = planChangeDetector;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        if (isAuthorizedRequest(request)) {
            final Plan remotePlan = Plan.fromId(response.header(USER_PLAN_HEADER));
            Log.d(TAG, "Got remote plan: " + remotePlan + " for req=" + request);
            planChangeDetector.handleRemotePlan(remotePlan);
        }
        return response;
    }

    private boolean isAuthorizedRequest(Request request) {
        return request.headers().names().contains(HttpHeaders.AUTHORIZATION);
    }
}
