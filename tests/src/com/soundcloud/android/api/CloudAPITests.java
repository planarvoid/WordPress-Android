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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CloudAPITests implements CloudAPI.Params, CloudAPI.Enddpoints {
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
    public void shouldUploadASimpleAudioFile() throws Exception {
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
    public void shouldOverrideConnectionSettings() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TITLE, "Hello Android"));
        params.add(new BasicNameValuePair(POST_TO_EMPTY, ""));

        ContentBody track = new FileBody(
                new File(getClass().getResource("hello.aiff").getFile()));

        HttpResponse resp = api.upload(track, null, params, null);
        int status = resp.getStatusLine().getStatusCode();

        assertEquals(201, status);
    }


    @Test @Ignore
    public void readTracks() throws Exception {
        int id = api.resolve("http://soundcloud.com/soundcloud-android-mwc");
        if (id == -1) throw new IllegalArgumentException("unknown id");

        HttpResponse resp = api.getContent("users/"+id+"/tracks");

        if (resp.getStatusLine().getStatusCode() == 200) {
            FileOutputStream fos = new FileOutputStream("out.json");
            InputStream is = resp.getEntity().getContent();
            byte[] b = new byte[8192];
            int n;

            while ((n = is.read(b)) >= 0) fos.write(b, 0, n);
            is.close();
            fos.close();
        } else {
            throw new RuntimeException(resp.getStatusLine().toString());
        }
    }

    @Test @Ignore
     public void readMe() throws Exception {
         HttpResponse resp = api.getContent("me");

         if (resp.getStatusLine().getStatusCode() == 200) {
             FileOutputStream fos = new FileOutputStream("out.json");
             InputStream is = resp.getEntity().getContent();
             byte[] b = new byte[8192];
             int n;

             while ((n = is.read(b)) >= 0) fos.write(b, 0, n);
             is.close();
             fos.close();
         } else {
             throw new RuntimeException(resp.getStatusLine().toString());
         }
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
