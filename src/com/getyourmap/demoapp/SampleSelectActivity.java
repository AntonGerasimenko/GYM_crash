package com.getyourmap.demoapp;


import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.getyourmap.demoapp.MapViewActivity;
import com.getyourmap.demoappjar.R;
import com.glmapview.GLMapManager;
import com.glmapview.GLMapView;

public class SampleSelectActivity extends ListActivity 
{
    public enum Samples {
        MAP,
        MAP_EMBEDD,
        MAP_ONLINE,
		MAP_ONLINE_RASTER,
        ZOOM_BBOX,
        IMAGE_SINGLE,
        IMAGE_MULTI,
        MULTILINE,
        POLYGON,
        GEO_JSON,
		CALLBACK_TEST,
        CAPTURE_SCREEN,
        DOWNLOAD_MAP,
        SVG_TEST,
        CRASH_NDK,
    };	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.sample_select);
        		
		if(!GLMapManager.initialize(this, "your apikey", null))
		{
			//Error caching resources. Check free space for world database (~25MB)			
		}
        
        String[] values = new String[] {
        		"Open offline map",
        		"Open embedd map", 
        		"Open online map",
				"Open online raster map",
        		"Zoom to bbox",
        		"Display single image",
        		"Display image group",
        		"Add multiline",
        		"Add polygon",
        		"Load GeoJSON",
				"Callback test",
        		"Capture screen",
        		"Download Map",
        		"SVG Test",
        		"Crash NDK",
        		};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {   
    	if(position == Samples.DOWNLOAD_MAP.ordinal())
    	{
        	Intent i = new Intent(SampleSelectActivity.this, DownloadActivity.class);
        	i.putExtra("cx", 27.0);
        	i.putExtra("cy", 53.0);
        	this.startActivity(i);    	
			return;
    	}
    	
    	if(position == Samples.SVG_TEST.ordinal())
    	{
        	Intent intent = new Intent(SampleSelectActivity.this, DisplayImageActivity.class);
        	this.startActivity(intent);
        	return;
    	}
    	if(position == Samples.CRASH_NDK.ordinal())
    	{
    		GLMapView.crashNDK2();
        	return;
    	}    	
    	
    	Intent intent = new Intent(SampleSelectActivity.this, MapViewActivity.class);
    	Bundle b = new Bundle();
    	b.putInt("example", position);
    	intent.putExtras(b);
    	this.startActivity(intent);
    }    
}
