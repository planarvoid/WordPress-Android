package com.soundcloud.android.tests;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

public class NetworkMappings {

    static final int MOCK_API_PORT = 8080;
    public static final String MOCK_API_ADDRESS = "http://127.0.0.1:" + MOCK_API_PORT;

    static WireMockServer create(WireMockConfiguration options, String baseUrl) {
        WireMockServer wireMockServer = new WireMockServer(options);
        wireMockServer.loadMappingsUsing(stubMappings -> {
            RequestPattern requestPattern = newRequestPattern(ANY, anyUrl()).build();
            ResponseDefinition responseDef = responseDefinition()
                    .proxiedFrom(baseUrl)
                    .build();

            StubMapping proxyBasedMapping = new StubMapping(requestPattern, responseDef);
            proxyBasedMapping.setPriority(10); // Make it low priority so that existing stubs will take precedence
            stubMappings.addMapping(proxyBasedMapping);
        });
        return wireMockServer;
    }
}
