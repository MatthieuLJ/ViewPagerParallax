package com.matthieu;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MyTestActivity extends Activity {
    private static final int MAX_PAGES = 10;

    private int num_pages = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ViewPagerParallax pager = (ViewPagerParallax) findViewById(R.id.pager);
        pager.set_max_pages(MAX_PAGES);
        pager.setBackgroundAsset(R.raw.sanfran);
        pager.setAdapter(new my_adapter());

        final Button add_page_button = (Button) findViewById(R.id.add_page_button);
        add_page_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                num_pages = Math.min(num_pages+1, MAX_PAGES);
                pager.getAdapter().notifyDataSetChanged();
            }
        });

        if (savedInstanceState!=null) {
            num_pages = savedInstanceState.getInt("num_pages");
            pager.setCurrentItem(savedInstanceState.getInt("current_page"), false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("num_pages", num_pages);
        final ViewPagerParallax pager = (ViewPagerParallax) findViewById(R.id.pager);
        outState.putInt("current_page", pager.getCurrentItem());
    }

    private class my_adapter extends PagerAdapter {
        @Override
        public int getCount() {
            return num_pages;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View new_view=null;

            LayoutInflater inflater = getLayoutInflater();
            new_view = inflater.inflate(R.layout.page, null);
            TextView num = (TextView) new_view.findViewById(R.id.page_number);
            num.setText(Integer.toString(position));

            container.addView(new_view);

            return new_view;
        }

    }
}

