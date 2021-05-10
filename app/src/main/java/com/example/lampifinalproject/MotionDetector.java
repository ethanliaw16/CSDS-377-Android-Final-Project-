package com.example.lampifinalproject;

import android.graphics.Bitmap;
import android.graphics.Color;

public class MotionDetector {
    private static final int mPixelThreshold = 50; // Difference in pixel (RGB)
    private static final int mThreshold = 10000; // Number of different pixels
    // (RGB)

    public static final int A = 0;
    public static final int R = 1;
    public static final int G = 2;
    public static final int B = 3;

    public static final int H = 0;
    public static final int S = 1;
    public static final int L = 2;

    private static int[] mPrevious = null;
    private static int mPreviousWidth = 0;
    private static int mPreviousHeight = 0;

    /**
     * Get RGB values from pixel.
     *
     * @param pixel
     *            Integer representation of a pixel.
     * @return float array of a,r,g,b values.
     */
    public static float[] getARGB(int pixel) {
        int a = (pixel >> 24) & 0xff;
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = (pixel) & 0xff;
        return (new float[] { a, r, g, b });
    }

     /* Get HSL (Hue, Saturation, Luma) from RGB. Note1: H is 0-360 (degrees)
            * Note2: S and L are 0-100 (percent)
            *
            * @param r
     *            Red value.
            * @param g
     *            Green value.
            * @param b
     *            Blue value.
            * @return Integer array representing an HSL pixel.
            */
    public static int[] convertToHSL(int r, int g, int b) {
        float red = r / 255;
        float green = g / 255;
        float blue = b / 255;

        float minComponent = Math.min(red, Math.min(green, blue));
        float maxComponent = Math.max(red, Math.max(green, blue));
        float range = maxComponent - minComponent;
        float h = 0, s = 0, l = 0;

        l = (maxComponent + minComponent) / 2;

        if (range == 0) { // Monochrome image
            h = s = 0;
        } else {
            s = (l > 0.5) ? range / (2 - range) : range / (maxComponent + minComponent);

            if (red == maxComponent) {
                h = (blue - green) / range;
            } else if (green == maxComponent) {
                h = 2 + (blue - red) / range;
            } else if (blue == maxComponent) {
                h = 4 + (red - green) / range;
            }
        }

        // convert to 0-360 (degrees)
        h *= 60;
        if (h < 0) h += 360;

        // convert to 0-100 (percent)
        s *= 100;
        l *= 100;

        // Since they were converted from float to int
        return (new int[] { (int) h, (int) s, (int) l });
    }

    /**
     * Decode a YUV420SP image to RGB.
     *
     * @param yuv420sp
     *            Byte array representing a YUV420SP image.
     * @param width
     *            Width of the image.
     * @param height
     *            Height of the image.
     * @return Integer array representing the RGB image.
     * @throws NullPointerException
     *             if yuv420sp byte array is NULL.
     */
    public static int[] decodeYUV420SPtoRGB(byte[] yuv420sp, int width, int height) {
        if (yuv420sp == null) throw new NullPointerException();

        final int frameSize = width * height;
        int[] rgb = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & (yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    /**
     * Convert an RGB image into a Bitmap.
     *
     * @param rgb
     *            Integer array representing an RGB image.
     * @param width
     *            Width of the image.
     * @param height
     *            Height of the image.
     * @return Bitmap of the RGB image.
     * @throws NullPointerException
     *             if RGB integer array is NULL.
     */
    public static Bitmap rgbToBitmap(int[] rgb, int width, int height) {
        if (rgb == null) throw new NullPointerException();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
        return bitmap;
    }


    public int[] getPrevious() {
        return ((mPrevious != null) ? mPrevious.clone() : null);
    }

    protected static boolean imagesAreDifferent(int[] first, int width, int height) {
        if (first == null) throw new NullPointerException();

        if (mPrevious == null) return false;
        if (first.length != mPrevious.length) return true;
        if (mPreviousWidth != width || mPreviousHeight != height) return true;

        int totDifferentPixels = 0;
        for (int i = 0, ij = 0; i < height; i++) {
            for (int j = 0; j < width; j++, ij++) {
                int pix = (0xff & (first[ij]));
                int otherPix = (0xff & (mPrevious[ij]));

                // Catch any pixels that are out of range
                if (pix < 0) pix = 0;
                if (pix > 255) pix = 255;
                if (otherPix < 0) otherPix = 0;
                if (otherPix > 255) otherPix = 255;

                if (Math.abs(pix - otherPix) >= mPixelThreshold) {
                    totDifferentPixels++;
                    // Paint different pixel red
                    first[ij] = Color.RED;
                }
            }
        }
        if (totDifferentPixels <= 0) totDifferentPixels = 1;
        boolean different = totDifferentPixels > mThreshold;
        /*
         * int size = height * width; int percent =
         * 100/(size/totDifferentPixels); String output =
         * "Number of different pixels: " + totDifferentPixels + "> " + percent
         * + "%"; if (different) { Log.e(TAG, output); } else { Log.d(TAG,
         * output); }
         */
        return different;
    }

    public boolean detect(int[] rgb, int width, int height) {
        if (rgb == null) throw new NullPointerException();

        int[] original = rgb.clone();

        // Create the "mPrevious" picture, the one that will be used to check
        // the next frame against.
        if (mPrevious == null) {
            mPrevious = original;
            mPreviousWidth = width;
            mPreviousHeight = height;
            // Log.i(TAG, "Creating background image");
            return false;
        }

        // long bDetection = System.currentTimeMillis();
        boolean motionDetected = imagesAreDifferent(rgb, width, height);
        // long aDetection = System.currentTimeMillis();
        // Log.d(TAG, "Detection "+(aDetection-bDetection));

        // Replace the current image with the previous.
        mPrevious = original;
        mPreviousWidth = width;
        mPreviousHeight = height;

        return motionDetected;
    }
}
