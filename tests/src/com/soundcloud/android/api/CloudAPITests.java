package com.soundcloud.android.api;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.utils.ApiWrapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.urbanstew.soundcloudapi.AuthorizationURLOpener;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CloudAPITests {
    static String CONSUMER_KEY = "n3X11THP8cBvoXFE1TVxQ";
    static String CONSUMER_SECRET = "nEIW6nGA4KXoTtNbYYHw4fIaHRUxGWDrF1SH0Q81I";

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

        Logger.getLogger("httpclient.wire.header").setLevel(Level.FINEST);
        Logger.getLogger("httpclient.wire.content").setLevel(Level.FINEST);
    }



    @Test
    public void testUpload() throws Exception {
        File file = File.createTempFile("upload", ".ogg");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("track[title]", "title"));

        ContentBody track = new FileBody(file);

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

        System.err.println("token: " +  api.getToken());
        System.err.println("secret: " + api.getTokenSecret());
    }
}
