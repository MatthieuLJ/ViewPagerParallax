package com.matthieu;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Debug;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class ViewPagerParallax extends ViewPager {
    private int background_id = -1;
    private int background_saved_id = -1;
    private int saved_width = -1;
    private int saved_height = -1;
    private int saved_max_num_pages = -1;
    private Bitmap saved_bitmap;
    private boolean insufficientMemory = false;

    private int max_num_pages=0;
    private int imageHeight;
    private int imageWidth;
    private float zoom_level;
    private float overlap_level;
    private Rect src = new Rect(), dst = new Rect();

    private boolean pagingEnabled = true;
    private boolean parallaxEnabled = true;

    private boolean loggable = true;
    private final static String TAG = "ViewPagerParallax";

    public ViewPagerParallax(Context context) {
        super(context);
    }

    public ViewPagerParallax(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    private int sizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        } else {
            return data.getByteCount();
        }
    }

    private void set_new_background() {
        if (background_id == -1)
            return;

        if (max_num_pages == 0)
            return;

        if (getWidth()==0 || getHeight()==0)
            return;

        if ((saved_height == getHeight()) && (saved_width == getWidth()) &&
                (background_saved_id==background_id) &&
                (saved_max_num_pages == max_num_pages))
            return;

        InputStream is;

        try {
            is = getContext().getResources().openRawResource(background_id);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            imageHeight = options.outHeight;
            imageWidth = options.outWidth;
            if (loggable) Log.v(TAG, "imageHeight=" + imageHeight + ", imageWidth=" + imageWidth);

            zoom_level = ((float) imageHeight) / getHeight();  // we are always in 'fitY' mode

            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.round(zoom_level);

            if (options.inSampleSize > 1) {
                imageHeight = imageHeight / options.inSampleSize;
                imageWidth = imageWidth / options.inSampleSize;
            }
            if (loggable) Log.v(TAG, "imageHeight=" + imageHeight + ", imageWidth=" + imageWidth);

            double max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
            double heapSize = Runtime.getRuntime().totalMemory(); //current heap size
            double heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
            double nativeUsage = Debug.getNativeHeapAllocatedSize();
            double remaining = max - (heapSize - heapRemaining) - nativeUsage;

            int freeMemory = (int)(remaining / 1024);
            int bitmap_size = imageHeight * imageWidth * 4 / 1024;
            if (loggable) Log.v(TAG, "freeMemory = " + freeMemory);
            if (loggable) Log.v(TAG, "calculated bitmap size = " + bitmap_size);
            if (bitmap_size > freeMemory / 5) {
                insufficientMemory = true;
                return; // we aren't going to use more than one fifth of free memory
            }

            zoom_level = ((float) imageHeight) / getHeight();  // we are always in 'fitY' mode
            overlap_level = zoom_level * Math.min(Math.max(imageWidth / zoom_level - getWidth(), 0) / (max_num_pages - 1), getWidth()/2); // how many pixels to shift for each panel

            is.reset();
            saved_bitmap = BitmapFactory.decodeStream(is, null, options);
            if (loggable) Log.i(TAG, "real bitmap size = " + sizeOf(saved_bitmap) / 1024);
            if (loggable) Log.v(TAG, "saved_bitmap.getHeight()=" + saved_bitmap.getHeight() + ", saved_bitmap.getWidth()=" + saved_bitmap.getWidth());

            is.close();
        } catch (IOException e) {
            if (loggable) Log.e(TAG, "Cannot decode: " + e.getMessage());
            background_id = -1;
            return;
        }

        saved_height = getHeight();
        saved_width = getWidth();
        background_saved_id = background_id;
        saved_max_num_pages = max_num_pages;
    }
    int current_position=-1;
    float current_offset=0.0f;

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        current_position = position;
        current_offset = offset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!insufficientMemory && parallaxEnabled) {
            if (current_position == -1)
                current_position=getCurrentItem();
            // maybe we could get the current position from the getScrollX instead?
            src.set((int) (overlap_level * (current_position + current_offset)), 0,
                    (int) (overlap_level * (current_position + current_offset) + (getWidth() * zoom_level)), imageHeight);

            dst.set((int) (getScrollX()), 0,
                    (int) (getScrollX() + canvas.getWidth()), canvas.getHeight());

            canvas.drawBitmap(saved_bitmap, src, dst, null);
        }
    }

    public void set_max_pages(int num_max_pages) {
        max_num_pages = num_max_pages;
        set_new_background();
    }

    public void setBackgroundAsset(int res_id) {
        background_id = res_id;
        set_new_background();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!insufficientMemory && parallaxEnabled)
            set_new_background();
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        current_position = item;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.pagingEnabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	// fix for https://github.com/JakeWharton/Android-ViewPagerIndicator/issues/72
        if(isFakeDragging()) {
        	return false;
        }
        if (this.pagingEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public boolean isPagingEnabled() {
        return pagingEnabled;
    }

    /**
     * Enables or disables paging for this ViewPagerParallax.
     * @param parallaxEnabled
     */
    public void setPagingEnabled(boolean pagingEnabled) {
        this.pagingEnabled = pagingEnabled;
    }

    public boolean isParallaxEnabled() {
        return parallaxEnabled;
    }

    /**
     * Enables or disables parallax effect for this ViewPagerParallax.
     * @param parallaxEnabled
     */
    public void setParallaxEnabled(boolean parallaxEnabled) {
        this.parallaxEnabled = parallaxEnabled;
    }

    protected void onDetachedFromWindow() {
        if (saved_bitmap != null) {
            saved_bitmap.recycle();
            saved_bitmap = null;
        }
        super.onDetachedFromWindow();
    }
}
