package com.soundcloud.android.api;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.utils.ApiWrapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.urbanstew.soundcloudapi.AuthorizationURLOpener;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CloudAPITests implements CloudAPI.Params {
    // testor app
    static String CONSUMER_KEY = "n3X11THP8cBvoXFE1TVxQ";
    static String CONSUMER_SECRET = "nEIW6nGA4KXoTtNbYYHw4fIaHRUxGWDrF1SH0Q81I";

    // jberkel_testing account on soundcloud.com
    private static final String TOKEN = "ikkHgVoKBdvPNRgxdQmDw";
    private static final String SECRET = "6CsbFGrW5I3eCu5pAwXI4if7qPSjBMqT9MS6IowjsY";

    CloudAPI api;

    @Before
    public void setUp() throws Exception {
        api = new ApiWrapper(
                CONSUMER_KEY,
                CONSUMER_SECRET,
                TOKEN,
                SECRET,
                true);
    }


    @Test
    @Ignore
    public void testConnections() throws IOException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(
                        api.executeRequest(CloudAPI.Enddpoints.CONNECTIONS)
                ));

        String l;
        while ((l = r.readLine()) != null) {
            System.err.println(l);
        }
    }

    @Test
    @Ignore
    public void testUpload() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TITLE, "Hello Android"));
        params.add(new BasicNameValuePair(POST_TO, "486436"));

        ContentBody track = new FileBody(
                new File(getClass().getResource("hello.aiff").getFile()));

        HttpResponse resp = api.upload(track, null, params, null);
        int status = resp.getStatusLine().getStatusCode();

        assertEquals(201, status);
    }

    @Test @Ignore
    public void testUploadOverridingSharing() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TITLE, "Hello Android"));
        params.add(new BasicNameValuePair(POST_TO_EMPTY, ""));

        ContentBody track = new FileBody(
                new File(getClass().getResource("hello.aiff").getFile()));

        HttpResponse resp = api.upload(track, null, params, null);
        int status = resp.getStatusLine().getStatusCode();

        assertEquals(201, status);
    }

    public static void main(String[] args) throws Exception {
        SoundCloudAPI api = new SoundCloudAPI(
                CONSUMER_KEY,
                CONSUMER_SECRET,
                SoundCloudAPI.USE_PRODUCTION);

        api.authorizeUsingUrl(
                "http://localhost:8088/",
                "Thank you for authorizing",
                new AuthorizationURLOpener() {
                    @Override
                    public void openAuthorizationURL(String verificationURL) {
                        System.err.println("Please visit " + verificationURL);
                    }
                });

        System.err.println("token: " + api.getToken());
        System.err.println("secret: " + api.getTokenSecret());
    }
}
