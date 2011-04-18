package com.soundcloud.api;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CloudAPIIntegrationTests implements CloudAPI.TrackParams, CloudAPI.Enddpoints {
    // http://sandbox-soundcloud.com/you/apps/java-api-wrapper-test-app
    static String CLIENT_ID     = "yH1Jv2C5fhIbZfGTpKtujQ";
    static String CLIENT_SECRET = "C6o8jc517b6PIw0RKtcfQsbOK3BjGpxWFLg977UiguY";

    CloudAPI api;

    /*
    To enable full HTTP logging, add the following system properties:

    -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
    -Dorg.apache.commons.logging.simplelog.showdatetime=true
    -Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG
    -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=ERROR
    */

    @Before
    public void setUp() throws Exception {
        api = new ApiWrapper(
                CLIENT_ID,
                CLIENT_SECRET,
                null,
                CloudAPI.Env.SANDBOX);

        api.login("api-testing", "testing");
    }

    @Test
    public void shouldUploadASimpleAudioFile() throws Exception {
        Http.Params params = new Http.Params(
                TITLE,         "Hello Android",
                POST_TO_EMPTY, ""
        ).addFile(ASSET_DATA, new File(getClass().getResource("hello.aiff").getFile()));
        HttpResponse resp = api.postContent(TRACKS, params);
        int status = resp.getStatusLine().getStatusCode();
        assertThat(status, is(201));
    }

    @Test
    public void shouldReturn401WithInvalidToken() throws Exception {
        api.setToken(new Token("invalid", "invalid"));
        HttpResponse resp = api.getContent(CloudAPI.Enddpoints.MY_DETAILS);
        assertThat(resp.getStatusLine().getStatusCode(), is(401));
    }

    @Test
    public void shouldRefreshAutomaticallyWhenTokenExpired() throws Exception {
        HttpResponse resp = api.getContent(CloudAPI.Enddpoints.MY_DETAILS);
        assertThat(resp.getStatusLine().getStatusCode(), is(200));

        final Token oldToken = api.getToken();
        api.invalidateToken();

        resp = api.getContent(CloudAPI.Enddpoints.MY_DETAILS);
        assertThat(resp.getStatusLine().getStatusCode(), is(200));
        // make sure we've got a new token
        assertThat(oldToken, not(equalTo(api.getToken())));
    }

    @Test
    public void shouldResolveUrls() throws Exception {
        long id = api.resolve("http://sandbox-soundcloud.com/api-testing");
        assertThat(id, not(-1L));
        assertThat(id, is(1862213L));
    }

    @Test
    public void readMe() throws Exception {
        HttpResponse resp = api.getContent(CloudAPI.Enddpoints.MY_DETAILS);
        assertThat(resp.getStatusLine().getStatusCode(), is(200));
        // writeResponse(resp, "me.json");
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void writeResponse(HttpResponse resp, String file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        InputStream is = resp.getEntity().getContent();
        byte[] b = new byte[8192];
        int n;

        while ((n = is.read(b)) >= 0) fos.write(b, 0, n);
        is.close();
        fos.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new RuntimeException("CloudAPITests <username> <password>");
        }

        Token token = new ApiWrapper(
                CLIENT_ID,
                CLIENT_SECRET,
                null,
                CloudAPI.Env.SANDBOX).login(args[0], args[1]);

        System.err.println("token: " + token);
    }
}
