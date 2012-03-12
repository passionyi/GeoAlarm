package edu.illinois.geoalarm;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * A MapActivity class that will be responsible for displaying the transit map.
 * 
 * @author deflume1
 * 
 */

public class RouteMap extends MapActivity {
	private static final int INITIAL_ZOOM = 15;
	protected static final int LAUNCH_ACTIVITY = 1;

	private MapView mainMap;
	private MapController mapControl;
	private Button backBtn;
	private CheckBox satellite;
	private Location currentLocation;
	private GeoPoint centerPoint;
	private List<Overlay> currentMarkerOverlays;
	private List<Overlay> nearStopsOverlays;
	private ArrayList<StopInfo> nearStops;
	private Location mapCenter;
	private GeoAlarmDB dbController;

	/** Called when the activity is first created. 
	 * @author Hyung Joo Kim and Seung Mok Lee
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.map);
        
        dbController = new GeoAlarmDB(this);
        try {
        	dbController.openDataBase();
        } catch (SQLException e) {
        			throw e;
        }
        		
        mainMap = (MapView)findViewById(R.id.mainMap);
        backBtn = (Button)findViewById(R.id.backBtn);
        satellite = (CheckBox)findViewById(R.id.satellite);

        showCurrentLocation();
        
        setupGoogleMap();
        
        showNearBusStopsOnMap(currentLocation);
        
        drawPath(new GeoPoint((int)(40.11024833*1E6), (int)(-88.22789764*1E6)), 
        		new GeoPoint((int)(40.10148621*1E6), (int)(-88.236055*1E6)));
        
        backBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(RouteMap.this, GeoAlarm.class);
				startActivityForResult(intent, LAUNCH_ACTIVITY);
			}
		});
        
        satellite.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(satellite.isChecked()){
					mainMap.setSatellite(true);
				}
				else
					mainMap.setSatellite(false);
			}
		});
    }

	private void setupGoogleMap() {
		mapControl = mainMap.getController();
        mainMap.setBuiltInZoomControls(true);

        mapControl.animateTo(centerPoint);
        mapControl.setZoom(INITIAL_ZOOM);
	}

	/**
	 * Method to show current location on the map
	 * @author Hyung Joo Kim and Seung Mok Lee
	 */
	private void showCurrentLocation() {
		setCurrentPoint();
		
		showMarkerOnMap();
	}
	
