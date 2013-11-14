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

    private PublicApiWrapper mApiWrapper;

    public PublicApi(Context context){
        this(PublicApiWrapper.getInstance(context));
    }

    @VisibleForTesting
    protected PublicApi(PublicApiWrapper wrapper) {
        mApiWrapper = wrapper;
    }

    public HttpResponse head(Request resource) throws IOException {
        return mApiWrapper.head(resource);
    }

    public HttpResponse get(Request resource) throws IOException {
        return mApiWrapper.get(resource);
    }

    public Token clientCredentials(String... scopes) throws IOException {
        return mApiWrapper.clientCredentials(scopes);
    }

    public Token extensionGrantType(String grantType, String... scopes) throws IOException {
        return mApiWrapper.extensionGrantType(grantType, scopes);
    }

    public Token login(String username, String password, String... scopes) throws IOException {
        return mApiWrapper.login(username, password, scopes);
    }

    public URI authorizationCodeUrl(String... options) {
        return mApiWrapper.authorizationCodeUrl(options);
    }

    public HttpResponse put(Request request) throws IOException {
        return mApiWrapper.put(request);
    }

    public HttpResponse post(Request request) throws IOException {
        return mApiWrapper.post(request);
    }

    public HttpResponse delete(Request request) throws IOException {
        return mApiWrapper.delete(request);
    }

    public Token refreshToken() throws IOException {
        return mApiWrapper.refreshToken();
    }

    public Token getToken() {
        return mApiWrapper.getToken();
    }

    public long resolve(String uri) throws IOException {
        return mApiWrapper.resolve(uri);
    }

    public void setToken(Token token) {
        mApiWrapper.setToken(token);
    }

    public void setTokenListener(TokenListener listener) {
        mApiWrapper.setTokenListener(listener);
    }

    public Token invalidateToken() {
        return mApiWrapper.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return mApiWrapper.getMapper();
    }

    public <T extends ScResource> T read(Request req) throws IOException {
        return mApiWrapper.read(req);
    }

    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        return mApiWrapper.update(request);
    }

    public <T extends ScResource> T create(Request request) throws IOException {
        return mApiWrapper.create(request);
    }

    public <T extends ScResource> List<T> readList(Request req) throws IOException {
        return mApiWrapper.readList(req);
    }

    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        return mApiWrapper.readCollection(req);
    }

    @NotNull
    public <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException {
        return mApiWrapper.readFullCollection(request, ch);
    }


    public <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        return mApiWrapper.readListFromIds(request, ids);
    }

    public Token authorizationCode(String code, String... scopes) throws IOException {
        return mApiWrapper.authorizationCode(code, scopes);
    }

    public void setDefaultContentType(String contentType) {
        mApiWrapper.setDefaultContentType(contentType);
    }

    public void setDefaultAcceptEncoding(String encoding) {
        mApiWrapper.setDefaultAcceptEncoding(encoding);
    }

    public HttpClient getHttpClient() {
        return mApiWrapper.getHttpClient();
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        return mApiWrapper.safeExecute(target, request);
    }

    public Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException {
        return mApiWrapper.resolveStreamUrl(uri, skipLogging);
    }

    @Override
    public String getUserAgent() {
        return mApiWrapper.getUserAgent();
    }

    @Override
    public Env getEnv() {
        return mApiWrapper.getEnv();
    }
}
