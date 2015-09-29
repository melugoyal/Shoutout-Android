package shoutout2.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

public class MessageNumCircle {
    private int redCircleSize;
    private Bitmap redCircleBase;
    private Bitmap redCircle;
    private ImageView messageNumCircle;
    private LruCache<Integer, Bitmap> messageNumCache = new LruCache<>(10);
    private Resources res;

    public MessageNumCircle(Resources res, ImageView messageNumCircle) {
        redCircleSize = (int) res.getDimension(R.dimen.red_circle_size);
        redCircleBase = BitmapFactory.decodeResource(res, R.drawable.redcircle);
        redCircle = Bitmap.createScaledBitmap(redCircleBase, redCircleSize, redCircleSize, false);
        this.messageNumCircle = messageNumCircle;
        this.res = res;
        setCircle(0);
    }

    public void setCircle(int count) {
        if (count == 0) {
            messageNumCircle.setVisibility(View.INVISIBLE);
            return;
        }
        Bitmap icon = messageNumCache.get(count);
        if (icon != null) {
            messageNumCircle.setImageBitmap(icon);
        }
        else {
            String text = String.valueOf(count);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(res.getDimension(R.dimen.cluster_text_size));
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            float x = redCircle.getWidth() / 2.0f;
            float y = (redCircle.getHeight() - bounds.height()) / 2.0f - bounds.top;
            Canvas canvas = new Canvas(redCircle);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, x, y, paint);
            messageNumCircle.setImageBitmap(redCircle);
        }
        messageNumCircle.setVisibility(View.VISIBLE);
    }
}
