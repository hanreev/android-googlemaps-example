package com.example.gmaps.lib;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonPoint;

public class BasicClusterItem implements ClusterItem {
    private LatLng mPosition;
    private String mTittle;
    private String mSnippet;
    private GeoJsonFeature mFeature;

    public BasicClusterItem(LatLng position, @Nullable String title, @Nullable String snippet) {
        mPosition = position;
        mTittle = title;
        mSnippet = snippet;
    }

    public BasicClusterItem(GeoJsonFeature feature, @Nullable String title, @Nullable String snippet) {
        mFeature = feature;
        mPosition = ((GeoJsonPoint) feature.getGeometry()).getCoordinates();
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

    @Nullable
    public GeoJsonFeature getFeature() {
        return mFeature;
    }
}
