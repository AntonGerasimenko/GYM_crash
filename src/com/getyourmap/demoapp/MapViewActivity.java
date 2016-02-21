package com.getyourmap.demoapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.Button;

import com.getyourmap.demoapp.crash.LakeModel;
import com.getyourmap.demoappjar.R;
import com.glmapview.FieldListener;
import com.glmapview.GLMapDownloadTask;
import com.glmapview.GLMapImage;
import com.glmapview.GLMapImageGroup;
import com.glmapview.GLMapImageGroupCallback;
import com.glmapview.GLMapInfo;
import com.glmapview.GLMapManager;
import com.glmapview.GLMapVectorObject;
import com.glmapview.GLMapView;
import com.glmapview.GLMapView.GLAlignment;
import com.glmapview.GLMapView.GLMapAttibutionPosition;
import com.glmapview.GLMapView.GLUnits;
import com.glmapview.PointD;
import com.glmapview.GLMapVectorStyle;
import com.glmapview.ScreenCaptureCallback;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MapViewActivity extends Activity implements ScreenCaptureCallback, FieldListener {
	class Pin
	{
		public PointD pos;
		public int imageVariant;		
	}

	private GLMapImage image=null;
	private GLMapImageGroup imageGroup=null;
	private List<Pin> pins = new ArrayList<>();	
	private GestureDetector gestureDetector;
	private GLMapView mapView;
	private GLMapInfo mapToDownload=null;
	private Button btnDownloadMap;


	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		mapView = (GLMapView) this.findViewById(R.id.map_view);

		// Map list is updated, because download button depends on available map list and during first launch this list is empty
		GLMapManager.updateMapList(null);

		btnDownloadMap = (Button) this.findViewById(R.id.button_dl_map);
		btnDownloadMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mapToDownload != null) {
					GLMapDownloadTask task = GLMapManager.getDownloadTask(mapToDownload);
					if (task != null) {
						task.cancel();
					} else {
						task = GLMapManager.createDownloadTask(mapToDownload);
						task.addListener(MapViewActivity.this);
						task.start();
					}
					updateMapDownloadButtonText();
				}
			}
		});

		for (GLMapDownloadTask task : GLMapManager.getMapDownloadTasks()) {
			task.addListener(this);
		}

		mapView.setAssetManager(getAssets());
		mapView.loadStyle("DefaultStyle.bundle");
		mapView.setUserLocationImages(
				mapView.imageManager.open("DefaultStyle.bundle/circle-new.svgpb", 1, 0xFFFFFFFF),
				mapView.imageManager.open("DefaultStyle.bundle/arrow-new.svgpb", 1, 0xFFFFFFFF));

		mapView.setShowsUserLocation(true);

		mapView.setScaleRulerStyle(GLUnits.SI, GLAlignment.Center, new PointD(0.5, 0.99), 0.5);
		mapView.setAttributionPosition(GLMapAttibutionPosition.TopCenter);

		Bundle b = getIntent().getExtras();
		final int example = b.getInt("example");

		if (example == SampleSelectActivity.Samples.MAP_EMBEDD.ordinal()) {
			if (!GLMapManager.AddCustomMap(getAssets(), "Montenegro.vm", null)) {
				//Failed to unpack to caches. Check free space.
			}
			zoomToPoint();
		} else if (example == SampleSelectActivity.Samples.MAP_ONLINE.ordinal()) {
			GLMapManager.SetAllowedTileDownload(true);
		} else if (example == SampleSelectActivity.Samples.MAP_ONLINE_RASTER.ordinal()) {
			mapView.setRasterTileSource( new OSMTileSource(this) );
		}else if (example == SampleSelectActivity.Samples.MULTILINE.ordinal()) {
			addMultiline();
		} else if (example == SampleSelectActivity.Samples.POLYGON.ordinal()) {
			addPolygon();
		} else if (example == SampleSelectActivity.Samples.CAPTURE_SCREEN.ordinal()) {
			zoomToPoint();
			captureScreen();
		} else if (example == SampleSelectActivity.Samples.IMAGE_SINGLE.ordinal()) {
			final Button btn = (Button) this.findViewById(R.id.button_img_action);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					btn.setText("Add image");
					addImage();
				}
			});
		} else if (example == SampleSelectActivity.Samples.IMAGE_MULTI.ordinal()) {
			mapView.setLongClickable(true);

			gestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					deletePin(e.getX(), e.getY());
					return true;
				}

				@Override
				public void onLongPress(MotionEvent e) {
					addPin(e.getX(), e.getY());
				}
			});

			mapView.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View arg0, MotionEvent ev) {
					return gestureDetector.onTouchEvent(ev);
				}
			});

		} else if (example == SampleSelectActivity.Samples.ZOOM_BBOX.ordinal()) {
			zoomToBBox();
		} else if (example == SampleSelectActivity.Samples.GEO_JSON.ordinal()) {
			loadGeoJSON();
		}

		mapView.setCenterTileStateChangedCallback(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateMapDownloadButton();
					}
				});
			}
		});
		mapView.setMapDidMoveCallback(new Runnable() {
			@Override
			public void run() {

				if (example == SampleSelectActivity.Samples.CALLBACK_TEST.ordinal()) {
					Log.w("GLMapView", "Did move");
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateMapDownloadButtonText();
					}
				});
			}
		});

		getAssetsPoint();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	PointD pt = new PointD(mapView.getWidth()/2, mapView.getHeight()/2);
		mapView.changeMapZoom(-1, pt, true);
	    return false;
	}

	@Override
	public void fieldValueChanged(Object obj, String fieldName, Object newValue)
	{
		if(fieldName.equals("finished") && newValue.equals(1))
		{
			mapView.reloadTiles();
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateMapDownloadButtonText();
			}
		});

	}

	void updateMapDownloadButtonText()
	{
		if(btnDownloadMap.getVisibility()==View.VISIBLE)
		{
			PointD center = mapView.getMapCenter(new PointD());

			if(mapToDownload==null || GLMapManager.DistanceToMap(mapToDownload, center)>0)
			{
				mapToDownload = GLMapManager.FindNearestMap(GLMapManager.getChildMaps(), center);
			}

			if (mapToDownload != null)
			{
				String text;
				GLMapDownloadTask task = GLMapManager.getDownloadTask(mapToDownload);
				if(task != null )
				{
					text = String.format("Downloading %s %d%%", mapToDownload.getName(), (int)(task.progressDownload*100));
				}else
				{
					text = String.format("Download %s", mapToDownload.getName());
				}
				btnDownloadMap.setText(text);
			}
		}
	}

	void updateMapDownloadButton()
	{
		switch (mapView.getCenterTileState())
		{
			case NoData:
			{
				if(btnDownloadMap.getVisibility()==View.INVISIBLE)
				{
					btnDownloadMap.setVisibility(View.VISIBLE);
					btnDownloadMap.getParent().requestLayout();
					updateMapDownloadButtonText();
				}
				break;
			}

			case Loaded:
			{
				if(btnDownloadMap.getVisibility()==View.VISIBLE)
				{
					btnDownloadMap.setVisibility(View.INVISIBLE);
				}
				break;
			}
			case Unknown:
				break;
		}
	}

	// Example how to calcludate zoom level for some bbox
	void zoomToBBox()
	{
		mapView.doWhenSurfaceCreated(new Runnable(){
			@Override
			public void run() 
			{
				// get internal coordinates of geo points
			/*	PointD pt1 = GLMapView.convertGeoToInternal(new PointD(13.4102, 52.5037)); // Berlin
				PointD pt2 = GLMapView.convertGeoToInternal(new PointD(27.5618, 53.9024)); // Minsk
*/
				PointD pt1 = GLMapView.convertGeoToInternal(new PointD(23.164568, 52.286482));
				PointD pt2 = GLMapView.convertGeoToInternal(new PointD(32.777307, 53.393074));

				
				PointD screenPt1 = mapView.convertInternalToDisplay(new PointD(pt1)); // get pixel positions of geo points
				PointD screenPt2 = mapView.convertInternalToDisplay(new PointD(pt2));
				
				double screenDx = Math.abs(screenPt1.getX() - screenPt2.getX()); // get distance in pixels in current zoom level
				double screenDy = Math.abs(screenPt1.getY() - screenPt2.getY());
				
			    // get scale beteen current screen size and desired screen size to fit points
			    double wscale = screenDx / mapView.getWidth();
			    double hscale = screenDy / mapView.getHeight();
			    double scaleChange = 1.0/Math.max(wscale, hscale);
			    
			    // set center point
			    PointD center = new PointD((pt1.getX()+pt2.getX())/2, (pt1.getY()+pt2.getY())/2);
			    mapView.setMapCenter(center, false); 
			    			    
			    // change zoom to make screenDistance less or equal mapView.bounds
			    mapView.scaleMap(scaleChange, null, false);
			}
		});				
	}
	    
    void zoomToPoint()
    {
    	//New York
    	//PointD pt = new PointD(-74.0059700 , 40.7142700	);
    	
    	//Belarus
    	//PointD pt = new PointD(27.56, 53.9);
    	//;

		// Move map to the Montenegro capital
		PointD pt = GLMapView.convertGeoToInternal(new PointD(19.26, 42.4341));
    	GLMapView mapView = (GLMapView) this.findViewById(R.id.map_view);
    	mapView.setMapCenter(pt, false);
    	mapView.setMapZoom(16, false);
    }
          
    void addPin(float touchX, float touchY)
    {
    	if(imageGroup == null)
    	{
    		final Bitmap images[] = new Bitmap[3];    		
    		images[0] = mapView.imageManager.open("1.svgpb", 1, 0xFFFF0000);
    		images[1] = mapView.imageManager.open("2.svgpb", 1, 0xFF00FF00);
    		images[2] = mapView.imageManager.open("3.svgpb", 1, 0xFF0000FF);
    		
        	class Callback implements GLMapImageGroupCallback {
        		@Override
        		public int getImagesCount() 
        		{
        			return pins.size();
        		}

        		@Override
        		public int getImageIndex(int i) 
        		{
        			return pins.get(i).imageVariant;
        		}

        		@Override
        		public PointD getImagePos(int i) 
        		{
        			return pins.get(i).pos;
        		}

				@Override
				public void updateStarted()
				{
					Log.i("GLMapImageGroupCallback", "Update started");
				}

				@Override
				public void updateFinished()
				{
					Log.i("GLMapImageGroupCallback", "Update finished");
				}

				@Override
        		public int getImageVariantsCount()
        		{
        			return images.length;
        		}
        		
        		@Override        		
        		public Bitmap getImageVariantBitmap(int i)
        		{
        			return images[i];
        		}
        		@Override        		
        		public PointD getImageVariantOffset(int i)
        		{
        			return new PointD(images[i].getWidth()/2, 0);
        		}
            }          	    		
    		imageGroup = mapView.createImageGroup(new Callback());    		
    	}    	
    	
    	PointD pt = mapView.convertDisplayToInternal(new PointD(touchX, touchY));
    	
    	Pin pin = new Pin();
    	pin.pos = pt;
    	pin.imageVariant = pins.size() % 3;    	
    	pins.add(pin);
    	imageGroup.setNeedsUpdate();    	   	
    }
    
    
    void deletePin(float touchX, float touchY)
    {
    	for(int i=0; i<pins.size(); ++i)
    	{    	    	
    		PointD pos = pins.get(i).pos;
    		PointD screenPos = mapView.convertInternalToDisplay(new PointD(pos)); 
    		
    		Rect rt = new Rect(-40,-40,40,40);
    		rt.offset( (int)screenPos.getX(), (int)screenPos.getY() );    	
    		if(rt.contains((int)touchX, (int)touchY))
    		{
    			pins.remove(i);
    			imageGroup.setNeedsUpdate();
    			break;	
    		}    		    		    		
    	}
    }     
    
    void addImage()
    {
    	Bitmap bmp = mapView.imageManager.open("arrow-maphint.svgpb", 1, 0xFFFFFFFF);
    	image = mapView.displayImage(bmp);
    	image.setOffset(new PointD(bmp.getWidth(), bmp.getHeight()/2));
    	image.setRotatesWithMap(true);
    	image.setAngle(Math.random()*360);

    	image.setPosition(mapView.getMapCenter( new PointD()));
    	
    	final Button btn = (Button) this.findViewById(R.id.button_img_action);
		btn.setText("Move image");
    	btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				moveImage();
			}
    	});    	
    }
    
    void moveImage()
    {
    	image.setPosition(mapView.getMapCenter(new PointD()));
    	
    	final Button btn = (Button) this.findViewById(R.id.button_img_action);
		btn.setText("Remove image");
    	btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				delImage();
			}
    	});    	
    }
    
    void delImage()
    {
    	mapView.removeImage(image);
    	
    	final Button btn = (Button) this.findViewById(R.id.button_img_action);
		btn.setText("Add image");
    	btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				addImage();
			}
    	});     	
    }
    
    void addMultiline()
    {
        final GLMapVectorObject obj = mapView.createVectorObject();
        PointD[] line1 = new PointD[5];
        line1[0] = new PointD(27.7151, 53.8869); // Minsk   
        line1[1] = new PointD(30.5186, 50.4339); // Kiev
        line1[2] = new PointD(21.0103, 52.2251); // Warsaw
        line1[3] = new PointD(13.4102, 52.5037); // Berlin
        line1[4] = new PointD(2.3343, 48.8505); // Paris
        
        PointD[] line2 = new PointD[3];
        line2[0] = new PointD(4.9021, 52.3690); // Amsterdam  
        line2[1] = new PointD(4.3458, 50.8263); // Brussel
        line2[2] = new PointD(6.1296, 49.6072); // Luxembourg
       
        PointD[][] multiline = {line1, line2};        
        obj.loadMultiline(multiline);
        
        mapView.doWhenSurfaceCreated(new Runnable() {
			@Override
			public void run() {
				// style applied to all lines added. Style is string with mapcss rules. Read more in manual.
				GLMapVectorStyle style = mapView.createStyle("{width: 2pt;color:green;layer:100;}");
				mapView.addVectorObjectWithStyle(obj, style);
			}
		});
     }
    
    void addPolygon()
    {
        final GLMapVectorObject obj = mapView.createVectorObject();
        int pointCount = 25;        
        PointD[] outerRing = new PointD[pointCount];
        PointD[] innerRing = new PointD[pointCount];
        
        float rOuter = 20, rInner = 10;
        float cx = 30, cy = 30;

        // let's display circle
        for (int i=0; i<pointCount; i++) 
        {
        	outerRing[i] = new PointD(cx + Math.sin(2*Math.PI / pointCount * i) * rOuter,
                                      cy + Math.cos(2*Math.PI / pointCount * i) * rOuter);
        	
        	innerRing[i] =  new PointD(cx + Math.sin(2*Math.PI / pointCount * i) * rInner,
                    cy + Math.cos(2*Math.PI / pointCount * i) * rInner);        	
        }
        
        PointD[][] outerRings = {outerRing};
        PointD[][] innerRings = {innerRing};        
        obj.loadPolygon(outerRings, innerRings);       
        
        mapView.doWhenSurfaceCreated(new Runnable(){
			@Override
			public void run() {
		        GLMapVectorStyle style = mapView.createStyle("{fill-color:#10106050;}"); // #RRGGBBAA format
		        mapView.addVectorObjectWithStyle(obj, style);
			}
        });         
    }  
    
	private void loadGeoJSON() 
	{	
		mapView.doWhenSurfaceCreated(new Runnable(){
			@Override
			public void run() {
				GLMapVectorObject []objects = mapView.createVectorObjectsFromGeoJSON(
						"[{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [30.5186, 50.4339]}, \"properties\": {\"id\": \"1\", \"text\": \"test1\"}},"
						+ "{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [27.7151, 53.8869]}, \"properties\": {\"id\": \"2\", \"text\": \"test2\"}},"
						+ "{\"type\":\"LineString\",\"coordinates\": [ [27.7151, 53.8869], [30.5186, 50.4339], [21.0103, 52.2251], [13.4102, 52.5037], [2.3343, 48.8505]]},"
						+ "{\"type\":\"Polygon\",\"coordinates\":[[ [0.0, 10.0], [10.0, 10.0], [10.0, 20.0], [0.0, 20.0] ],[ [2.0, 12.0], [ 8.0, 12.0], [ 8.0, 18.0], [2.0, 18.0] ]]}]");
				
				GLMapVectorStyle style = mapView.createCascadeStyle(
						"node[id=1]{icon-image:\"autobus.svgpb\";icon-scale:0.5;icon-tint:green;text:eval(tag('text'));text-color:red;font-size:12;text-priority:100;}"
						+ "node|z-9[id=2]{icon-image:\"autobus.svgpb\";icon-scale:0.7;icon-tint:blue;text:eval(tag('text'));text-color:red;font-size:12;text-priority:100;}"
						+ "line{linecap: round; width: 5pt; color:blue;}"
						+ "area{fill-color:green; width:1pt; color:red;}");
				
				mapView.addVectorObjectsWithStyle(objects, style);				
			}			
		});
	}		   
    
    void captureScreen()
    {
    	GLMapView mapView = (GLMapView) this.findViewById(R.id.map_view);    	
    	mapView.captureFrameWhenFinish(this);
    }

	@Override
	public void screenCaptured(final Bitmap bmp) 
	{
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.PNG, 100, bytes);
				try {
					FileOutputStream fo = openFileOutput("screenCapture", Context.MODE_PRIVATE);
					fo.write(bytes.toByteArray());
					fo.close();

					File file = new File(getExternalFilesDir(null), "Test.jpg");
					fo = new FileOutputStream(file);
					fo.write(bytes.toByteArray());
					fo.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				Intent intent = new Intent(MapViewActivity.this, DisplayImageActivity.class);
				Bundle b = new Bundle();
				b.putString("imageName", "screenCapture");
				intent.putExtras(b);
				startActivity(intent);
			}
		});
    }

	private void getAssetsPoint() {
		try {

			List<LakeModel> list = parse(getAssets().open("lakes/dictionary.xml"));


			for (LakeModel model:list)
				showPin(model);


		} catch (IOException e) {
			e.printStackTrace();
		}


	}


	public List<LakeModel> parse(InputStream is) {
		XmlPullParserFactory factory = null;
		XmlPullParser parser = null;
		List <LakeModel >mLakesList = new ArrayList<>(); // duplicate result

		LakeModel mLake = null;
		String value = "";
		try {
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			parser = factory.newPullParser();

			parser.setInput(is, null);

			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tagname = parser.getName();
				switch (eventType) {
					case XmlPullParser.START_TAG:
						if (tagname.equalsIgnoreCase("lake")) {
							// create a new instance of employee
							mLake = new LakeModel();
						}
						break;

					case XmlPullParser.TEXT:
						value = parser.getText();
						break;

					case XmlPullParser.END_TAG:
						if (tagname.equalsIgnoreCase("lake")) {
							// add employee object to list
							mLakesList.add(mLake);
						} else if (tagname.equalsIgnoreCase("id")) {
							mLake.setId(Integer.parseInt(value));
						} else if (tagname.equalsIgnoreCase("name")) {
							mLake.setName(value);
						} else if (tagname.equalsIgnoreCase("isFree")) {
							mLake.setIsFree(value.equals("true"));
						} else if (tagname.equalsIgnoreCase("lat")) {
							mLake.setLat(Double.parseDouble(value));
						} else if (tagname.equalsIgnoreCase("lng")) {
							mLake.setLng(Double.parseDouble(value));
						}else if (tagname.equalsIgnoreCase("path")) {
							mLake.setPath(value);
						}
						break;

					default:
						break;
				}
				eventType = parser.next();
			}

			is.close();

			return mLakesList;


		} catch (XmlPullParserException e) {

			throw  new RuntimeException(e);
		} catch (IOException e) {

			throw  new RuntimeException(e);
		}
	}


	public void showPin(LakeModel model) {

		String path = "lakes/sign_map_free.png";
		Bitmap bmp = mapView.imageManager.open(path, 1, 0xFFFFFFFF);
		GLMapImage image = mapView.displayImage(bmp);
		image.setOffset(new PointD(bmp.getWidth() / 2, 0));
		image.setRotatesWithMap(true);
		image.setPosition(GLMapView.convertGeoToInternal(new PointD(model.getLng(), model.getLat())));


	}
}
