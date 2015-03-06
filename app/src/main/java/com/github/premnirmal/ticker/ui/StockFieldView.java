package com.github.premnirmal.ticker.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.premnirmal.tickerwidget.R;

/**
 * Created by premnirmal on 3/4/15.
 */
public class StockFieldView extends LinearLayout {

    private TextView fieldName;
    private TextView fieldValue;

    public StockFieldView(Context context) {
        this(context, null);
    }

    public StockFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.stock_field_view, this, true);
        fieldName = ((TextView) findViewById(R.id.fieldname));
        fieldValue = ((TextView) findViewById(R.id.fieldvalue));
        if (attrs != null) {
            final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.StockFieldView);
            final int orientation = array.getInt(R.styleable.StockFieldView_or, 0);
            if (orientation == 0) {
                setOrientation(HORIZONTAL);
                fieldName.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f));
                fieldValue.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f));
                fieldValue.setGravity(Gravity.RIGHT);
            } else {
                setOrientation(VERTICAL);
                fieldName.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.5f));
                fieldValue.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.5f));
                fieldValue.setGravity(Gravity.LEFT);
            }
            setWeightSum(1f);
            final String name = getStringValue(context, array, R.styleable.StockFieldView_name);
            fieldName.setText(name);
            final float textSize = array.getDimensionPixelSize(R.styleable.StockFieldView_size, 20);
            fieldName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            fieldValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * 0.8f);
            array.recycle();
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
