package com.soundcloud.android.streaming;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicLineParser;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * orig: http://code.google.com/p/npr-android-app/source/browse/trunk/Npr/src/org/npr/android/news/StreamProxy.java
 */
public class StreamProxy implements Runnable {
    private static final String LOG_TAG = StreamLoader.LOG_TAG;

    private static final int INITIAL_TIMEOUT = 15;   // before receiving the first chunk
    private static final int TRANSFER_TIMEOUT = 120; // subsequent chunks
    private static final String CRLF = "\r\n";

    private static final String SERVER = "SoundCloudStreaming";

    private static final String PARAM_STREAM_URL = "streamUrl";
    private static final String PARAM_NEXT_STREAM_URL = "nextStreamUrl";
    private int mPort;

    private boolean mIsRunning = true;
    private ServerSocketChannel mSocket;
    private Thread mThread;

    public static String userAgent;

    /* package */ final StreamLoader loader;
    /* package */ final StreamStorage storage;

    public StreamProxy(SoundCloudApplication app) {
        this(app, 0);
    }

    public StreamProxy(SoundCloudApplication app, int port) {
        storage = new StreamStorage(app, Consts.EXTERNAL_STREAM_DIRECTORY);
        loader = new StreamLoader(app, storage);
        mPort = port;
    }

    public StreamProxy init() throws IOException {
        mSocket = ServerSocketChannel.open();
        mSocket.socket().bind(new InetSocketAddress(mPort));
        mPort = mSocket.socket().getLocalPort();

        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
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


    public boolean isRunning() {
        return mIsRunning;
    }

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

        loader.stop();
    }

