package com.soundcloud.android.utils.play;


import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.utils.Http;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * orig: http://code.google.com/p/npr-android-app/source/browse/trunk/Npr/src/org/npr/android/news/StreamProxy.java
 */
public class StreamProxy implements Runnable {
    private static final String LOG_TAG = StreamProxy.class.getSimpleName();

    private int mPort = 0;
    private boolean mIsRunning = true;
    private ServerSocket mSocket;
    private Thread mThread;
    private AndroidCloudAPI mApi;


    private Map<String, ResolvedUrl> resolverCache = new HashMap<String, ResolvedUrl>();
    public static final long DEFAULT_URL_LIFETIME = 60 * 1000; // expire after 1 minute

    public StreamProxy(AndroidCloudAPI api) {
        this.mApi = api;
    }

    public void init() {
        try {
            mSocket = new ServerSocket(mPort, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            mSocket.setSoTimeout(5000);
            mPort = mSocket.getLocalPort();
            Log.d(LOG_TAG, "port " + mPort + " obtained");
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, "Error initializing server", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error initializing server", e);
        }
    }

    public void start() {
        if (mSocket == null) {
            throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
        }
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public void stop() {
        mIsRunning = false;
        if (mThread == null) {
            throw new IllegalStateException("Cannot stop proxy; it has not been started.");
        }

        mThread.interrupt();
        try {
            mThread.join(5000);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        Log.d(LOG_TAG, "running");
        while (mIsRunning) {
            try {
                final Socket client = mSocket.accept();
                Log.d(LOG_TAG, "client connected");
                try {
                    final HttpUriRequest request = readRequest(client);
                    new Thread("handle-proxy-request") {
                        @Override
                        public void run() {
                            processRequest(request, client);
                        }
                    }.start();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "error reading request");
                    client.close();
                }
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to client", e);

            }
        }
        Log.d(LOG_TAG, "Proxy interrupted. Shutting down.");
    }

    private HttpUriRequest readRequest(Socket client) throws IOException {
        InputStream is = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
        String line = reader.readLine();

        if (line == null) {
            throw new IOException("Proxy client closed connection without a request.");
        }

        StringTokenizer st = new StringTokenizer(line);
        String method = st.nextToken();

        if (!"GET".equalsIgnoreCase(method)) {
            throw new IOException("only GET is supported, got: " + method);
        }
        String uri = st.nextToken();
        String realUri = uri.substring(1);

        final HttpUriRequest request = new HttpGet(resolve(realUri));
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) break;
            Header h = BasicLineParser.parseHeader(line, BasicLineParser.DEFAULT);

            if (!h.getName().equalsIgnoreCase("host")) {
                request.addHeader(h);
            }
        }
        return request;
    }


    public int getPort() {
        return mPort;
    }

    private void processRequest(HttpUriRequest request, Socket client) {
        if (request == null) {
            return;
        }
        InputStream data = null;
        HttpClient httpClient = null;
        try {
            httpClient = Http.createHttpClient(AndroidCloudAPI.USER_AGENT);
            HttpResponse realResponse = httpClient.execute(request);
            if (realResponse == null) {
                return;
            }

            data = realResponse.getEntity().getContent();
            StatusLine line = realResponse.getStatusLine();
            HttpResponse response = new BasicHttpResponse(line);
            response.setHeaders(realResponse.getAllHeaders());

            StringBuilder httpString = new StringBuilder();
            httpString.append(response.getStatusLine().toString());

            httpString.append("\n");
            for (Header h : response.getAllHeaders()) {
                httpString.append(h.getName()).append(": ").append(h.getValue()).append(
                        "\n");
            }
            httpString.append("\n");
            byte[] buffer = httpString.toString().getBytes();
            int readBytes;
            Log.d(LOG_TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // Start streaming content.
            byte[] buff = new byte[1024 * 50];
            while (mIsRunning && (readBytes = data.read(buff, 0, buff.length)) != -1) {
                client.getOutputStream().write(buff, 0, readBytes);
            }
        } catch (SocketException e) {
            if ("Connection reset by peer".equals(e.getMessage())) {
                Log.d(LOG_TAG, "client closed connection [expected]");
            } else {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        } finally {
            if (data != null) {
                try {
                    data.close();
                } catch (IOException ignored) {
                }
            }
            try {
                client.close();
            } catch (IOException ignored) {
            }
            Http.close(httpClient);
        }
    }


    private synchronized String resolve(String streamUrl) throws IOException {
        ResolvedUrl resolved = resolverCache.get(streamUrl);
        if (resolved == null || resolved.isExpired()) {
            Log.d(LOG_TAG, "resolving url:" + streamUrl);
            resolved = new ResolvedUrl(doResolve(streamUrl));
            resolverCache.put(streamUrl, resolved);
        } else {
            Log.d(LOG_TAG, "using cached url:" + resolved);
        }
        return resolved.url;
    }


    private String doResolve(String streamUrl) throws IOException {
        HttpResponse resp = mApi.get(Request.to(streamUrl));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            final Header location = resp.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                return location.getValue();
            } else {
                throw new IOException("no location header");
            }
        } else {
            throw new IOException("unexpected status code:" + resp.getStatusLine());
        }

    }

    static class ResolvedUrl {
        ResolvedUrl(String url) {
            this.url = url;
            String exp = Uri.parse(url).getQueryParameter("Expires");
            if (exp != null) {
                try {
                    this.expires = Long.parseLong(exp) * 1000L;
                    Log.d(LOG_TAG, String.format("url expires in %d secs", (this.expires-System.currentTimeMillis())/1000L));
                } catch (NumberFormatException e) {
                    this.expires = System.currentTimeMillis() + DEFAULT_URL_LIFETIME;
                }
            } else {
                this.expires = System.currentTimeMillis() + DEFAULT_URL_LIFETIME;
            }
        }

        long expires;
        final String url;

        public boolean isExpired() {
            return System.currentTimeMillis() > expires;
        }

        @Override
        public String toString() {
            return "ResolvedUrl{" +
                    "expires=" + expires +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
