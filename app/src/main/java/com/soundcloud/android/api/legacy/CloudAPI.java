package com.soundcloud.android.api.legacy;

import com.soundcloud.android.api.oauth.Token;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

/**
 * Interface with SoundCloud, using OAuth2.
 * <p/>
 * This is the interface, for the implementation see ApiWrapper.
 *
 * @see ApiWrapper
 */
public interface CloudAPI {

    // other constants
    String REALM = "SoundCloud";
    String OAUTH_SCHEME = "oauth";
    String VERSION = "1.3.1";
    String USER_AGENT = "SoundCloud Java Wrapper (" + VERSION + ")";

    /**
     * Request a token using <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.1.2">
     * Resource Owner Password Credentials</a>.
     *
     * @param username SoundCloud username
     * @param password SoundCloud password
     * @return a valid token
     * @throws InvalidTokenException invalid token
     * @throws IOException                                       In case of network/server errors
     */
    Token login(String username, String password) throws IOException;


    /**
     * Request a "signup" token using <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-15#section-4.4">
     * Client Credentials</a>.
     * <p/>
     * Note that this token is <b>not</b> set as the current token in the wrapper - it should only be used
     * for one request (typically the signup / user creation request).
     * Also note that not all apps are allowed to request this token type (the wrapper throws
     * InvalidTokenException in this case).
     *
     * @param scopes the desired scope(s), or empty for default scope
     * @return a valid token
     * @throws IOException                                       IO/Error
     * @throws InvalidTokenException if requested scope is not available
     */
    Token clientCredentials(String... scopes) throws IOException;


    /**
     * Request a token using an <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-4.5">
     * extension grant type</a>.
     *
     * @param grantType
     * @return
     * @throws IOException
     */
    Token extensionGrantType(String grantType) throws IOException;

    /**
     * Tries to refresh the currently used access token with the refresh token.
     * If successful the API wrapper will have the new token set already.
     *
     * @return a valid token
     * @throws IOException                                       in case of network problems
     * @throws InvalidTokenException invalid token
     * @throws IllegalStateException                             if no refresh token present
     */
    Token refreshToken() throws IOException;

    /**
     * This method should be called when the token was found to be invalid.
     * Also replaces the current token, if there is one available.
     *
     * @return an alternative token, or null if none available
     * (which indicates that a refresh could be tried)
     */
    Token invalidateToken();

    /**
     * @param request resource to GET
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse get(Request request) throws IOException;

    /**
     * @param request resource to POST
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse post(Request request) throws IOException;

    /**
     * @param request resource to PUT
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse put(Request request) throws IOException;

    /**
     * @param request resource to DELETE
     * @return the HTTP response
     * @throws IOException IO/Error
     */
    HttpResponse delete(Request request) throws IOException;

    /**
     * @return the used httpclient
     */
    HttpClient getHttpClient();

    /**
     * Generic execute method, with added workarounds for various HTTPClient bugs.
     *
     * @param target  the target host (can be null)
     * @param request the request
     * @return the HTTP response
     * @throws IOException               network errors
     * @throws BrokenHttpClientException in case of HTTPClient framework bugs
     */
    HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException;

    /**
     * Resolve the given SoundCloud URI
     *
     * @param uri SoundCloud model URI, e.g. http://soundcloud.com/bob
     * @return the id
     * @throws IOException       network errors
     * @throws ResolverException if object could not be resolved
     */
    long resolve(String uri) throws IOException;

    /**
     * @return the current token
     */
    Token getToken();

}
