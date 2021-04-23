package com.example.gmaps.lib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

import org.jetbrains.annotations.NotNull;

public final class ExpandableGridView extends GridView {
    private final boolean mIsExpanded;

    public ExpandableGridView(Context context) {
        super(context);
        mIsExpanded = true;
    }

    public ExpandableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsExpanded = true;
    }

    public ExpandableGridView(@NotNull Context context, @NotNull AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIsExpanded = true;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsExpanded) {
            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = this.getMeasuredHeight();
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
