package com.beeflirt.beetalk.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_WIDTH = 1080;
    private static final int MAX_HEIGHT = 1080;
    private static final int COMPRESSION_QUALITY = 80;

    /**
     * Convert relative photo URL to full URL
     */
    public static String getFullPhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return "";
        }
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        return Constants.IMAGE_BASE_URL + photoUrl;
    }

    /**
     * Compress image from Uri. Returns a compressed File ready for upload.
     * Scales down to max 1080x1080 and compresses to JPEG 80% quality.
     */
    public static File compressImage(Context context, Uri imageUri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) throw new Exception("Cannot open image");

        // Decode bounds first
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);
        options.inJustDecodeBounds = false;

        // Decode with inSampleSize
        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        if (bitmap == null) throw new Exception("Failed to decode image");

        // Rotate if needed based on EXIF
        bitmap = rotateIfNeeded(context, imageUri, bitmap);

        // Scale to fit within max dimensions
        bitmap = scaleBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT);

        // Write compressed file
        File outputFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = new FileOutputStream(outputFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, fos);
        fos.flush();
        fos.close();
        bitmap.recycle();

        return outputFile;
    }

    /**
     * Compress image from File path. Returns a compressed File ready for upload.
     */
    public static File compressImage(Context context, File inputFile) throws Exception {
        return compressImage(context, Uri.fromFile(inputFile));
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private static Bitmap rotateIfNeeded(Context context, Uri uri, Bitmap bitmap) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return bitmap;
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();

            int rotation = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270; break;
            }

            if (rotation == 0) return bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return rotated;
        } catch (Exception e) {
            return bitmap;
        }
    }
}
