package shoutout2.app.MapView;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.mapbox.mapboxsdk.overlay.ClusterMarker;

import shoutout2.app.R;

public class DrawClusterMarker implements ClusterMarker.OnDrawClusterListener {
    private static final int TEXT_SIZE = 16;
    private Resources res;

    public DrawClusterMarker(Resources res) {
        this.res = res;
    }

    @Override
    public Drawable drawCluster(ClusterMarker clusterMarker) {
        Bitmap background = BitmapFactory.decodeResource(res, R.drawable.cluster_icon);
        background = Bitmap.createScaledBitmap(background,
            (int) res.getDimension(R.dimen.cluster_circle_width),
            (int) res.getDimension(R.dimen.cluster_circle_height),
            false);
        int numMarkers = clusterMarker.getMarkersReadOnly().size();
        Canvas canvas = new Canvas(background);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE, res.getDisplayMetrics()));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(numMarkers), canvas.getWidth()/2, canvas.getHeight()/2, paint);
        return new BitmapDrawable(res, background);
    }
}
