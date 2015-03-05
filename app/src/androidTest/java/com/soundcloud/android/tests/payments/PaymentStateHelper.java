package com.soundcloud.android.tests.payments;

import com.soundcloud.android.utils.IOUtils;

import android.os.Environment;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class PaymentStateHelper {

    private static final String PROXY_ADDRESS = "mobileci-proxy.int.s-cloud.net";
    private static final int PROXY_PORT = 3128;
    private static final int TEST_USER_ID = 122411702;
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private static final String KEY_FILE = "payment_test_secrets.txt";

    private String proxyPassword;
    private String accessToken;

    public PaymentStateHelper() {
        loadSecretsFromDevice();
    }

    private void loadSecretsFromDevice() {
        final File keyFile = new File(Environment.getExternalStorageDirectory(), KEY_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(keyFile));
            proxyPassword = reader.readLine();
            accessToken = reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reset test user subscription state: error reading keys");
        } finally {
            IOUtils.close(reader);
        }
    }

    public void resetTestAccount() {
        try {
            buildDeleteSubscriptionConnection().getResponseCode();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reset test user subscription state: connection error");
        }
    }

    private HttpURLConnection buildDeleteSubscriptionConnection() throws IOException {
        URL url = new URL(buildDeleteSubcriptionUrl());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDRESS, PROXY_PORT));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("X-Access-Token", accessToken);
        connection.setRequestProperty("Proxy-Authorization", formatProxyAuth());
        connection.setConnectTimeout((int) TIMEOUT);
        connection.setReadTimeout((int) TIMEOUT);
        return connection;
    }

    private String formatProxyAuth() {
        final String login = "ciuser" + ":" + proxyPassword;
        return "Basic " + Base64.encodeToString(login.getBytes(), Base64.DEFAULT);
    }

    private String buildDeleteSubcriptionUrl() {
        final String path = "http://buckster-test.int.s-cloud.net/api/users/%s/consumer_subscriptions/active";
        return String.format(Locale.US, path, String.valueOf(TEST_USER_ID));
    }

}
