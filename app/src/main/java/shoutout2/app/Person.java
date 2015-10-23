package shoutout2.app;

import android.graphics.Bitmap;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Person {
    public String userId;
    public String status;
    public String username;
    public String updatedAt;
    public Bitmap emptyStatusIcon;
    public Person(ParseUser user) {
        this.userId = user.getObjectId();
        this.username = user.getUsername();
        setFields(user.getString("status"), user.getUpdatedAt());
    }

    public void setFields(String status, Date date) {
        this.status = status;
        updatedAt = dateToString(date);
    }

    private String dateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("M/d/yy h:mm a");
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
    }

    public ParseUser getParseUserObject() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", userId);
        try {
            return query.getFirst();
        } catch (Exception e) {
            return null;
        }
    }
}
