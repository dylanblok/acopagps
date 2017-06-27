package com.example.dylanblok.acopagps2;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.example.dylanblok.acopagps2.MapsActivity;

public class TouchableWrapper extends FrameLayout {

    public TouchableWrapper(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                MapsActivity.mMapIsTouched = true;
                MapsActivity.mMapIsScrolling = true;
                break;
            case MotionEvent.ACTION_UP:
                MapsActivity.mMapIsTouched = false;
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}