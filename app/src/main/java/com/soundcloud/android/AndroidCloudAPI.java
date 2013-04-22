package com.soundcloud.android;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;

import android.content.Context;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface AndroidCloudAPI extends CloudAPI {
    String TAG = AndroidCloudAPI.class.getSimpleName();
    URI REDIRECT_URI = URI.create("soundcloud://auth");

    String getUserAgent();
    Env getEnv();
    ObjectMapper getMapper();
    Context getContext();

    /**
     * Reads a single resource from the API.
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     * @throws NotFoundException if the resource could not be found (404)
     */
    <T extends ScResource> T read(Request request) throws NotFoundException, IOException;

    /**
     * PUTs a resource to the API and returns the parsed representation.
     *
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     * @throws NotFoundException if the resource could not be found (404)
     */
    <T extends ScResource> T update(Request request) throws NotFoundException, IOException;

    /**
     * POSTs a resource to the API and returns the parsed representation.
     *
     * @param request the request to make
     * @return the parsed resource
     * @throws IOException
     */
    <T extends ScResource> T create(Request request) throws IOException;

    /**
     * Parses a list of resources from the API. This can be transparently be an array of objects, or
     * a cursor object (see {@link #readCollection(Request)}).
     *
     * @param request the request make
     * @return a list of resources
     * @throws IOException parsing errors
     */
    <T extends ScResource> List<T> readList(Request request) throws IOException;

    /**
     * Reads a cursor object (for the <em>linked_partioning</em> scheme.
     * @param request the request to make
     * @return a resource holder containing resources
     * @throws IOException
     */
    <T extends ScResource> CollectionHolder<T> readCollection(Request request) throws IOException;

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
    <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException;


    class NotFoundException extends IOException {
        public NotFoundException() {
            super();
        }
    }
}
