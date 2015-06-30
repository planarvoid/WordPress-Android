package com.soundcloud.api;

import org.apache.http.HttpResponse;

/**
 * Thrown if resolving the audio stream of a SoundCloud sound fails.
 */
public class ResolverException extends ApiResponseException {

    public ResolverException(String s, HttpResponse resp) {
        super(resp, s);
    }

    public ResolverException(Throwable throwable, HttpResponse response) {
        super(throwable, response);
    }
}
