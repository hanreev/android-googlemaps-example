package com.example.navdrawer.lib;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class BasicClusterItem implements ClusterItem {
    private LatLng mPosition;
    private String mTittle;
    private String mSnippet;

    public BasicClusterItem(LatLng position, @Nullable String title, @Nullable String snippet) {
        mPosition = position;
        mTittle = title;
        mSnippet = snippet;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Nullable
    @Override
    public String getTitle() {
        return mTittle;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return mSnippet;
    }
}
