
package com.soundcloud.utils.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;

public class HttpUtils {

    private static HttpUtils instance_ = new HttpUtils();

    public static HttpUtils get() {
        return instance_;
    }

    public class HttpResult {
        public int code;

        public String content;

        public Exception exception;

        public HttpResponse response;

        public boolean successful() {
            return (code < 400 && code > 199);
        }
    }

    private class AsyncRunner extends AsyncTask<HttpTask, Integer, HttpResult> {

        private HttpTask task_;

        @Override
        protected HttpResult doInBackground(HttpTask... params) {
            task_ = params[0];
            return task_.runAsAsyncTask();
        }

        @Override
        protected void onPostExecute(HttpResult result) {
            super.onPostExecute(result);
            task_.doCallback(result);
        }

    }

    private class HttpTask implements Runnable {
        private HttpCallback callback_;

        private HttpResponse response_;

        private Exception exception_;

        private Map<String, Object> postData_;

        private String url_;

        private ProgressListener listener_;

        public HttpTask(String url, HttpCallback callback_) {
            super();
            url_ = url;
            this.callback_ = callback_;
        }

        public ProgressListener getListener() {
            return listener_;
        }

        public void setListener(ProgressListener listener) {
            listener_ = listener;
        }

        public Map<String, Object> getPostData() {
            return postData_;
        }

        public void setPostData(Map<String, Object> postData) {
            postData_ = postData;
        }

        public HttpResult runAsAsyncTask() {
            return doHttp(url_, postData_, listener_, false);
        }

        public void run() {
            HttpResult result = doHttp(url_, postData_, listener_, false);
            doCallback(result);
        }

        public void doCallback(HttpResult result) {
            if (callback_ != null) {
                if (result.exception != null)
                    callback_.onError(result.exception);
                else
                    callback_.onResponse(result.response);
            }
        }
    }

    private HttpUtils() {
    }

    public static String responseToString(HttpResponse resp) {
        try {
            return convertStreamToString(resp.getEntity().getContent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public String get(String url) {
        HttpResult res = getResource(url);
        if (res.exception != null) {
            throw new RuntimeException(res.exception);
        } else {
            return res.content;
        }
    }

    public HttpResult getResource(String url) {
        HttpResult result = new HttpResult();

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);

            result.code = response.getStatusLine().getStatusCode();
            result.content = convertStreamToString(response.getEntity().getContent());

        } catch (Exception e) {
            result.exception = e;
        }

        return result;
    }

    public void get(String url, HttpCallback callback) {
        HttpTask task = new HttpTask(url, callback);
        // Thread t = new Thread(task);
        // t.start();
        AsyncRunner runner = new AsyncRunner();
        runner.execute(task);
    }

    public void post(String url, Map<String, Object> data, HttpCallback callback) {
        HttpTask task = new HttpTask(url, callback);
        task.setPostData(data);
        // Thread t = new Thread(task);
        // t.start();
        AsyncRunner runner = new AsyncRunner();
        runner.execute(task);
    }

    public HttpResult post(String url, Map<String, Object> data) {
        return postWithProgress(url, data, null);
    }

    public HttpResult postWithProgress(String url, Map<String, Object> data,
            ProgressListener listener) {
        return postWithProgress(url, data, listener, true);
    }

    public HttpResult postWithProgress(String url, Map<String, Object> data,
            ProgressListener listener, boolean stringifyresp) {
        HttpTask task = new HttpTask(url, null);
        task.setListener(listener);
        task.setPostData(data);
        task.run();
        int code = task.response_.getStatusLine().getStatusCode();

        HttpResult result = new HttpResult();
        result.code = code;

        if (task.exception_ != null) {
            task.exception_.printStackTrace();
            result.exception = task.exception_;
        } else if (code > 399 || code < 200) {
            result.content = null;
        } else if (stringifyresp) {
            try {
                result.content = convertStreamToString(task.response_.getEntity().getContent());
            } catch (Exception e) {
                result.content = null;
                result.exception = e;
            }
        }
        return result;
    }

    public HttpResult doHttp(String url, Map<String, Object> postData, ProgressListener listener,
            boolean stringifyresp) {
        HttpResult result = new HttpResult();
        HttpResponse response = null;

        try {
            HttpClient client = new DefaultHttpClient();

            if (postData == null) {
                HttpGet request = new HttpGet(url);
                response = client.execute(request);
                result.response = response;
                result.code = response.getStatusLine().getStatusCode();
            } else {
                HttpPost request = new HttpPost(url);

                boolean multipart = false;
                for (String param : postData.keySet()) {
                    if (postData.get(param) instanceof File) {
                        multipart = true;
                    }
                }

                if (multipart) {
                    MultipartEntity entity = new MultipartEntity();
                    for (String param : postData.keySet()) {
                        if (postData.get(param) instanceof File) {
                            entity.addPart(param, new FileBody((File) postData.get(param)));
                        } else {
                            entity.addPart(param, new StringBody("" + postData.get(param)));
                        }
                    }
                    request.setEntity(new CountingMultipartRequestEntity(entity, listener));
                } else {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    for (String param : postData.keySet()) {
                        nameValuePairs.add(new BasicNameValuePair(param, "" + postData.get(param)));
                    }
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                }

                response = client.execute(request);

                result.code = response.getStatusLine().getStatusCode();
                result.response = response;
                if (stringifyresp)
                    result.content = responseToString(response);
            }

        } catch (Exception e) {
            result.exception = e;
        }

        return result;
    }

    public interface HttpCallback {

        public void onResponse(HttpResponse resp);

        public void onError(Exception e);

    }
}
