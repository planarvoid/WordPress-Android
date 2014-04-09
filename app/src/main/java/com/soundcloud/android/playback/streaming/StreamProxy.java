package com.soundcloud.android.playback.streaming;


import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicLineParser;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.util.functions.Func1;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * orig: http://code.google.com/p/npr-android-app/source/browse/trunk/Npr/src/org/npr/android/news/StreamProxy.java
 */
public class StreamProxy {
    private static final String LOG_TAG = StreamProxy.class.getSimpleName();

    private static final int INITIAL_TIMEOUT  = 15;   // before receiving the first chunk
    private static final int TRANSFER_TIMEOUT = 60;   // subsequent chunks
    private static final String CRLF = "\r\n";

    private static final String SERVER = "SoundCloudStreaming";

    private static final String PARAM_STREAM_URL = "streamUrl";
    private static final String PARAM_NEXT_STREAM_URL = "nextStreamUrl";

    private boolean mIsRunning;
    private Thread mThread;

    private ReplaySubject<Integer> mPortSubject = ReplaySubject.create();

    private static String userAgent;

    private final StreamLoader loader;
    private final StreamStorage storage;

    @Inject
    public StreamProxy(Context context) {
        storage = new StreamStorage(context.getApplicationContext(), Consts.EXTERNAL_STREAM_DIRECTORY);
        loader = new StreamLoader(context.getApplicationContext(), storage);
    }

    public StreamProxy start() {
        mThread = new Thread(new Runnable() {
            @Override public void run() {
                initAndRun();
            }
        }, "StreamProxy-accept");
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
        if (!isRunning()) {
            throw new IllegalStateException("Cannot stop proxy; it has not been started.");
        }
        mIsRunning = false;
        mThread.interrupt();
        try {
            mThread.join(5000);
        } catch (InterruptedException ignored) {
        }
        mThread = null;
        loader.stop();
    }

    public StreamItem getStreamItem(String url) {
        return storage.getMetadata(url);
    }

    public Observable<Uri> uriObservable(final String streamUrl, final @Nullable String nextStreamUrl) {
        if (TextUtils.isEmpty(streamUrl)) {
            return Observable.error(new IllegalArgumentException("streamUrl is empty"));
        } else {
            return mPortSubject.map(new Func1<Integer, Uri>() {
                @Override
                public Uri call(Integer port) {
                    return createUri(port, streamUrl, nextStreamUrl);
                }
            });
        }
    }

    private void initAndRun() {
        try {
            ServerSocket serverSocket = initializeSocket();
            mIsRunning = true;
            mPortSubject.onNext(serverSocket.getLocalPort());
            acceptLoop(serverSocket);
        } catch (IOException e) {
            Log.w(LOG_TAG, "error initialising socket", e);
            mPortSubject.onError(e);
        }
    }

