package shoutout2.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.parse.ParseObject;

public class MessageArrayAdapter<T> extends ArrayAdapter<T> {
    private List<ParseObject> messageObjects = new ArrayList<>();
    private Map<String, Person> people;
    private ImageButton messageButton;
    private Resources res;
    float dpWidth;
    int messageImageWidth = -1;
    public MessageArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, Map<String, Person> people, ImageButton messageButton, Resources res) {
        super(context, resource, textViewResourceId, objects);
        for (T object : objects) {
            messageObjects.add((ParseObject)object);
        }
        this.people = people;
        this.messageButton = messageButton;
        this.res = res;
        getScreenInfo();
    }

    void getScreenInfo() {
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        messageImageWidth = (int) ((dpWidth - res.getDimension(R.dimen.message_image_padding)) * displayMetrics.density);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        ImageView messageImage = (ImageView) itemView.findViewById(R.id.messageBubble);
        TextView text = (TextView) itemView.findViewById(R.id.label);
        if (position == 0) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
            params.width = messageButton.getWidth();
            params.height = messageButton.getHeight();
            params.setMarginStart((int) res.getDimension(R.dimen.inbox_text_padding));
            imageView.setLayoutParams(params);
            messageImage.setVisibility(View.GONE);

            imageView.setImageResource(R.drawable.message);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageButton.callOnClick();
                }
            });
            text.setText("Inbox");
            text.setTypeface(null, Typeface.BOLD);
        }
        else {
            messageImage.getLayoutParams().width = messageImageWidth;
            ParseObject messageObject = messageObjects.get(position);
            text.setText(messageObject.getString("message"));
            if (!messageObject.getBoolean("read")) {
                text.setTypeface(null, Typeface.BOLD);
                messageObject.put("read", true);
                messageObject.saveInBackground();
            }
            else {
                text.setTypeface(null, Typeface.NORMAL);
            }
            try {
                imageView.setImageBitmap(people.get(messageObject.fetchIfNeeded().getParseUser("from").getObjectId()).icon);
            } catch (Exception e) {
                Log.e("IMAGE FOR MESSAGE VIEW", messageObject.getObjectId() + " " + people.get(messageObject.getParseUser("from").getObjectId()));
            }
        }
        return itemView;
    }
}
