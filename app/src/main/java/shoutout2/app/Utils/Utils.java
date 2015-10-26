package shoutout2.app.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import shoutout2.app.MapView.MapActivity;
import shoutout2.app.Permissions;

public class Utils {

    public static int SELECT_FILE = 1;
    public static int REQUEST_CAMERA = 2;

    public static boolean usernameTaken(String username) {
        ParseQuery<ParseUser> newUserQuery = ParseUser.getQuery();
        newUserQuery.whereEqualTo("username", username);
        try {
            return newUserQuery.count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static Bitmap getCroppedBitmap(Bitmap sbmp) {
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

    public static void startMapActivity(Activity activity) {
        Intent intent = new Intent(activity, MapActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void replaceFragment(FragmentManager fragmentManager, int old, String tag, Fragment fragment) {
        fragmentManager
                .beginTransaction()
//                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(old,
                        fragment,
                        tag)
                .addToBackStack(null)
                .commit();
    }

    public static Bitmap viewToBitmap(View v) {
        v.setDrawingCacheEnabled(true);
        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    public static String dateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("M/d/yy h:mm a");
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
    }


    public static void selectImage(final Fragment origin) {
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(origin.getActivity());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    origin.startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Choose from Library")) {
                    if (Permissions.requestPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        startChoosePhotoIntent(origin);
                    }
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public static void startChoosePhotoIntent(Fragment origin) {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        origin.startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                SELECT_FILE);
    }

    public static Bitmap getUserIcon(final ParseUser user) {
        final String urlString = user.getString("picURL");
        if (urlString != null) {
            try {
                URL url = new URL(urlString);
                Bitmap icon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId()) && user.get("profileImage") == null) { // upload the user's image to Parse
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    ParseFile imageFile = new ParseFile(user.getObjectId() + "_pic.png", stream.toByteArray());
                    final ParseObject imageObj = new ParseObject("Images");
                    imageObj.put("image", imageFile);
                    imageObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            user.put("profileImage", imageObj);
                            user.remove("picURL");
                            user.saveInBackground();
                        }
                    });
                }
                return icon;
            } catch (Exception e) {
            }
        } else {
            try {
                ParseObject imageObject = user.fetchIfNeeded().getParseObject("profileImage");
                byte[] fileData = imageObject.fetchIfNeeded().getParseFile("image").getData();
                return BitmapFactory.decodeByteArray(fileData, 0, fileData.length);
            } catch (Exception e) {
                Log.e("ERROR GETTING ICON", user.getObjectId() + "\n" + e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static Bitmap photoActivityResultHelper(Fragment origin, int requestCode, Intent data) {
        Bitmap thumbnail = null;
        if (requestCode == REQUEST_CAMERA) {
            thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            File destination = new File(Environment.getExternalStorageDirectory(),
                    System.currentTimeMillis() + ".png");
            FileOutputStream fo;
            try {
                destination.createNewFile();
                fo = new FileOutputStream(destination);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == SELECT_FILE) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = origin.getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            thumbnail = BitmapFactory.decodeFile(picturePath);
        }
        return thumbnail;

    }
}
