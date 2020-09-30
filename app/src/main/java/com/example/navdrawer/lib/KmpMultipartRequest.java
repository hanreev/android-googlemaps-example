package com.example.navdrawer.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public abstract class KmpMultipartRequest extends Request<NetworkResponse> {
    private final String TAG = KmpMultipartRequest.class.getSimpleName();
    private final Object mLock;
    private Response.Listener<NetworkResponse> mListener;
    private final String mBoundary;
    private final String mLineEnding;
    private final String mHyphens;
    private static final float BITMAP_MAX_SIZE = 1024.0F;

    public KmpMultipartRequest(String url, Response.Listener<NetworkResponse> listener, @Nullable Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        mLock = new Object();
        mListener = listener;
        mBoundary = "WebKitFormBoundary" + System.currentTimeMillis();
        mLineEnding = "\r\n";
        mHyphens = "--";
    }

    protected abstract Map<String, DataPart> getDataParams();

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + mBoundary;
    }

    @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            Map<String, String> params = this.getParams();
            if (params != null) {
                for (String key : params.keySet()) {
                    this.buildTextPart(dos, key, params.get(key));
                }
            }

            Map<String, DataPart> dataParams = this.getDataParams();
            if (dataParams != null) {
                for (String key : dataParams.keySet()) {
                    this.buildDataPart(dos, key, dataParams.get(key));
                }
            }

            dos.writeBytes(mHyphens + mBoundary + mHyphens + mLineEnding);
            return bos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (mLock) {
            mListener = null;
        }
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error((VolleyError) (new ParseError(e)));
        }
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        Response.Listener<NetworkResponse> listener;

        synchronized (mLock) {
            listener = mListener;
        }

        if (listener != null) {
            listener.onResponse(response);
        }
    }


    private void buildTextPart(DataOutputStream dos, String key, String value) throws IOException {
        dos.writeBytes(mHyphens + mBoundary + mLineEnding);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + '"' + mLineEnding);
        dos.writeBytes(mLineEnding);
        dos.writeBytes(value + mLineEnding);
    }

    private void buildDataPart(DataOutputStream dos, String key, KmpMultipartRequest.DataPart data) throws IOException {
        dos.writeBytes(mHyphens + mBoundary + mLineEnding);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + data.getFilename() + '"' + mLineEnding);
        if (data.getMimeType() != null) {
            dos.writeBytes("Content-Type: " + data.getMimeType() + mLineEnding);
        }

        dos.writeBytes(mLineEnding);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getData());
        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(inputStream.available(), maxBufferSize);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = inputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize);
            bufferSize = Math.min(inputStream.available(), maxBufferSize);
            bytesRead = inputStream.read(buffer, 0, bufferSize);
        }
        inputStream.close();

        dos.writeBytes(mLineEnding);
    }

    public static final class DataPart {
        private String mFilename;
        private byte[] mData;
        private String mMimeType;

        public final String getFilename() {
            return mFilename;
        }

        public final void setFilename(String mFilename) {
            this.mFilename = mFilename;
        }

        public final byte[] getData() {
            return this.mData;
        }

        public final void setData(byte[] data) {
            this.mData = data;
        }

        @Nullable
        public final String getMimeType() {
            return mMimeType;
        }

        public final void setMimeType(@Nullable String mimeType) {
            mMimeType = mimeType;
        }

        public DataPart(String filename, byte[] data, @Nullable String mimeType) {
            mFilename = filename;
            mData = data;
            mMimeType = mimeType;
        }
    }

    public static DataPart newDataFromDrawable(String filename, Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return newDataFromBitmap(filename, bitmap);
    }

    public static DataPart newDataFromBitmap(String filename, Bitmap bitmap) {
        float ratio = Math.min(
                BITMAP_MAX_SIZE / bitmap.getWidth(),
                BITMAP_MAX_SIZE / bitmap.getHeight()
        );
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        return new DataPart(filename, bos.toByteArray(), "image/png");
    }

    public static DataPart newDataFromUri(Context context, Uri uri, @Nullable String filename) throws IOException {
        if (uri.getPath() == null) {
            throw new FileNotFoundException("Invalid file path");
        } else {
            String mimeType = context.getContentResolver().getType(uri);
            return newDataFromFile(context, new File(uri.getPath()), filename, mimeType);
        }
    }

    public static DataPart newDataFromFile(Context context, File file, @Nullable String filename, @Nullable String mimeType) throws IOException {
        byte[] byteArray = new byte[(int) file.length()];
        BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file));
        buffer.read(byteArray, 0, byteArray.length);
        buffer.close();
        return new DataPart(
                filename == null ? file.getName() : filename,
                byteArray,
                mimeType == null ? context.getContentResolver().getType(Uri.fromFile(file)) : mimeType
        );
    }
}
