package com.soundcloud.android;

import com.soundcloud.utils.SoundCloudAuthorizationClient;
import com.soundcloud.utils.http.ProgressListener;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CloudAPI {
    // XXX simplify+consolidate methods


    ObjectMapper getMapper();

    InputStream executeRequest(String req) throws IOException;

    InputStream getContent(String path) throws IOException;

    HttpUriRequest getPreparedRequest(String path);
    HttpUriRequest getRequest(String path, List<NameValuePair> params);

    String getSignedUrl(String path);

    String signStreamUrlNaked(String path);

    InputStream putContent(String path) throws IOException;

    InputStream deleteContent(String path) throws IOException;

    HttpResponse upload(ContentBody trackBody,
                        ContentBody artworkBody,
                        List<NameValuePair> params,
                        ProgressListener listener)
                    throws IOException;

    void unauthorize();

    void authorizeWithoutCallback(Client client);

    SoundCloudAPI.State getState();

    InputStream executeRequest(HttpUriRequest req) throws IOException;

    static interface Client extends SoundCloudAuthorizationClient {
        void storeKeys(String token , String secret);
    }

}
