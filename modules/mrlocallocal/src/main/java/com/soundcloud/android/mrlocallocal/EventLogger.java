package com.soundcloud.android.mrlocallocal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.soundcloud.android.mrlocallocal.data.LoggedEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class EventLogger {

    private final ObjectMapper jsonMapper;
    private final WireMockServer wireMockServer;
    private final String eventGatewayUrl;

    EventLogger(WireMockServer wireMockServer, String eventGatewayUrl) {
        this.wireMockServer = wireMockServer;
        this.eventGatewayUrl = eventGatewayUrl;
        jsonMapper = new ObjectMapper(new JsonFactory());

        initWireMockServer();
    }

    List<LoggedEvent> getLoggedEvents() throws IOException {
        List<LoggedRequest> requests = wireMockServer.findAll(postRequestedFor(urlMatching(eventGatewayUrl)));
        List<LoggedEvent> loggedEvents = new ArrayList<>(requests.size());
        for (LoggedRequest request : requests) {
            List<LoggedEvent> mappedEvents = jsonMapper.readValue(request.getBody(), new TypeReference<List<LoggedEvent>>() {});
            loggedEvents.addAll(mappedEvents);
        }
        return loggedEvents;
    }

    private void initWireMockServer() {
        wireMockServer.stubFor(post(urlMatching(eventGatewayUrl))
                                       .willReturn(aResponse().withStatus(200)));
    }
}
