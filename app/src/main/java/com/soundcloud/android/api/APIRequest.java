package com.soundcloud.android.api;


import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

/**
 * Represent a request that can be made with a {@link RxHttpClient}. Mandatory values are
 * the target API (public/private), uri path & method. If the request targets the private API
 * then one must also provide a version. A resource type is not mandatory however any response will be ignored if
 * one is not provided.
 * @param <ResourceType>
 */
public interface APIRequest<ResourceType> {
    /**
     * Should return the path (minus host) that the request should be made to. Query parameters not currently supported
     * @return The path the request will be made to
     */
    String getEncodedPath();

    /**
     * Should return the resource type that the response (if any) should be unmarshaled into.
     * Can be either single model or a collection of models. See {@link TypeToken}.
     * @return TypeToken representing the type to unmarshal the response into
     */
    TypeToken<ResourceType> getResourceType();

    /**
     * The Http method that should be used when making the request
     * @return String of the Http method to use
     */
    String getMethod();

    /**
     * The version number of the response format for a given endpoint. This should only be used if
     * the request is to be made to the private API
     * @return An integer >0 that the response format should be for the given URI endpoint
     */
    int getVersion();

    /**
     * Indicates if this request is to be made to the private API
     * @return True if the request is to be made to the private API, false otherwise
     */
    boolean isPrivate();

    /**
     * Returns the query parameters to be sent with the request.
     * @return Multimap containing the key value pairs representing the query parameters.
     */
    Multimap<String, String> getQueryParameters();

    /**
     * Returns content object that is to be serialized to json and sent along with the request
     * @return Object representing the content to b serialized
     */
    Object getContent();
}
