package com.example.gmaps.lib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.gmaps.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReportAdapter extends BaseAdapter {
    private Context m_context;
    private LayoutInflater m_layoutInflater;
    private JSONArray m_reports;

    public ReportAdapter(Context context, JSONArray reports) {
        m_context = context;
        m_layoutInflater = LayoutInflater.from(context);
        m_reports = reports;
    }

    @Override
    public int getCount() {
        return m_reports.length();
    }

    @Override
    public JSONObject getItem(int position) {
        try {
            return m_reports.getJSONObject(position);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        JSONObject report = getItem(position);
        if (report == null) {
            return 0;
        }

        try {
            return report.getInt("id");
        } catch (JSONException e) {
            return 0;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : m_layoutInflater.inflate(R.layout.report_item, parent, false);
        JSONObject report = getItem(position);
        if (report == null) return view;
        try {
            ImageView imageView = view.findViewById(R.id.report_photo);
            Glide.with(m_context)
                    .load("https://sijj-palu.karogis.com/" + report.getString("gambar"))
                    .into(imageView);
            ((TextView) view.findViewById(R.id.val_id)).setText(report.getString("id"));
            ((TextView) view.findViewById(R.id.val_condition)).setText(report.getString("kondisi"));
            String latLong = report.getString("lat") + ", " + report.getString("long");
            ((TextView) view.findViewById(R.id.val_position)).setText(latLong);
            ((TextView) view.findViewById(R.id.val_description)).setText(report.getString("keterangan"));
        } catch (JSONException ignored) {
        }
        return view;
    }
}
