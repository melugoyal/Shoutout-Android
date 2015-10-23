package shoutout2.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.parse.ParseQuery;
import com.parse.ParseUser;

public class Utils {

    protected static boolean usernameTaken(String username) {
        ParseQuery<ParseUser> newUserQuery = ParseUser.getQuery();
        newUserQuery.whereEqualTo("username", username);
        try {
            return newUserQuery.count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    protected static Bitmap getCroppedBitmap(Bitmap sbmp) {
        Bitmap output = Bitmap.createBitmap(sbmp.getWidth(),
                sbmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, sbmp.getWidth(), sbmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(sbmp.getWidth() / 2f, sbmp.getHeight() / 2f,
                sbmp.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);
        return output;
    }
}
