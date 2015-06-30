package com.soundcloud.android.api.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public interface PublicCloudAPI extends CloudAPI {
    String TAG = PublicCloudAPI.class.getSimpleName();

    String getUserAgent();
    Env getEnv();
    ObjectMapper getMapper();

    /**
     * Reads a single resource from the API.
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     * @throws NotFoundException if the resource could not be found (404)
     */
    <T extends PublicApiResource> T read(Request request) throws NotFoundException, IOException;

    /**
     * PUTs a resource to the API and returns the parsed representation.
     *
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     * @throws NotFoundException if the resource could not be found (404)
     */
    <T extends PublicApiResource> T update(Request request) throws NotFoundException, IOException;

    /**
     * POSTs a resource to the API and returns the parsed representation.
     *
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     */
    <T extends PublicApiResource> T create(Request request) throws IOException;

    /**
     * Parses a list of resources from the API. This can be transparently be an array of objects, or
     * a cursor object (see {@link #readCollection(Request)}).
     *
     * @param request the request make
     * @return a list of resources
     * @throws IOException parsing errors
     */
    <T extends PublicApiResource> List<T> readList(Request request) throws IOException;

    /**
     * Reads a cursor object (for the <em>linked_partioning</em> scheme.
     * @param request the request to make
     * @return a resource holder containing resources
     * @throws IOException
     */
    <T extends PublicApiResource> CollectionHolder<T> readCollection(Request request) throws IOException;

    /**
     * Fetches all resources for the given request until the cursor has been completely read.
     *
     * @param request the initial request
     * @param ch a collection holder to be used for deserializing
     * @throws IOException
     */
    @NotNull <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException;


    /**
     * Performs a batch lookup of a list of ids
     * @param request the initial request
     * @param ids a list of ids
     * @return a list of resources
     * @throws IOException
     */
    <T extends PublicApiResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException;


}