    private void acceptLoop(ServerSocket socket) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "running");
        while (isRunning()) {
            try {
                final Socket client = socket.accept();
                client.setKeepAlive(true);
                client.setSendBufferSize(storage.chunkSize);

                Log.d(LOG_TAG, "client connected: "+client.getRemoteSocketAddress());
                try {
                    final HttpUriRequest request = readRequest(client.getInputStream());
                    new Thread("handle-proxy-request") {
                        @Override
                        public void run() {
                            processRequest(request, client);
                        }
                    }.start();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "error reading request", e);
                    client.close();
                } catch (URISyntaxException e) {
                    Log.w(LOG_TAG, "error reading request - URI malformed", e);
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

    private static Uri createUri(int port, String streamUrl, @Nullable String nextStreamUrl) {
        final Uri.Builder builder = Uri.parse("http://127.0.0.1:" + port)
            .buildUpon()
            .appendPath("/")
            .appendQueryParameter(PARAM_STREAM_URL, streamUrl);
        if (nextStreamUrl != null) {
            builder.appendQueryParameter(PARAM_NEXT_STREAM_URL, nextStreamUrl);
        }
        return builder.build();
    }

    @VisibleForTesting
    protected static HttpUriRequest readRequest(InputStream is) throws IOException, URISyntaxException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
        String line = reader.readLine();

        if (line == null) {
            throw new IOException("Proxy client closed connection without a request.");
        }

        StringTokenizer st = new StringTokenizer(line);
        if (!st.hasMoreTokens()) throw new IOException("Invalid request: "+line);
        String method = st.nextToken();
        if (!st.hasMoreTokens()) throw new IOException("Invalid request: "+line);
        String uri = st.nextToken();
        String realUri = uri.substring(1);

        final HttpUriRequest request;
        if ("GET".equalsIgnoreCase(method)) {
            request = new HttpGet(new URI(realUri));
        } else if ("HEAD".equalsIgnoreCase(method)) {
            request = new HttpHead(new URI(realUri));
        } else {
            throw new IOException("only GET/HEAD is supported, got: " + method);
        }
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) break;
            // copy original headers in new request
            try {
                Header h = BasicLineParser.parseHeader(line, BasicLineParser.DEFAULT);
                if (!"host".equalsIgnoreCase(h.getName())) {
                    request.addHeader(h);
                }
            } catch (ParseException e) {
                Log.w(LOG_TAG, "error parsing header", e);
            }
        }
        return request;
    }

    private ServerSocket initializeSocket() throws IOException {
        ServerSocketChannel socket = ServerSocketChannel.open();
        socket.socket().bind(new InetSocketAddress(0));

        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, "port " + socket.socket().getLocalPort() + " obtained");

        return socket.socket();
    }

    private long firstRequestedByte(HttpRequest request) {
        Header range = request.getFirstHeader("Range");
        if (range != null) {
            return firstRequestedByte(range.getValue());
        } else {
            return 0;
        }
    }

    @VisibleForTesting
    protected static long firstRequestedByte(String range) {
        long startByte = 0;
        if (!TextUtils.isEmpty(range)) {
            Matcher m = Pattern.compile("bytes=(-?\\d+)-?(\\d+)?(?:,.*)?").matcher(range);
            if (m.matches()) {
                startByte = Long.parseLong(m.group(1));
            }
        }
        return startByte < 0 ? 0 : startByte; // we don't support final byte ranges (-100)
    }

    private void processRequest(HttpUriRequest request, Socket client) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            logRequest(request);
        }

        final Uri uri = Uri.parse(request.getURI().toString());
        final String streamUrl = uri.getQueryParameter(PARAM_STREAM_URL);
        final String nextUrl = uri.getQueryParameter(PARAM_NEXT_STREAM_URL);

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG,
                String.format("processRequest: url=%s, port=%d, firstByte=%d", streamUrl, client.getPort(), firstRequestedByte(request)));

        setUserAgent(request);

        try {
            if (streamUrl == null) throw new IOException("missing stream url parameter");

            final long startByte = firstRequestedByte(request);
            final SocketChannel channel = client.getChannel();
            Map<String, String> headers = headerMap();

            final File completeFile = storage.completeFileForUrl(streamUrl);
            if (completeFile.exists()) {
                streamCompleteFile(request, streamUrl, nextUrl, completeFile, startByte, channel, headers);
            } else {
                writeChunks(request, streamUrl, nextUrl, startByte, channel, headers);
            }
        } catch (SocketException e) {
            final String msg = e.getMessage();
            if (msg != null &&
               (msg.contains("Connection reset by peer") ||
                       "Broken pipe".equals(msg))) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, String.format("client %d closed connection [expected]", client.getPort()));
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
        } catch (ExecutionException e) {
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

    private void logRequest(HttpUriRequest request) {
        for (Header h : request.getAllHeaders()) {
            Log.d(LOG_TAG, h.getName()+": "+h.getValue());
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

    private void writeChunks(HttpUriRequest request, String streamUrl, String nextUrl, final long startByte,
                             SocketChannel channel,
                             Map<String, String> headers)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {

        ByteBuffer buffer;
        for (long offset = startByte; isRunning();) {
            StreamFuture stream = loader.getDataForUrl(streamUrl, Range.from(offset, storage.chunkSize));
            try {
                if (offset == startByte) {
                    // first chunk
                    buffer = stream.get(INITIAL_TIMEOUT, TimeUnit.SECONDS);
                    final long length = stream.item.getContentLength();
                    // NB: Content-Length is the number of bytes in the body sent, not the total length of the resource
                    headers.put("Content-Length", String.valueOf(length - offset));
                    headers.put("ETag", stream.item.etag());

                    if (startByte != 0) {
                        headers.put("Content-Range",
                                String.format(Locale.ENGLISH, "%d-%d/%d", startByte, length - 1, length));
                    }
                    ByteBuffer header = getHeader(startByte, headers);
                    channel.write(header);

                    // since we already got some data for this track, ready to queue next one
                    queueNextUrl(nextUrl, 10000);

                    if (request.getMethod().equalsIgnoreCase("HEAD")) break;
                } else {
                    // subsequent chunks
                    buffer = stream.get(TRANSFER_TIMEOUT, TimeUnit.SECONDS);
                }

                if (stream.item == null || stream.item.getContentLength() == 0) {
                    // should not happen
                    IOException e = new IOException("BUG: "+(stream.item == null ? "item is null" : "content-length is 0"));
                    SoundCloudApplication.handleSilentException(null, e);
                    throw e;
                }

                final int written = channel.write(buffer);

                offset += written;
                if (written == 0 || offset >= stream.item.getContentLength()) {
                    if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG,
                            String.format("reached end of stream (%d > %d)", offset, stream.item.getContentLength()));
                    break;
                }
            } catch (TimeoutException e) {
                // timeout happened before header write, take the chance to return a proper error code
                if (offset == startByte) {
                    channel.write(getErrorHeader(503, "Data read timeout"));
                }
                throw e;
            } catch (ExecutionException e) {
                // stream item got probably canceled
                if (offset == startByte) {
                    channel.write(getErrorHeader(stream.item.getHttpError(), "Error"));
                }
                throw e;
            }
        }
    }

    @VisibleForTesting
    protected Map<String, String> headerMap() {
        Map<String, String> h = new LinkedHashMap<String, String>();
        h.put("Server", SERVER);
        h.put("Accept-Ranges", "bytes");
        h.put("Content-Type", "audio/mpeg");
        h.put("Connection", "close");
        h.put("X-SocketTimeout", "0");  // stagefright specific
        h.put("Date", DateUtils.formatDate(new Date()));
        return h;
    }

    private void streamCompleteFile(HttpUriRequest request, String streamUrl, String nextUrl,
                                    File file, long offset,
                                    SocketChannel channel,
                                    Map<String, String> headers) throws IOException {
        Log.d(LOG_TAG, "streaming complete file " + file);
        headers.put("Content-Length", String.valueOf(file.length() - offset));
        channel.write(getHeader(offset, headers));

        if (request.getMethod().equalsIgnoreCase("HEAD")) return;

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
            while (read < file.length() - offset && isRunning()) {
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
