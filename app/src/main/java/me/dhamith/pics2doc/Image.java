package me.dhamith.pics2doc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Image {
    private static List<Image> selectedImages;
    private String name;
    private Bitmap preview;
    private Uri uri;

    public static List<Image> getSelectedImages() {
        if (selectedImages == null) {
            selectedImages = new ArrayList<>();
        }
        return selectedImages;
    }

    public static Image fromUri(Context context, Uri uri) throws IOException {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex;
        Image file = new Image();
        if (returnCursor != null) {
            nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            file.setName(returnCursor.getString(nameIndex));
            file.setUri(uri);
            file.preview = context.getContentResolver().loadThumbnail(uri, new Size(640, 480), null);
            returnCursor.close();
        }
        return file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
