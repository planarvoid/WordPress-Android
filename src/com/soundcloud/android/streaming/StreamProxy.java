package com.soundcloud.android.streaming;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicLineParser;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * orig: http://code.google.com/p/npr-android-app/source/browse/trunk/Npr/src/org/npr/android/news/StreamProxy.java
 */
public class StreamProxy implements Runnable {
    private static final String LOG_TAG = StreamProxy.class.getSimpleName();

    private int mPort = 0;
    private boolean mIsRunning = true;
    private ServerSocketChannel mSocket;
    private Thread mThread;
    /* package */ final StreamLoader loader;
    /* package */ final StreamStorage storage;

    public StreamProxy(SoundCloudApplication app, int port) {
        storage = new StreamStorage(app, Consts.EXTERNAL_STREAM_DIRECTORY);
        loader = new StreamLoader(app, storage);
        mPort = port;
    }

    public StreamProxy init() throws IOException {
        mSocket = ServerSocketChannel.open();
        mSocket.socket().bind(new InetSocketAddress(mPort));
        mPort = mSocket.socket().getLocalPort();

        Log.d(LOG_TAG, "port " + mPort + " obtained");
        return this;
    }

    public StreamProxy start() {
        if (mSocket == null) {
            throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
        }
        mThread = new Thread(this);
        mThread.start();
        return this;
    }

    public void join() {
        if (mThread != null) try {
            mThread.join();
        } catch (InterruptedException ignored) {
        }
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
                final Socket client = mSocket.socket().accept();
                Log.d(LOG_TAG, "client connected");
                try {
                    final HttpGet request = readRequest(client);
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

    private HttpGet readRequest(Socket client) throws IOException {
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

        final HttpGet request = new HttpGet(realUri);
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) break;
            // copy original headers in new request
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

    private void processRequest(HttpGet request, Socket client) {
        final String streamUrl = request.getURI().toString();
        Log.d(LOG_TAG, "processRequest: " + streamUrl);

        long startByte = 0;

        Header range = request.getFirstHeader("Range");
        if (range != null && range.getValue() != null) {
            Matcher m = Pattern.compile("bytes=(\\d+)-").matcher(range.getValue());
            if (m.matches()) {
                startByte = Long.parseLong(m.group(1));
            }
        }
        Log.d(LOG_TAG, "startByte: " + startByte);

        try {
            SocketChannel channel = client.getChannel();

            final StreamItem item = new StreamItem(streamUrl);
            // write HTTP response header
            StringBuilder sb = new StringBuilder()
                    .append("HTTP/1.1 ")
                    .append(range == null ? "200 OK" : "206 OK").append("\r\n")
                    .append("Server: SoundCloudStreaming\r\n")
                    .append("Accept-Ranges: bytes\r\n")
                    .append("Content-Type: audio/mpeg\r\n")
                    .append("Connection: keep-alive\r\n");

            if (range != null) {
//                sb.append(String.format("Content-Range: %d-%d/%d", startByte, item.
            }

            sb.append("\r\n");

            channel.write(ByteBuffer.wrap(sb.toString().getBytes()));
            for (; ; ) {
                StreamFuture stream = loader.getDataForItem(item,
                        Range.from(startByte, storage.chunkSize));

                channel.write(stream.get());
                startByte += storage.chunkSize;

                if (startByte > item.getContentLength()) {
                    Log.d(LOG_TAG, "reached end of stream");
                    break;
                }
            }
        } catch (SocketException e) {
            if ("Connection reset by peer".equals(e.getMessage())) {
                Log.d(LOG_TAG, "client closed connection [expected]");
            } else {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, e);
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }
}
