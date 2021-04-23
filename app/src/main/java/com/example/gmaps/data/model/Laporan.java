package com.example.gmaps.data.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Laporan {
    private String mKondisi;
    private String mKeterangan;
    private LatLng mLokasi;
    private Bitmap mGambar;

    public Laporan() {
    }

    public Laporan(String kondisi, LatLng lokasi) {
        this(kondisi, lokasi, null, (Bitmap) null);
    }

    public Laporan(String kondisi, LatLng lokasi, @Nullable String keterangan, @Nullable String gambar) {
        this(kondisi, lokasi, keterangan, (Bitmap) null);
        if (gambar != null) {
            setGambar(gambar);
        }
    }

    public Laporan(String kondisi, LatLng lokasi, @Nullable String keterangan, @Nullable Bitmap gambar) {
        mKondisi = kondisi;
        mLokasi = lokasi;
        mKeterangan = keterangan;
        mGambar = gambar;
    }

    public String getKondisi() {
        return mKondisi;
    }

    public void setKondisi(String kondisi) {
        mKondisi = kondisi;
    }

    public String getKeterangan() {
        return mKeterangan;
    }

    public void setKeterangan(String keterangan) {
        mKeterangan = keterangan;
    }

    public LatLng getLokasi() {
        return mLokasi;
    }

    public void setLokasi(LatLng lokasi) {
        mLokasi = lokasi;
    }

    public Bitmap getGambar() {
        return mGambar;
    }

    public void setGambar(Bitmap gambar) {
        mGambar = gambar;
    }

    public void setGambar(Uri uri) {
        mGambar = BitmapFactory.decodeFile(uri.getPath());
    }

    public void setGambar(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            mGambar = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(Laporan.class.getSimpleName(), e.getMessage());
            mGambar = null;
        }
    }
}
