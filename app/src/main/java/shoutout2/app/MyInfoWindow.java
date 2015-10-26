package shoutout2.app;

import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.InfoWindow;
import com.mapbox.mapboxsdk.views.MapView;

import shoutout2.app.MapView.MapActivity;

public class MyInfoWindow extends InfoWindow {
    private MapActivity mapActivity;

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
