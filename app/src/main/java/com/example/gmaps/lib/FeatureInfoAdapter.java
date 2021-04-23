package com.example.gmaps.lib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.gmaps.R;

import java.util.ArrayList;
import java.util.Map;

public class FeatureInfoAdapter extends BaseAdapter {
    private Map<String, String> mProperties;
    private ArrayList<String> mKeys;
    private LayoutInflater mLayoutInflater;

    public FeatureInfoAdapter(Context context, Map<String, String> properties) {
        this(LayoutInflater.from(context), properties);
    }

    public FeatureInfoAdapter(LayoutInflater inflater, Map<String, String> properties) {
        mLayoutInflater = inflater;
        mProperties = properties;
        mKeys = new ArrayList<>(properties.keySet());
    }

    @Override
    public int getCount() {
        return mProperties.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : mLayoutInflater.inflate(R.layout.feature_property_item, parent, false);
        String key = mKeys.get(position);
        String value = mProperties.get(key);
        TextView keyTextView = view.findViewById(R.id.prop_key);
        TextView valTextView = view.findViewById(R.id.prop_value);
        keyTextView.setText(key);
        valTextView.setText(value);
        return view;
    }
}