	private void setCurrentPoint() {
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.NO_REQUIREMENT);
		criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		
		currentLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));

		double latitude = currentLocation.getLatitude();   
		double longitude = currentLocation.getLongitude();
	  
		centerPoint = new GeoPoint((int)(latitude*1E6), (int)(longitude*1E6));
	}

	private void showMarkerOnMap() {
		currentMarkerOverlays = mainMap.getOverlays(); 
	    Drawable drawable = this.getResources().getDrawable(R.drawable.current);        
	    
	    CurrMarkerOverlay itemizedOverlay = new CurrMarkerOverlay(drawable, this);
        OverlayItem overlayitem = new OverlayItem(centerPoint, "", "");
        
        itemizedOverlay.addOverlay(overlayitem);  
        currentMarkerOverlays.add(itemizedOverlay);
	}

    /**
     * Helper function to show the bus stops near the current location on the map
     * @param currentLocation 
     * @author Hyung Joo Kim and Seung Mok Lee
     */
	private void showNearBusStopsOnMap(Location currentLocation) {
		nearStopsOverlays = mainMap.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.near);
		
		nearStops = dbController.getAroundMe(currentLocation);
		NearStopOverlay itemizedOverlay = new NearStopOverlay(drawable, this);
		
		if(!nearStops.isEmpty()){
			for(StopInfo stopToShow : nearStops){
				
				NearStopOverlayItem item = new NearStopOverlayItem(stopToShow);
				itemizedOverlay.addOverlay(item);
			}
			
			nearStopsOverlays.add(itemizedOverlay);
		}
		else {
			Toast.makeText(RouteMap.this, "No near bus stop", Toast.LENGTH_SHORT).show();
			onResume();
		}
	}
	
    /**
     * Called when the user move the center of the map
     * @author Hyung Joo Kim and Seung Mok Lee
     */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		boolean result = super.dispatchTouchEvent(event);
		if (event.getAction() == MotionEvent.ACTION_UP){
			GeoPoint center = mainMap.getMapCenter();
			mapCenter = new Location("");
			mapCenter.setLatitude((double)center.getLatitudeE6()/(double)1E6);
			mapCenter.setLongitude((double)center.getLongitudeE6()/(double)1E6);

			nearStopsOverlays.clear();
			
			showCurrentLocation();
			showNearBusStopsOnMap(mapCenter);
		}

		return result;
	}

	private void drawPath(GeoPoint src, GeoPoint dest) 
	{ 
		// connect to map web service 
		StringBuilder urlString = new StringBuilder(); 
		urlString.append("http://maps.google.com/maps?f=d&hl=en"); 
		urlString.append("&saddr=");//from 
		urlString.append( Double.toString((double)src.getLatitudeE6()/1.0E6 )); 
		urlString.append(","); 
		urlString.append( Double.toString((double)src.getLongitudeE6()/1.0E6 )); 
		urlString.append("&daddr=");//to 
		urlString.append( Double.toString((double)dest.getLatitudeE6()/1.0E6 )); 
		urlString.append(","); 
		urlString.append( Double.toString((double)dest.getLongitudeE6()/1.0E6 )); 
		urlString.append("&ie=UTF8&0&om=0&output=kml"); 
		Log.d("xxx","URL="+urlString.toString()); 
		
		// get the kml (XML) doc. And parse it to get the coordinates(direction route). 
		Document doc = null; 
		HttpURLConnection urlConnection= null; 
		URL url = null; 
		try 
		{ 
			url = new URL(urlString.toString()); 
			urlConnection=(HttpURLConnection)url.openConnection(); 
			urlConnection.setRequestMethod("GET"); 
			urlConnection.setDoOutput(true); 
			urlConnection.setDoInput(true); 
			urlConnection.connect(); 

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
			DocumentBuilder db = dbf.newDocumentBuilder(); 
			doc = db.parse(urlConnection.getInputStream()); 

			if(doc.getElementsByTagName("GeometryCollection").getLength()>0) 
			{ 
				String path = doc.getElementsByTagName("GeometryCollection").item(0).getFirstChild().getFirstChild().getFirstChild().getNodeValue() ; 
				Log.d("xxx","path="+ path); 
				String [] pairs = path.split(" "); 
				String[] lngLat = pairs[0].split(","); // lngLat[0]=longitude lngLat[1]=latitude lngLat[2]=height 
				
				// src 
				GeoPoint startGP = new GeoPoint((int)(Double.parseDouble(lngLat[1])*1E6),(int)(Double.parseDouble(lngLat[0])*1E6)); 
				mainMap.getOverlays().add(new DirectionPathOverlay(startGP,startGP)); 
				
				GeoPoint gp1; 
				GeoPoint gp2 = startGP; 
				for(int i=1;i<pairs.length;i++) // the last one would be crash 
				{ 
					lngLat = pairs[i].split(","); 
					gp1 = gp2; 
					// watch out! For GeoPoint, first:latitude, second:longitude 
					gp2 = new GeoPoint((int)(Double.parseDouble(lngLat[1])*1E6),(int)(Double.parseDouble(lngLat[0])*1E6)); 
					mainMap.getOverlays().add(new DirectionPathOverlay(gp1,gp2)); 
					Log.d("xxx","pair:" + pairs[i]); 
				}
				
				mainMap.getOverlays().add(new DirectionPathOverlay(dest,dest)); // use the default color 
			} 
		} catch (Exception e) {
			// TODO: handle exception
			
			
		}
	}
		
	/**
	 * This method returns whether routes are currently being displayed on the
	 * map. Right now, they're not.
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}