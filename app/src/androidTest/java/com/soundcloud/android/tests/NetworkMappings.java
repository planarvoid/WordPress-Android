package com.soundcloud.android.tests;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.Log;

import android.content.Context;

public class NetworkMappings {

    public static final int MOCK_API_PORT = 8080;
    public static final String MOCK_API_ADDRESS = "http://127.0.0.1:" + MOCK_API_PORT;

    public static WireMockServer create(Context targetContext, boolean shouldLog, boolean failByDefault) {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration().port(NetworkMappings.MOCK_API_PORT);
        if (shouldLog) {
            wireMockConfiguration.notifier(new SoundcloudNotifier());
        }

        return create(wireMockConfiguration, targetContext, failByDefault);
    }

    public static WireMockServer create(WireMockConfiguration options, Context context, boolean failByDefault) {
        WireMockServer wireMockServer = new WireMockServer(options);
        addDefaultMapping(context, wireMockServer, failByDefault);
        return wireMockServer;
    }

    public static void addDefaultMapping(Context context, WireMockServer wireMockServer, boolean failByDefault) {
        final String baseUrl = context.getResources().getString(R.string.mobile_api_base_url);

        wireMockServer.loadMappingsUsing(stubMappings -> {
            RequestPattern requestPattern = newRequestPattern(ANY, anyUrl()).build();
            ResponseDefinition responseDef = getResponseDefinitionDefault(failByDefault, baseUrl);

            StubMapping proxyBasedMapping = new StubMapping(requestPattern, responseDef);
            proxyBasedMapping.setPriority(10); // Make it low priority so that existing stubs will take precedence
            stubMappings.addMapping(proxyBasedMapping);
        });
    }

    private static ResponseDefinition getResponseDefinitionDefault(boolean failByDefault, String baseUrl) {
        return failByDefault ? responseDefinition().withFault(Fault.MALFORMED_RESPONSE_CHUNK).build()
                             : responseDefinition().proxiedFrom(baseUrl).build();
    }

    private static class SoundcloudNotifier implements Notifier {
        @Override
        public void info(String s) {
            Log.i(s);
        }

        @Override
        public void error(String s) {
            Log.e(s);
        }

        @Override
        public void error(String s, Throwable throwable) {
            Log.e(s, throwable);
        }
    }
}
