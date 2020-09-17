package com.example.navdrawer.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.navdrawer.R;
import com.example.navdrawer.lib.ExpandableListView;
import com.example.navdrawer.lib.FeatureInfoAdapter;
import com.example.navdrawer.lib.GeoJsonParser;
import com.example.navdrawer.lib.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

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

    private View mRootView;
    private RequestQueue mRequestQueue;
    private BottomSheetBehavior<NestedScrollView> mInfoBehavior;
    private ExpandableListView mInfoListView;
    private TextView mInfoTitle;

    private GeoJsonLayer mLayer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = requireActivity().getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(mContext);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        setupFeatureInfo();

        return mRootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMapToolbarEnabled(false);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mBounds.getCenter(), 9));
        mMap.setOnMarkerClickListener(null);

        // setupGeoJsonLayer();
        fetchGeoJson("jalan");
        fetchGeoJson("jembatan");

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
        try {
            JSONObject jalanGeoJson = Utils.createJsonFileObject(getResources().openRawResource(R.raw.jalan));
            createGeoJsonLayer("jalan", jalanGeoJson);

            JSONObject jembatanGeoJson = Utils.createJsonFileObject(getResources().openRawResource(R.raw.jembatan));
            createGeoJsonLayer("jembatan", jembatanGeoJson);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void createGeoJsonLayer(String layerName, JSONObject jsonObject) {
        if (mLayer == null) {
            mLayer = new GeoJsonLayer(mMap, new JSONObject());
            mLayer.addLayerToMap();
            mLayer.setOnFeatureClickListener(new Layer.OnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    showFeatureInfo((GeoJsonFeature) feature);
                }
            });
        }

        GeoJsonParser parser = new GeoJsonParser(jsonObject);

        if (layerName.equals("jembatan")) {
            GeoJsonPointStyle styleJembatan = new GeoJsonPointStyle();
            styleJembatan.setIcon(Utils.bitmapFromDrawable(mContext, R.drawable.ic_jembatan_24));

            for (GeoJsonFeature feature : parser.getFeatures()) {
                feature.setPointStyle(styleJembatan);
                mLayer.addFeature(feature);
            }
        } else if (layerName.equals("jalan")) {
            GeoJsonLineStringStyle styleJalan = new GeoJsonLineStringStyle();
            styleJalan.setColor(mContext.getColor(android.R.color.holo_red_light));
            styleJalan.setWidth(8f);

            for (GeoJsonFeature feature : parser.getFeatures()) {
                feature.setLineStringStyle(styleJalan);
                mLayer.addFeature(feature);
            }
        } else {
            for (GeoJsonFeature feature : parser.getFeatures()) {
                mLayer.addFeature(feature);
            }
        }
    }

    private void fetchGeoJson(final String layerName) {
        String url = "http://10.0.2.2:8080/service.php?action=getGeojson&geojsonTable=" + layerName;

        JsonObjectRequest request = new JsonObjectRequest(
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        createGeoJsonLayer(layerName, response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.getMessage(), error);
                    }
                }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    // Cache the response
                    long refreshInterval = 3 * 60 * 1000; // Refresh every 3 minutes.
                    long expiredIn = 24 * 60 * 60 * 1000; // Invalidate the cache after 24 hours
                    Cache.Entry cacheEntry = Utils.getCacheEntry(response, refreshInterval, expiredIn);

                    String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                } catch (JSONException je) {
                    return Response.error(new ParseError(je));
                }
            }
        };

        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private void setupFeatureInfo() {
        NestedScrollView infoContainer = mRootView.findViewById(R.id.feature_info_container);
        mInfoListView = infoContainer.findViewById(R.id.info_list);
        mInfoTitle = infoContainer.findViewById(R.id.info_title);

        // Setup bottom sheet
        mInfoBehavior = BottomSheetBehavior.from(infoContainer);
        mInfoBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Setup close button
        ImageButton closeButton = infoContainer.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfoBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    private void showFeatureInfo(GeoJsonFeature feature) {
        // Transform feature properties into HashMap
        HashMap<String, String> properties = new HashMap<>();
        for (String key : feature.getPropertyKeys())
            properties.put(key.toUpperCase(), feature.getProperty(key)); // Transform key to upper case

        // Set the title
        String title = feature.getProperty("title");
        if (title != null)
            mInfoTitle.setText(title);

        // Setup list view adapter
        FeatureInfoAdapter adapter = new FeatureInfoAdapter(mContext, properties);
        mInfoListView.setAdapter(adapter);

        // Show bottom sheet
        mInfoBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}