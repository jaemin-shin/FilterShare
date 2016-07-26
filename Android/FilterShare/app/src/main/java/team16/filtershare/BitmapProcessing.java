package team16.filtershare;

import java.io.IOException;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

public class BitmapProcessing {

    public static Bitmap applyEffects(Bitmap origBitmap, int brightness, int contrast, int saturation, int sharpen, int temperature, int tint, int vignette, int grain) {
        int width = origBitmap.getWidth();
        int height = origBitmap.getHeight();
        int[] pix = new int[width * height];
        origBitmap.getPixels(pix, 0, width, 0, 0, width, height);
        for (int index = 0; index < width * height; index++) {
            pix[index] = BitmapProcessing.brightness(pix[index], brightness);
            pix[index] = BitmapProcessing.contrast(pix[index], contrast);
            pix[index] = BitmapProcessing.fade(pix[index], sharpen);
            pix[index] = BitmapProcessing.temperature(pix[index], temperature);
            pix[index] = BitmapProcessing.tint(pix[index], tint);
            pix[index] = BitmapProcessing.grain(pix[index], grain);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height,origBitmap.getConfig());
        bitmap.setPixels(pix, 0, width, 0, 0, width, height);
        pix = null;
        bitmap = BitmapProcessing.saturation(bitmap, saturation);
        // bitmap = BitmapProcessing.sharpen(bitmap,sharpen);
        bitmap = BitmapProcessing.vignette(bitmap, vignette);
        return bitmap;
    }

    /**
     * Returns a value that is one of val (if it's between min or max) or min or max (if it's outside that range).
     * @param val
     * @param min
     * @param max
     * @return constrained value
     */
    private static double constrain(double val, double min, double max) {
        if (val < min) {
            return min;
        }
        else if (val > max) {
            return max;
        }
        return val;
    }

    // [-255, +255] -> Default = 0
    public static int brightness(int pixel, int value) {
        int A, R, G, B;
        // get pixel color
        A = Color.alpha(pixel);
        R = Color.red(pixel);
        G = Color.green(pixel);
        B = Color.blue(pixel);

        // increase/decrease each channel
        R += value;
        if (R > 255) {
            R = 255;
        } else if (R < 0) {
            R = 0;
        }

        G += value;
        if (G > 255) {
            G = 255;
        } else if (G < 0) {
            G = 0;
        }

        B += value;
        if (B > 255) {
            B = 255;
        } else if (B < 0) {
            B = 0;
        }
        return Color.argb(A, R, G, B);
    }

