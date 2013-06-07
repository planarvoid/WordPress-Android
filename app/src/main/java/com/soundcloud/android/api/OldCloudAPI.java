package com.soundcloud.android.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.api.http.Wrapper;
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

public class OldCloudAPI implements AndroidCloudAPI {

    private Wrapper mCloudApi;
    private Context mContext;

    public OldCloudAPI(Context context){
        this(context, Wrapper.getInstance(context));
    }

    @VisibleForTesting
    protected OldCloudAPI(Context context, Wrapper wrapper) {
        mContext = context;
        mCloudApi = wrapper;
    }

    public HttpResponse head(Request resource) throws IOException {
        return mCloudApi.head(resource);
    }

    public HttpResponse get(Request resource) throws IOException {
        return mCloudApi.get(resource);
    }

    public Token clientCredentials(String... scopes) throws IOException {
        return mCloudApi.clientCredentials(scopes);
    }

    public Token extensionGrantType(String grantType, String... scopes) throws IOException {
        return mCloudApi.extensionGrantType(grantType, scopes);
    }

    public Token login(String username, String password, String... scopes) throws IOException {
        return mCloudApi.login(username, password, scopes);
    }

    public URI authorizationCodeUrl(String... options) {
        return mCloudApi.authorizationCodeUrl(options);
    }

    public HttpResponse put(Request request) throws IOException {
        return mCloudApi.put(request);
    }

    public HttpResponse post(Request request) throws IOException {
        return mCloudApi.post(request);
    }

    public HttpResponse delete(Request request) throws IOException {
        return mCloudApi.delete(request);
    }

    public Token refreshToken() throws IOException {
        return mCloudApi.refreshToken();
    }

    public Token getToken() {
        return mCloudApi.getToken();
    }

    public long resolve(String uri) throws IOException {
        return mCloudApi.resolve(uri);
    }

    public void setToken(Token token) {
        mCloudApi.setToken(token);
    }

    public void setTokenListener(TokenListener listener) {
        mCloudApi.setTokenListener(listener);
    }

    public Token exchangeOAuth1Token(String oauth1AccessToken) throws IOException {
        return mCloudApi.exchangeOAuth1Token(oauth1AccessToken);
    }

    public Token invalidateToken() {
        return mCloudApi.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    public <T extends ScResource> T read(Request req) throws IOException {
        return mCloudApi.read(req);
    }

    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        return mCloudApi.update(request);
    }

    public <T extends ScResource> T create(Request request) throws IOException {
        return mCloudApi.create(request);
    }

    public <T extends ScResource> List<T> readList(Request req) throws IOException {
        return mCloudApi.readList(req);
    }

    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        return mCloudApi.readCollection(req);
    }

    @NotNull
    public <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException {
        return mCloudApi.readFullCollection(request, ch);
    }


    public <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        return mCloudApi.readListFromIds(request, ids);
    }

    public Token authorizationCode(String code, String... scopes) throws IOException {
        return mCloudApi.authorizationCode(code, scopes);
    }

    public void setDefaultContentType(String contentType) {
        mCloudApi.setDefaultContentType(contentType);
    }

    public void setDefaultAcceptEncoding(String encoding) {
        mCloudApi.setDefaultAcceptEncoding(encoding);
    }

    public HttpClient getHttpClient() {
        return mCloudApi.getHttpClient();
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        return mCloudApi.safeExecute(target, request);
    }

    public Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException {
        return mCloudApi.resolveStreamUrl(uri, skipLogging);
    }

    @Override
    public String getUserAgent() {
        return mCloudApi.getUserAgent();
    }

    @Override
    public Env getEnv() {
        return mCloudApi.getEnv();
    }
}
