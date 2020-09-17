package com.example.navdrawer.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {
    public static JSONObject createJsonFileObject(InputStream stream)
            throws IOException, JSONException {
        String line;
        StringBuilder result = new StringBuilder();
        // Reads from stream
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        // Read each line of the GeoJSON file into a string
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();

        // Converts the result string into a JSONObject
        return new JSONObject(result.toString());
    }

    public static BitmapDescriptor bitmapFromDrawable(Context context, int drawableId) {
        Drawable drawable = context.getDrawable(drawableId);
        if (drawable == null)
            return null;
        Canvas canvas = new Canvas();
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static Cache.Entry getCacheEntry(NetworkResponse response) {
        return getCacheEntry(response, 3 * 60 * 1000, 24 * 60 * 60 * 1000);
    }

    public static Cache.Entry getCacheEntry(NetworkResponse response, final long refreshInterval, final long expiredIn) {
        Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
        if (cacheEntry == null) {
            cacheEntry = new Cache.Entry();
        }

        long now = System.currentTimeMillis();
        final long softExpire = now + refreshInterval;
        final long ttl = now + expiredIn;
        cacheEntry.data = response.data;
        cacheEntry.softTtl = softExpire;
        cacheEntry.ttl = ttl;
        String headerValue = response.headers.get("Date");
        if (headerValue != null)
            cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);

        headerValue = response.headers.get("Last-Modified");
        if (headerValue != null)
            cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);

        cacheEntry.responseHeaders = response.headers;
        return cacheEntry;
    }
}