    // [-100, +100] -> Default = 0
    public static int contrast(int pixel, double value) {
        int A, R, G, B;
        // get contrast value
        double contrast = Math.pow((100 + value) / 100, 2);

        A = Color.alpha(pixel);
        // apply filter contrast for every channel R, G, B
        R = Color.red(pixel);
        R = (int) (((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
        if (R < 0) {
            R = 0;
        } else if (R > 255) {
            R = 255;
        }

        G = Color.green(pixel);
        G = (int) (((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
        if (G < 0) {
            G = 0;
        } else if (G > 255) {
            G = 255;
        }

        B = Color.blue(pixel);
        B = (int) (((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
        if (B < 0) {
            B = 0;
        } else if (B > 255) {
            B = 255;
        }

        return Color.argb(A, R, G, B);
    }

    // [0, 200] -> Default = 100
    public static Bitmap saturation(Bitmap src, int value) {
        float f_value = (float) (value / 100.0);

        int w = src.getWidth();
        int h = src.getHeight();

        Bitmap bitmapResult =
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvasResult = new Canvas(bitmapResult);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(f_value);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvasResult.drawBitmap(src, 0, 0, paint);

        // src.recycle();
        src = null;

        return bitmapResult;
    }

	public static int fade(int pixel, int value) {
        int a = pixel & 0xff000000;
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        r = (int) (r + (127.5 - 0.5 * r) / 100 * value); // (r+255)/2;
        g = (int) (g + (127.5 - 0.5 * g) / 100 * value); // (g+255)/2;
        b = (int) (b + (127.5 - 0.5 * b) / 100 * value); // (b+255)/2;
        return a | (r << 16) | (g << 8) | b;
	}

    public static int temperature(int pixel, int value) {
        int A, R, G, B;
        // get pixel color
        A = Color.alpha(pixel);
        R = Color.red(pixel);
        G = Color.green(pixel);
        B = Color.blue(pixel);

        // increase/decrease each channel
        R = R + (value - 50);
        if (R > 255) {
            R = 255;
        } else if (R < 0) {
            R = 0;
        }

        G = G - ((value - 50) / 2);
        if (G > 255) {
            G = 255;
        } else if (G < 0) {
            G = 0;
        }

        B = B - (value - 50);
        if (B > 255) {
            B = 255;
        } else if (B < 0) {
            B = 0;
        }

        return Color.argb(A, R, G, B);
    }

    public static int tint(int pixel, int degree) {
        double PI = 3.14159d;
        double FULL_CIRCLE_DEGREE = 360d;
        double HALF_CIRCLE_DEGREE = 180d;
        double RANGE = 256d;

        int RY, GY, BY, RYY, GYY, BYY, R, G, B, Y;
        double angle = (PI * (double)degree) / HALF_CIRCLE_DEGREE;

        int S = (int)(RANGE * Math.sin(angle));
        int C = (int)(RANGE * Math.cos(angle));

        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        RY = (70 * r - 59 * g - 11 * b) / 100;
        GY = (-30 * r + 41 * g - 11 * b) / 100;
        BY = (-30 * r - 59 * g + 89 * b) / 100;
        Y = (30 * r + 59 * g + 11 * b) / 100;
        RYY = (S * BY + C * RY) / 256;
        BYY = (C * BY - S * RY) / 256;
        GYY = (-51 * RYY - 19 * BYY) / 100;
        R = Y + RYY;
        R = (R < 0) ? 0 : ((R > 255) ? 255 : R);
        G = Y + GYY;
        G = (G < 0) ? 0 : ((G > 255) ? 255 : G);
        B = Y + BYY;
        B = (B < 0) ? 0 : ((B > 255) ? 255 : B);
        return 0xff000000 | (R << 16) | (G << 8) | B;
    }

    public static Bitmap vignette(Bitmap image, int value) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        float radius = width / ((value / 100.0f) + 0.01f);
        int[] colors = new int[] { 0, 0x55000000, 0xff000000 };
        float[] positions = new float[] { 0.0f, 0.5f, 1.0f };

        //RadialGradient gradient = new RadialGradient(width / 2, height / 2, radius, colors, positions, Shader.TileMode.CLAMP);

        RadialGradient gradient = new RadialGradient(width / 2, height / 2, radius, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);

        Canvas canvas = new Canvas(image);
        canvas.drawARGB(1, 0, 0, 0);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setShader(gradient);

        final Rect rect = new Rect(0, 0, image.getWidth(), image.getHeight());
        final RectF rectf = new RectF(rect);

        canvas.drawRect(rectf, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(image, rect, rect, paint);

        return image;
    }

	public static int grain(int pixel, int scale) {
		final int COLOR_MAX = 0xFF;
		// a random object
		Random random = new Random();

        int color = pixel;
        int red = (int) (constrain(Color.red(color) + scale * (2 * Math.random() - 1), 0, 255));
        int green = (int) (constrain(Color.green(color) + scale * (2 * Math.random() - 1), 0, 255));
        int blue = (int) (constrain(Color.blue(color) + scale * (2 * Math.random() - 1), 0, 255));
        // Put new color
        int randColor = Color.rgb(red, green, blue);

        pixel |= randColor;
        return pixel;

	}
}
