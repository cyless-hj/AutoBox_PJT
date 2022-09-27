package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DtItemView extends LinearLayout {
    TextView textDate;
    TextView textTime;

    public DtItemView(Context context) {
        super(context);
        init(context);
    }

    public DtItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dt_item_view, this, true);

        textDate = (TextView) findViewById(R.id.text_date);
        textTime = (TextView) findViewById(R.id.text_time);
    }

    public void setTextDate(String date) {
        textDate.setText(String.valueOf(date));
    }

    public void setTextTime(String time) {
        textTime.setText(String.valueOf(time));
    }


}
