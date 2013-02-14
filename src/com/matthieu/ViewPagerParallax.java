package com.matthieu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;

public class ViewPagerParallax extends ViewPager {
	private int background_id =-1;
    private int background_saved_id =-1;
    private int saved_width=-1, saved_height=-1, saved_max_num_pages =-1;
    private Bitmap saved_bitmap;
    
    private int current_position=-1;
    private float current_offset=0.0f;
    private Rect src = new Rect(), dst = new Rect();

    private int max_num_pages=0;
    private int imageHeight;
    private int imageWidth;
    private float zoom_level;

    private float overlap_level;
    private boolean pagingEnabled = true;
    private boolean parallaxEnabled = true;

    private final static String TAG="ViewPagerParallax";

    public ViewPagerParallax(Context context) {
        super(context);
    }

    public ViewPagerParallax(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint({"NewApi"})
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
            
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int bitmap_size = imageHeight * imageWidth * 4 / 1024;
            if (bitmap_size > maxMemory / 2)
            	return;

            zoom_level = ((float) imageHeight) / getHeight();  // we are always in 'fitY' mode
            is.reset();

            overlap_level = zoom_level * Math.min(Math.max(imageWidth / zoom_level - getWidth(),0) / (max_num_pages - 1), getWidth()/2); // how many pixels to shift for each panel
            saved_bitmap = BitmapFactory.decodeStream(is);

            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot decode: " + e.getMessage());
            background_id = -1;
            return;
        }

        saved_height = getHeight();
        saved_width = getWidth();
        background_saved_id = background_id;
        saved_max_num_pages = max_num_pages;
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        current_position = position;
        current_offset = offset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isParallaxEnabled() && saved_bitmap != null) {
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
}
