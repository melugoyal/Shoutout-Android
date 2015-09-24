package shoutout2.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.LruCache;

import com.androidmapsextensions.ClusterOptions;
import com.androidmapsextensions.ClusterOptionsProvider;
import com.androidmapsextensions.Marker;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.List;

public class MapClusteringOptions implements ClusterOptionsProvider {
    private Resources res;
    private LruCache<Integer, BitmapDescriptor> clusterIconCache = new LruCache<>(100);
    private ClusterOptions clusterOptions = new ClusterOptions().anchor(0.5f,0.5f);

    public MapClusteringOptions(Resources res) {
        this.res = res;
    }

    @Override
    public ClusterOptions getClusterOptions(List<Marker> markers) {
        int markerCount = markers.size();
        BitmapDescriptor icon = clusterIconCache.get(markerCount);
        if (icon != null) {
            return clusterOptions.icon(icon);
        }
        Bitmap base = BitmapFactory.decodeResource(res, R.drawable.cluster_background);
        Bitmap bitmap = base.copy(Bitmap.Config.ARGB_8888, true);
        String text = String.valueOf(markerCount);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(res.getDimension(R.dimen.cluster_text_size));
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float x = bitmap.getWidth() / 2.0f;
        float y = (bitmap.getHeight() - bounds.height()) / 2.0f - bounds.top;
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, x, y, paint);
        icon = BitmapDescriptorFactory.fromBitmap(bitmap);
        clusterIconCache.put(markerCount, icon);
        return clusterOptions.icon(icon);
    }
}
