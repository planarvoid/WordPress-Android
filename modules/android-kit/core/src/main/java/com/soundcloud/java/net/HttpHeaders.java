/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.java.net;

/**
 * Contains constant definitions for the HTTP header field names. See:
 * <ul>
 * <li><a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2183.txt">RFC 2183</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2965.txt">RFC 2965</a>
 * <li><a href="http://www.ietf.org/rfc/rfc5988.txt">RFC 5988</a>
 * </ul>
 *
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
public final class HttpHeaders {
    // HTTP Request and Response header fields

    /**
     * The HTTP {@code Cache-Control} header field name.
     */
    public static final String CACHE_CONTROL = "Cache-Control";
    /**
     * The HTTP {@code Content-Length} header field name.
     */
    public static final String CONTENT_LENGTH = "Content-Length";
    /**
     * The HTTP {@code Content-Type} header field name.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    // HTTP Request header fields

    /**
     * The HTTP {@code Accept} header field name.
     */
    public static final String ACCEPT = "Accept";
    /**
     * The HTTP {@code Accept-Charset} header field name.
     */
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    /**
     * The HTTP {@code Accept-Encoding} header field name.
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    /**
     * The HTTP {@code Accept-Language} header field name.
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    /**
     * The HTTP {@code Authorization} header field name.
     */
    public static final String AUTHORIZATION = "Authorization";
    /**
     * The HTTP {@code Cookie} header field name.
     */
    public static final String COOKIE = "Cookie";
    /**
     * The HTTP {@code User-Agent} header field name.
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * The HTTP {@code ETag} header field name.
     */
    public static final String ETAG = "ETag";
    /**
     * The HTTP {@code Expires} header field name.
     */
    public static final String EXPIRES = "Expires";
    /**
     * The HTTP {@code Last-Modified} header field name.
     */
    public static final String LAST_MODIFIED = "Last-Modified";
    /**
     * The HTTP {@code Location} header field name.
     */
    public static final String LOCATION = "Location";
    /**
     * The HTTP {@code X-User-IP} header field name.
     */
    public static final String X_USER_IP = "X-User-IP";

    private HttpHeaders() {
        // no instances
    }
}
