package com.example.navdrawer.lib;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class BasicClusterRenderer extends DefaultClusterRenderer<BasicClusterItem> {
    private BitmapDescriptor mIcon;

    public BasicClusterRenderer(Context context, GoogleMap map, ClusterManager<BasicClusterItem> clusterManager) {
        super(context, map, clusterManager);
    }

    public void setIcon(BitmapDescriptor icon) {
        mIcon = icon;
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull BasicClusterItem item, @NonNull MarkerOptions markerOptions) {
        if (mIcon != null) markerOptions.icon(mIcon);
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}
