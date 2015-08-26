package net.hockeyapp.android.utils;

import android.os.Build;
import android.text.TextUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h3>Description</h3>
 * <p/>
 * Base class for asynchronous HTTP connections.
 * <p/>
 * <h3>License</h3>
 * <p/>
 * <pre>
 * Copyright (c) 2011-2015 Bit Stadium GmbH
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 *
 * @author Matthias Wenz
 **/
public class HttpURLConnectionBuilder {

    private static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;
    public static final String DEFAULT_CHARSET = "UTF-8";

    private final String mUrlString;

    private String mRequestMethod;
    private String mRequestBody;
    private int mTimeout = DEFAULT_TIMEOUT;

    private final Map<String, String> mHeaders;

    public HttpURLConnectionBuilder(String urlString) {
        mUrlString = urlString;
        mHeaders = new HashMap<String, String>();
    }

    public HttpURLConnectionBuilder setRequestMethod(String requestMethod) {
        mRequestMethod = requestMethod;
        return this;
    }

    public HttpURLConnectionBuilder setRequestBody(String requestBody) {
        mRequestBody = requestBody;
        return this;
    }

    public HttpURLConnectionBuilder writeFormFields(Map<String, String> fields) {
        try {
            String formString = getFormString(fields, DEFAULT_CHARSET);
            setHeader("Content-Type", "application/x-www-form-urlencoded");
            setRequestBody(formString);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HttpURLConnectionBuilder setTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout has to be positive.");
        }
        mTimeout = timeout;
        return this;
    }

    public HttpURLConnectionBuilder setHeader(String name, String value) {
        mHeaders.put(name, value);
        return this;
    }

    public HttpURLConnectionBuilder setBasicAuthorization(String username, String password) {
        String authString = "Basic " + net.hockeyapp.android.utils.Base64.encodeToString(
                (username + ":" + password).getBytes(), android.util.Base64.NO_WRAP);

        setHeader("Authorization", authString);
        return this;
    }

    public HttpURLConnection build() {
        HttpURLConnection connection;
        try {
            URL url = new URL(mUrlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(mTimeout);
            connection.setReadTimeout(mTimeout);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
                connection.setRequestProperty("Connection", "close");
            }

            if (!isNullOrEmptyString(mRequestMethod)) {
                connection.setRequestMethod(mRequestMethod);
                if (!isNullOrEmptyString(mRequestBody) || mRequestMethod.equalsIgnoreCase("POST") || mRequestMethod.equalsIgnoreCase("PUT")) {
                    connection.setDoOutput(true);
                }
            }

            for (String name : mHeaders.keySet()) {
                connection.setRequestProperty(name, mHeaders.get(name));
            }

            if (!isNullOrEmptyString(mRequestBody)) {
                OutputStream outputStream = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, DEFAULT_CHARSET));
                writer.write(mRequestBody);
                writer.flush();
                writer.close();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    private static String getFormString(Map<String, String> params, String charset) throws UnsupportedEncodingException {
        List<String> protoList = new ArrayList<String>();
        for (String key : params.keySet()) {
            String value = params.get(key);
            key = URLEncoder.encode(key, charset);
            value = URLEncoder.encode(value, charset);
            protoList.add(key + "=" + value);
        }
        return TextUtils.join("&", protoList);
    }

    private static boolean isNullOrEmptyString(String in) {
        return in == null || in.isEmpty();
    }

}
