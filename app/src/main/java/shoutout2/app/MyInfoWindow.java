package shoutout2.app;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.InfoWindow;
import com.mapbox.mapboxsdk.views.MapView;

public class MyInfoWindow extends InfoWindow {
    private MyMapActivity mapActivity;

    public MyInfoWindow(MapView mapView) {
        super(R.layout.no_info_window, mapView);
    }

    @Override
    public void onOpen(Marker marker) {

    }

    @Override
    public void onClose() {
    }
}