    @Override
    public void run() {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "running");
        while (mIsRunning) {
            try {
                final Socket client = mSocket.socket().accept();
                client.setKeepAlive(true);
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

    public Uri createUri(String streamUrl, String nextStreamUrl) {
        final Uri.Builder builder = Uri.parse("http://127.0.0.1:" + getPort()).buildUpon();
        builder.appendPath("/");
        builder.appendQueryParameter(PARAM_STREAM_URL, streamUrl);
        if (nextStreamUrl != null) {
            builder.appendQueryParameter(PARAM_NEXT_STREAM_URL, nextStreamUrl);
        }
        return builder.build();
    }

    private long firstRequestedByte(HttpRequest request) {
        long startByte = 0;
        Header range = request.getFirstHeader("Range");
        if (range != null && range.getValue() != null) {
            Matcher m = Pattern.compile("bytes=(\\d+)-").matcher(range.getValue());
            if (m.matches()) {
                startByte = Long.parseLong(m.group(1));
            }
        }
        return startByte;
    }

    private void processRequest(HttpGet request, Socket client) {
        final Uri uri = Uri.parse(request.getURI().toString());
        final String streamUrl = uri.getQueryParameter(PARAM_STREAM_URL);
        final String nextUrl = uri.getQueryParameter(PARAM_NEXT_STREAM_URL);

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "processRequest: " + streamUrl);
        setUserAgent(request);

        try {
            if (streamUrl == null) throw new IOException("missing stream url parameter");

            final long startByte = firstRequestedByte(request);
            final SocketChannel channel = client.getChannel();
            Map<String, String> headers = headerMap();

            final File completeFile = storage.completeFileForUrl(streamUrl);
            if (completeFile.exists()) {
                streamCompleteFile(streamUrl, nextUrl, completeFile, startByte, channel, headers);
            } else {
                writeChunks(streamUrl, nextUrl, startByte, channel, headers);
            }
        } catch (SocketException e) {
            if ("Connection reset by peer".equals(e.getMessage()) ||
                    "Broken pipe".equals(e.getMessage())) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "client closed connection [expected]");
            } else {
                Log.w(LOG_TAG, e.getMessage(), e);
            }
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, e);
            storage.removeMetadata(streamUrl);
        } catch (IOException e) {
            Log.w(LOG_TAG, e);
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, e);
        } catch (TimeoutException e) {
            // let client reconnect to stream on timeout
            Log.d(LOG_TAG, "timeout getting data - closing connection", e);
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void queueNextUrl(String nextUrl, long delay) {
        if (nextUrl != null) {
            loader.preloadDataForUrl(nextUrl, delay);
        }
    }

    private ByteBuffer getHeader(long startByte, Map<String, String> headers) throws IOException {
        // write HTTP response header
        StringBuilder sb = new StringBuilder()
                .append("HTTP/1.1 ")
                .append(startByte == 0 ? "200 OK" : "206 OK").append(CRLF);

        for (Map.Entry<String, String> e : headers.entrySet()) {
            sb.append(e.getKey()).append(':').append(' ').append(e.getValue()).append(CRLF);
        }
        sb.append(CRLF);

        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, "header:" + sb);
        return ByteBuffer.wrap(sb.toString().getBytes());
    }

    private ByteBuffer getErrorHeader(int code, String message) {
        StringBuilder sb = new StringBuilder()
                .append("HTTP/1.1 ")
                .append(code).append(' ').append(message).append(CRLF);

        sb.append("Server: ").append(SERVER).append(CRLF)
                .append("Date: ").append(DateUtils.formatDate(new Date())).append(CRLF)
                .append(CRLF);

        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, "header:" + sb);
        return ByteBuffer.wrap(sb.toString().getBytes());
    }

    private void writeChunks(String streamUrl, String nextUrl, final long startByte, SocketChannel channel,
                             Map<String, String> headers)
            throws IOException, InterruptedException, TimeoutException {

        ByteBuffer buffer;
        for (long offset = startByte; ; ) {
            StreamFuture stream = loader.getDataForUrl(streamUrl, Range.from(offset, storage.chunkSize));
            try {
                if (offset == startByte) {
                    buffer = stream.get(INITIAL_TIMEOUT, TimeUnit.SECONDS);

                    // first chunk, write header
                    final long length = stream.item.getContentLength();
                    headers.put("Content-Length", String.valueOf(length));
                    headers.put("ETag", stream.item.etag());

                    if (startByte != 0) {
                        headers.put("Content-Range",
                                String.format("%d-%d/%d", startByte, length - 1, length));
                    }
                    channel.write(getHeader(startByte, headers));

                    // since we already got some data for this track, ready to queue next one
                    queueNextUrl(nextUrl, 3000);
                } else {
                    buffer = stream.get(TRANSFER_TIMEOUT, TimeUnit.SECONDS);
                }
            } catch (TimeoutException e) {
                // timeout happened before header write, take the chance to return a proper error code
                if (offset == startByte) {
                    channel.write(getErrorHeader(503, "Data read timeout"));
                }
                throw e;
            }

            if (stream.item.getContentLength() == 0) {
                // should not happen
                throw new IOException("BUG: content-length is 0");
            }

            offset += channel.write(buffer);
            if (offset >= stream.item.getContentLength()) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "reached end of stream");
                break;
            }
        }
    }

    /* package */ Map<String, String> headerMap() {
        Map<String, String> h = new LinkedHashMap<String, String>();
        h.put("Server", SERVER);
        h.put("Accept-Ranges", "bytes");
        h.put("Content-Type", "audio/mpeg");
        h.put("Connection", "close");
        h.put("X-SocketTimeout", "0");
        h.put("Date", DateUtils.formatDate(new Date()));
        return h;
    }

    private void streamCompleteFile(String streamUrl, String nextUrl,
                                    File file, long offset, SocketChannel channel, Map<String, String> headers) throws IOException {
        Log.d(LOG_TAG, "streaming complete file " + file);
        headers.put("Content-Length", String.valueOf(file.length() - offset));
        channel.write(getHeader(offset, headers));

        if (!loader.logPlaycount(streamUrl)) {
            Log.w(LOG_TAG, "could not queue playcount log");
        }

        queueNextUrl(nextUrl, 0);

        // touch file to prevent it from being collected by the cleaner
        if (!file.setLastModified(System.currentTimeMillis())) {
            Log.w(LOG_TAG, "could not touch file " + file);
        }

        FileChannel f = new FileInputStream(file).getChannel();
        f.position(offset);
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int read = 0;
        try {
            while (read < file.length() - offset) {
                int readnow = f.read(buffer);
                if (readnow == -1) {
                    break;
                } else {
                    read += readnow;
                    buffer.flip();
                    channel.write(buffer);
                    buffer.rewind();
                }
            }
        } finally {
            try {
                f.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void setUserAgent(HttpRequest r) {
        if (r.containsHeader("User-Agent")) {
            String agent = r.getFirstHeader("User-Agent").getValue();
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "userAgent:" + agent);
            userAgent = agent;
        }
    }

    public static boolean isOpenCore() {
        // Samsung Galaxy, 2.2: CORE/6.506.4.1 OpenCORE/2.02 (Linux;Android 2.2)
        // Emulator 2.2: stagefright/1.0 (Linux;Android 2.2)
        // N1, Cyanogen 7: stagefright/1.1 (Linux;Android 2.3.3)
        return userAgent != null && userAgent.contains("OpenCORE");
    }
}
