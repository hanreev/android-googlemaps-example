package com.example.gmaps.ui.report;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.gmaps.R;
import com.example.gmaps.lib.ReportAdapter;

import org.json.JSONArray;

public class ReportFragment extends Fragment {
    private static final String TAG = ReportFragment.class.getSimpleName();

    private Context mContext;
    private RequestQueue mRequestQueue;
    private ListView mListView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        requireContext();
        mContext = requireActivity().getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(mContext);

        View root = inflater.inflate(R.layout.fragment_report, container, false);
        mListView = root.findViewById(R.id.report_list_view);
        fetchReport();
        return root;
    }

    private void fetchReport() {
        JsonArrayRequest request = new JsonArrayRequest(
                "https://sijj-palu.karogis.com/service.php?action=indexKerusakan",
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        ReportAdapter adapter = new ReportAdapter(mContext, response);
                        mListView.setAdapter(adapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.getMessage(), error);
                    }
                }
        );

        request.setTag(TAG);
        mRequestQueue.add(request);
    }
}