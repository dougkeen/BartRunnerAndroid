package com.dougkeen.bart.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.dougkeen.bart.R;
import com.dougkeen.util.Assert;


public class ViewMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar supportActionBar = Assert.notNull(getSupportActionBar());
        supportActionBar.setHomeButtonEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        SubsamplingScaleImageView photoView = new SubsamplingScaleImageView(this);

        setContentView(photoView);

        photoView.setImage(ImageSource.resource(R.drawable.map).dimensions(2279, 2075),
                ImageSource.resource(R.drawable.map_preview));
        photoView.setMinimumDpi(320);
        photoView.setDoubleTapZoomDpi(480);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.system_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
