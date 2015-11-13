package shoutout2.app.MapView;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import shoutout2.app.ListViewArrayAdapter;
import shoutout2.app.MessageArrayAdapter;
import shoutout2.app.R;

public class ListViewFragment extends Fragment {
    public static final String TAG = "list_view_fragment";
    private MapActivity mapActivity;
    private ListView pinsListView;
    private TextView pinCount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapActivity = (MapActivity) getActivity();
        View view = inflater.inflate(R.layout.list_view, container, false);
        pinsListView = (ListView) view.findViewById(R.id.pins_list);
        pinCount = (TextView) view.findViewById(R.id.numPins);
        getPins();
        pinsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ParseUser item = (ParseUser) pinsListView.getAdapter().getItem(i);
                mapActivity.onBackPressed();
                try {
                    mapActivity.startMessageTo(item.fetchIfNeeded().getUsername());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    protected void getPins() {
        final ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        Point size = new Point();
        mapActivity.getWindowManager().getDefaultDisplay().getSize(size);
        ILatLng southWest = mapActivity.mapFragment.map.getProjection()
                .fromPixels(getResources().getDimension(R.dimen.list_view_padding_side),
                            getResources().getDimension(R.dimen.list_view_padding_top));
        ILatLng northEast = mapActivity.mapFragment.map.getProjection()
                .fromPixels(size.x - getResources().getDimension(R.dimen.list_view_padding_side),
                            getResources().getDimension(R.dimen.list_view_padding_side));
        ParseGeoPoint sw = new ParseGeoPoint(southWest.getLatitude(), southWest.getLongitude());
        ParseGeoPoint ne = new ParseGeoPoint(northEast.getLatitude(), northEast.getLongitude());
        userQuery.whereEqualTo("visible", true);
        userQuery.whereWithinGeoBox("geo", sw, ne);
        userQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> userList, ParseException e) {
                if (e == null && userList != null) {
                    ArrayAdapter<ParseUser> adapter = new ListViewArrayAdapter<>(mapActivity, R.layout.single_pin_view, R.id.status, userList, mapActivity.people);
                    pinsListView.setAdapter(adapter);
                    pinCount.setText(Integer.toString(userList.size()) + " Pins");
                }
            }
        });
    }
}
