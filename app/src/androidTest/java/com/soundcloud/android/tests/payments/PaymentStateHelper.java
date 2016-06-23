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


    private PaymentStateHelper() {
    }

    public static void resetTestAccount() {
        try {
            final Secrets secrets = loadSecretsFromDevice();
            buildDeleteSubscriptionConnection(secrets).getResponseCode();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reset test user subscription state: connection error", e);
        }
    }

    private static Secrets loadSecretsFromDevice() {
        final File keyFile = new File(Environment.getExternalStorageDirectory(), KEY_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(keyFile));
            return new Secrets(reader.readLine(), reader.readLine());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reset test user subscription state: error reading keys", e);
        } finally {
            IOUtils.close(reader);
        }
    }

    private static HttpURLConnection buildDeleteSubscriptionConnection(Secrets secrets) throws IOException {
        URL url = new URL(buildDeleteSubcriptionUrl());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDRESS, PROXY_PORT));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("X-Access-Token", secrets.accessToken);
        connection.setRequestProperty("Proxy-Authorization", formatProxyAuth(secrets.proxyPassword));
        connection.setConnectTimeout((int) TIMEOUT);
        connection.setReadTimeout((int) TIMEOUT);
        return connection;
    }

    private static String formatProxyAuth(String proxyPassword) {
        final String login = "ciuser" + ":" + proxyPassword;
        return "Basic " + Base64.encodeToString(login.getBytes(), Base64.DEFAULT);
    }

    private static String buildDeleteSubcriptionUrl() {
        final String path = "http://buckster-test.int.s-cloud.net/api/users/%s/consumer_subscriptions/active";
        return String.format(Locale.US, path, String.valueOf(TEST_USER_ID));
    }

    private static class Secrets {
        public final String proxyPassword;
        public final String accessToken;

        public Secrets(String proxyPassword, String accessToken) {
            this.proxyPassword = proxyPassword;
            this.accessToken = accessToken;
        }
    }

}
