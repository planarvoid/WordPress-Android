package com.soundcloud.android;

import com.soundcloud.utils.http.ProgressListener;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CloudAPI {
    InputStream executeRequest(String req) throws IOException;

    InputStream getContent(String path) throws IOException;

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
}
