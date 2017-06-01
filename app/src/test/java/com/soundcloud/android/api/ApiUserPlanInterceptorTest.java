package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeDetector;
import com.soundcloud.java.net.HttpHeaders;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiUserPlanInterceptorTest {

    private static final String AUTOCOMPLETE_URL = "http://api-mobile.soundcloud.com/search/autocomplete?q=abc";
    @Mock private PlanChangeDetector planChangeDetector;
    @Mock private Interceptor.Chain chain;
    private ApiUserPlanInterceptor apiUserPlanInterceptor;

    @Before
    public void setUp() throws Exception {
        apiUserPlanInterceptor = new ApiUserPlanInterceptor(planChangeDetector);
    }

    @Test
    public void doesNotChangePlanOnAnonymousRequest() throws Exception {
        final Request request = new Request.Builder().url(AUTOCOMPLETE_URL).build();
        final Response response = responseBuilder(request).build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        final Response interceptedResponse = apiUserPlanInterceptor.intercept(chain);

        assertThat(interceptedResponse).isEqualTo(response);
        verifyZeroInteractions(planChangeDetector);
    }

    @Test
    public void changePlanOnAuthorizedRequest() throws Exception {
        final Request request = new Request.Builder().url(AUTOCOMPLETE_URL).addHeader(HttpHeaders.AUTHORIZATION, "123").build();
        final Response response = responseBuilder(request).addHeader("SC-Mob-UserPlan", "mid_tier").build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        final Response interceptedResponse = apiUserPlanInterceptor.intercept(chain);

        assertThat(interceptedResponse).isEqualTo(response);
        verify(planChangeDetector).handleRemotePlan(Plan.MID_TIER);
    }

    private Response.Builder responseBuilder(Request request) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_2).message("message").code(200);
    }
}
