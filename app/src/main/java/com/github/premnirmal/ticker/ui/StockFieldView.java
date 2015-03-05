package com.github.premnirmal.ticker.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.premnirmal.tickerwidget.R;

/**
 * Created by premnirmal on 3/4/15.
 */
public class StockFieldView extends RelativeLayout {

    private TextView fieldName;
    private TextView fieldValue;

    public StockFieldView(Context context) {
        this(context, null);
    }

    public StockFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        fieldName = ((TextView) findViewById(R.id.fieldname));
        fieldValue = ((TextView) findViewById(R.id.fieldvalue));
        if (attrs != null) {
            final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.StockFieldView);
            final int orientation = array.getInt(R.styleable.StockFieldView_or, 0);
            if (orientation == 0) {
                LayoutInflater.from(context).inflate(R.layout.stock_field_view, this, true);
            } else {
                LayoutInflater.from(context).inflate(R.layout.stock_field_view_vertical, this, true);
            }
            final String name = getStringValue(context, array, R.styleable.StockFieldView_name);
            fieldName.setText(name);
            final float textSize = array.getDimensionPixelSize(R.styleable.StockFieldView_size, 20);
            fieldName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            fieldValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * 0.8f);
            array.recycle();
        } else {
            LayoutInflater.from(context).inflate(R.layout.stock_field_view, this, true);
        }
    }

    public StockFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public StockFieldView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }

    public void setText(CharSequence text) {
        fieldValue.setText(text);
    }

    public void setTextColor(int color) {
        fieldValue.setTextColor(color);
    }

    private String getStringValue(Context context, TypedArray array, int stylelable) {
        String name = array.getString(stylelable);
        if (name == null) {
            final int stringId = array.getResourceId(stylelable, -1);
            if (stringId > 0) {
                name = context.getString(stringId);
            } else {
                name = "";
            }
        }
        return name;
    }

}
