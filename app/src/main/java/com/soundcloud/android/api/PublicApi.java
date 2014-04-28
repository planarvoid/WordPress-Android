package com.soundcloud.android.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Stream;
import com.soundcloud.api.Token;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;

import android.content.Context;

import java.io.IOException;
import java.net.URI;
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

    public HttpResponse head(Request resource) throws IOException {
        return apiWrapper.head(resource);
    }

    public HttpResponse get(Request resource) throws IOException {
        return apiWrapper.get(resource);
    }

    public Token clientCredentials(String... scopes) throws IOException {
        return apiWrapper.clientCredentials(scopes);
    }

    public Token extensionGrantType(String grantType, String... scopes) throws IOException {
        return apiWrapper.extensionGrantType(grantType, scopes);
    }

    public Token login(String username, String password, String... scopes) throws IOException {
        return apiWrapper.login(username, password, scopes);
    }

    public URI authorizationCodeUrl(String... options) {
        return apiWrapper.authorizationCodeUrl(options);
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

    public void setToken(Token token) {
        apiWrapper.setToken(token);
    }

    public void setTokenListener(TokenListener listener) {
        apiWrapper.setTokenListener(listener);
    }

    public Token invalidateToken() {
        return apiWrapper.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return apiWrapper.getMapper();
    }

    public <T extends ScResource> T read(Request req) throws IOException {
        return apiWrapper.read(req);
    }

    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        return apiWrapper.update(request);
    }

    public <T extends ScResource> T create(Request request) throws IOException {
        return apiWrapper.create(request);
    }

    public <T extends ScResource> List<T> readList(Request req) throws IOException {
        return apiWrapper.readList(req);
    }

    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        return apiWrapper.readCollection(req);
    }

    @NotNull
    public <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException {
        return apiWrapper.readFullCollection(request, ch);
    }


    public <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        return apiWrapper.readListFromIds(request, ids);
    }

    public Token authorizationCode(String code, String... scopes) throws IOException {
        return apiWrapper.authorizationCode(code, scopes);
    }

    public void setDefaultContentType(String contentType) {
        apiWrapper.setDefaultContentType(contentType);
    }

    public void setDefaultAcceptEncoding(String encoding) {
        apiWrapper.setDefaultAcceptEncoding(encoding);
    }

    public HttpClient getHttpClient() {
        return apiWrapper.getHttpClient();
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        return apiWrapper.safeExecute(target, request);
    }

    public Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException {
        return apiWrapper.resolveStreamUrl(uri, skipLogging);
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
