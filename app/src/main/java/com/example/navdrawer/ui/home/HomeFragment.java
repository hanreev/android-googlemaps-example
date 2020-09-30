package com.example.navdrawer.ui.home;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
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
import com.example.navdrawer.lib.KmpMultipartRequest;
import com.example.navdrawer.lib.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static String TAG = HomeFragment.class.getSimpleName();
    private static int PERMISSION_CODE_ACCESS_FINE_LOCATION = 212;
    private static int PERMISSION_CODE_ACCESS_CAMERA = 213;
    private static int PERMISSION_CODE_READ_EXTERNAL_STORAGE = 213;
    private static int REQUEST_CODE_TAKE_PHOTO = 11;
    private static int REQUEST_CODE_PICK_IMAGE = 12;

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

    private BottomSheetBehavior<NestedScrollView> mFormLaporanBehavior;
    private LinearLayout mFormLaporanContentLayout;
    private Bitmap mFormLaporanImage;
    private ImageView mFormLaporanImagePreview;

    private GeoJsonLayer mLayer;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = requireActivity().getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(mContext);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        setupFeatureInfo();
        setupFormLaporan();

        FloatingActionButton fabLaporan = mRootView.findViewById(R.id.fab_laporan);
        fabLaporan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLaporan();
            }
        });

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
        if (requestCode == PERMISSION_CODE_ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }

        if (requestCode == PERMISSION_CODE_ACCESS_CAMERA && grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePhoto();
        }

        if (requestCode == PERMISSION_CODE_READ_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchPickImage();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_TAKE_PHOTO || requestCode == REQUEST_CODE_PICK_IMAGE) {
            mFormLaporanImage = null;
        }

        if (requestCode == REQUEST_CODE_TAKE_PHOTO && data != null) {
            mFormLaporanImage = (Bitmap) data.getExtras().get("data");
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE && data != null) {
            Uri imageUri = data.getData();
            ContentResolver contentResolver = mContext.getContentResolver();
            try {
                if (Build.VERSION.SDK_INT < 28) {
                    mFormLaporanImage = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
                } else {
                    ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, imageUri);
                    mFormLaporanImage = ImageDecoder.decodeBitmap(source);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        if (mFormLaporanImagePreview != null) {
            mFormLaporanImagePreview.setImageBitmap(mFormLaporanImage);
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

    private void setupFormLaporan() {
        NestedScrollView formContainer = mRootView.findViewById(R.id.form_laporan_container);
        mFormLaporanContentLayout = formContainer.findViewById(R.id.content_layout);
        mFormLaporanImagePreview = formContainer.findViewById(R.id.image_preview);

        // Setup bottom sheet
        mFormLaporanBehavior = BottomSheetBehavior.from(formContainer);
        mFormLaporanBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Setup close button
        ImageButton closeButton = formContainer.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFormLaporanBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        ImageButton btnLokasi = formContainer.findViewById(R.id.btn_lokasi);
        final EditText lokasiEditText = formContainer.findViewById(R.id.lokasi);

        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setExpirationDuration(5000);

        final LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                lokasiEditText.setText(String.format("%s, %s", location.getLatitude(), location.getLongitude()));
            }
        };

        btnLokasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocation(locationRequest, locationCallback);
            }
        });

        ImageButton btnTakePhoto = formContainer.findViewById(R.id.btn_take_photo);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        ImageButton btnPickImage = formContainer.findViewById(R.id.btn_pick_image);
        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        Button btnSubmit = formContainer.findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitForm();
            }
        });
    }

    private void clearFormLaporan() {
        if (mFormLaporanContentLayout == null) return;

        // Clear form
        EditText kondisiEditText = mFormLaporanContentLayout.findViewById(R.id.kondisi);
        EditText keteranganEditText = mFormLaporanContentLayout.findViewById(R.id.keterangan);
        EditText lokasiEditText = mFormLaporanContentLayout.findViewById(R.id.lokasi);

        kondisiEditText.setText(null);
        keteranganEditText.setText(null);
        lokasiEditText.setText(null);
        mFormLaporanImagePreview.setImageBitmap(null);
        mFormLaporanImage = null;
    }

    private void addLaporan() {
        if (mFormLaporanContentLayout == null) return;

        clearFormLaporan();

        // Set to current location
        EditText lokasiEditText = mFormLaporanContentLayout.findViewById(R.id.lokasi);
        LatLng position = mMap.getCameraPosition().target;
        lokasiEditText.setText(String.format("%s, %s", position.latitude, position.longitude));

        mFormLaporanBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void takePhoto() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE_ACCESS_CAMERA);
        } else {
            dispatchTakePhoto();
        }
    }

    private void pickImage() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE_READ_EXTERNAL_STORAGE);
        } else {
            dispatchPickImage();
        }
    }

    private void dispatchTakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }

    private void dispatchPickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    private void requestLocation(LocationRequest request, LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_ACCESS_FINE_LOCATION);
            return;
        }

        if (mFusedLocationProviderClient == null) {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        }

        mFusedLocationProviderClient.requestLocationUpdates(request, callback, null);
    }

    private void submitForm() {
        TextInputLayout kondisiLayout = mFormLaporanContentLayout.findViewById(R.id.layout_kondisi);

        final EditText kondisiEditText = mFormLaporanContentLayout.findViewById(R.id.kondisi);
        EditText keteranganEditText = mFormLaporanContentLayout.findViewById(R.id.keterangan);
        EditText lokasiEditText = mFormLaporanContentLayout.findViewById(R.id.lokasi);

        final String kondisi = kondisiEditText.getText().toString();
        final String keterangan = keteranganEditText.getText().toString();
        final String lokasi = lokasiEditText.getText().toString();

        kondisiLayout.setError(null);
        if (kondisi.isEmpty()) {
            kondisiLayout.setError("Kolom kondisi harus diisi");
            return;
        }

        KmpMultipartRequest request = new KmpMultipartRequest("http://10.0.2.2:8080/service.php?action=inputKerusakan", new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                // TODO: Handle response data
                String responseString = new String(response.data, StandardCharsets.UTF_8);
                Log.e(TAG, responseString);

                Toast.makeText(mContext, "Laporan berhasil dibuat", Toast.LENGTH_SHORT).show();
                clearFormLaporan();
                mFormLaporanBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, new String(error.networkResponse.data), error);

                Toast.makeText(mContext, "ERROR: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("kondisi", kondisi);
                params.put("keterangan", keterangan);
                params.put("kondisi", kondisi);

                // Split latitude and longitude
                String[] lokasiParts = lokasi.split(", ?");
                if (lokasiParts.length == 2) {
                    params.put("lat", lokasiParts[0]);
                    params.put("long", lokasiParts[1]);
                }

                return params;
            }

            @Override
            protected Map<String, DataPart> getDataParams() {
                HashMap<String, DataPart> dataParams = new HashMap<>();
                if (mFormLaporanImage != null) {
                    dataParams.put("gambar", KmpMultipartRequest.newDataFromBitmap(String.format("%s.png", System.currentTimeMillis()), mFormLaporanImage));
                }
                return dataParams;
            }
        };

        mRequestQueue.add(request);
    }

}