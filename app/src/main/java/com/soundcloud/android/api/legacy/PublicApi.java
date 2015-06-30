package com.soundcloud.android.api.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.oauth.Token;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;

import android.content.Context;

import java.io.IOException;
import java.util.List;

public class PublicApi implements PublicCloudAPI {

    private final PublicApiWrapper apiWrapper;

    public PublicApi(Context context) {
        this(PublicApiWrapper.getInstance(context));
    }

    @VisibleForTesting
    protected PublicApi(PublicApiWrapper wrapper) {
        apiWrapper = wrapper;
    }

    public HttpResponse get(Request resource) throws IOException {
        return apiWrapper.get(resource);
    }

    public Token clientCredentials(String... scopes) throws IOException {
        return apiWrapper.clientCredentials(scopes);
    }

    public Token extensionGrantType(String grantType) throws IOException {
        return apiWrapper.extensionGrantType(grantType);
    }

    public Token login(String username, String password) throws IOException {
        return apiWrapper.login(username, password);
    }

    public HttpResponse put(Request request) throws IOException {
        return apiWrapper.put(request);
    }

    public HttpResponse post(Request request) throws IOException {
        return apiWrapper.post(request);
    }

    public HttpResponse delete(Request request) throws IOException {
        return apiWrapper.delete(request);
    }

    public Token refreshToken() throws IOException {
        return apiWrapper.refreshToken();
    }

    public Token getToken() {
        return apiWrapper.getToken();
    }

    public long resolve(String uri) throws IOException {
        return apiWrapper.resolve(uri);
    }

    public Token invalidateToken() {
        return apiWrapper.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return apiWrapper.getMapper();
    }

    public <T extends PublicApiResource> T read(Request req) throws IOException {
        return apiWrapper.read(req);
    }

    public <T extends PublicApiResource> T update(Request request) throws IOException {
        return apiWrapper.update(request);
    }

    public <T extends PublicApiResource> T create(Request request) throws IOException {
        return apiWrapper.create(request);
    }

    public <T extends PublicApiResource> List<T> readList(Request req) throws IOException {
        return apiWrapper.readList(req);
    }

    public <T extends PublicApiResource> PublicApiResource.ResourceHolder<T> readCollection(Request req) throws IOException {
        return apiWrapper.readCollection(req);
    }

    @NotNull
    public <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException {
        return apiWrapper.readFullCollection(request, ch);
    }


    public <T extends PublicApiResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        return apiWrapper.readListFromIds(request, ids);
    }

    public HttpClient getHttpClient() {
        return apiWrapper.getHttpClient();
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        return apiWrapper.safeExecute(target, request);
    }

    @Override
    public String getUserAgent() {
        return apiWrapper.getUserAgent();
    }

    @Override
    public Env getEnv() {
        return apiWrapper.getEnv();
    }
}
