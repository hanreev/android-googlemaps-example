package com.example.navdrawer.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.navdrawer.R;
import com.example.navdrawer.lib.BasicClusterItem;
import com.example.navdrawer.lib.BasicClusterRenderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static String TAG = HomeFragment.class.getSimpleName();
    private static int PERMISSION_CODE_ACCESS_FINE_LOCATION = 212;

    private Context mContext;
    private GoogleMap mMap;
    private LatLng mCenterPoint = new LatLng(-3.5552097470269772, 138.95569633309174);
    private LatLngBounds mBounds = new LatLngBounds(
            new LatLng(-3.8405211749999348, 138.6142965010000125),
            new LatLng(-3.2701076399999351, 139.2974821700000803)
    );

    private RequestQueue mRequestQueue;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = requireActivity().getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(mContext);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMapToolbarEnabled(false);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mBounds.getCenter(), 9));

        setupGeoJsonLayer();
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_ACCESS_FINE_LOCATION);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults[0] != -1) enableMyLocation();
        }
    }

    private void setupGeoJsonLayer() {
        if (mMap == null) return;

        try {
            GeoJsonLayer layerJalan = new GeoJsonLayer(mMap, R.raw.jalan, mContext);

            GeoJsonLineStringStyle styleJalan = new GeoJsonLineStringStyle();
            styleJalan.setColor(mContext.getColor(android.R.color.holo_red_light));
            styleJalan.setWidth(8f);

            for (GeoJsonFeature feature : layerJalan.getFeatures()) {
                feature.setLineStringStyle(styleJalan);
            }

            layerJalan.addLayerToMap();

            GeoJsonLayer layerJembatan = new GeoJsonLayer(mMap, R.raw.jembatan, mContext);

            ClusterManager<BasicClusterItem> clusterManager = new ClusterManager<>(mContext, mMap);
            BasicClusterRenderer clusterRenderer = new BasicClusterRenderer(mContext, mMap, clusterManager);
            clusterRenderer.setIcon(bitmapFromDrawable(R.drawable.ic_jembatan_24));
            clusterManager.setRenderer(clusterRenderer);

            GeoJsonPointStyle styleJembatan = new GeoJsonPointStyle();
            styleJembatan.setIcon(bitmapFromDrawable(R.drawable.ic_jembatan_24));

            for (GeoJsonFeature feature : layerJembatan.getFeatures()) {
                feature.setPointStyle(styleJembatan);
                GeoJsonPoint geom = (GeoJsonPoint) feature.getGeometry();
                clusterManager.addItem(new BasicClusterItem(geom.getCoordinates(), null, null));
            }
            mMap.setOnCameraIdleListener(clusterManager);

            layerJembatan.setOnFeatureClickListener(new Layer.OnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    HashMap<String, String> properties = new LinkedHashMap<>();
                    for (String key : feature.getPropertyKeys()) {
                        properties.put(key, feature.getProperty(key));
                    }
                    Log.e(TAG, properties.toString());
                }
            });
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private BitmapDescriptor bitmapFromDrawable(int drawableId) {
        Drawable drawable = mContext.getDrawable(drawableId);
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

    private void createGeoJsonLayer(String layerName, JSONObject jsonObject) {
        GeoJsonLayer layer = new GeoJsonLayer(mMap, jsonObject);

        if (layerName.equals("jembatan")) {
            ClusterManager<BasicClusterItem> clusterManager = new ClusterManager(mContext, mMap);
            BasicClusterRenderer clusterRenderer = new BasicClusterRenderer(mContext, mMap, clusterManager);
            clusterRenderer.setIcon(bitmapFromDrawable(R.drawable.ic_jembatan_24));
            clusterManager.setRenderer(clusterRenderer);

            for (GeoJsonFeature feature : layer.getFeatures()) {
                GeoJsonPoint geom = (GeoJsonPoint) feature.getGeometry();
                clusterManager.addItem(new BasicClusterItem(geom.getCoordinates(), null, null));
            }

            mMap.setOnCameraIdleListener(clusterManager);
        } else if (layerName.equals("jalan")) {
            GeoJsonLineStringStyle styleJalan = new GeoJsonLineStringStyle();
            styleJalan.setColor(mContext.getColor(android.R.color.holo_red_light));
            styleJalan.setWidth(8f);

            for (GeoJsonFeature feature : layer.getFeatures()) {
                feature.setLineStringStyle(styleJalan);
            }

            layer.addLayerToMap();
        }
    }

    private void fetchGeojson(final String layerName) {
        String url = String.format(Locale.ENGLISH, "http://10.0.2.2:8080/%s.geojson", layerName);

        JsonObjectRequest request = new JsonObjectRequest(
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        createGeoJsonLayer(layerName, response);
                        // Log.e("GEOJSON", response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.getMessage(), error);
                    }
                });

        request.setTag(TAG);
        mRequestQueue.add(request);
    }
}