package edu.illinois.geoalarm.test;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.test.ActivityInstrumentationTestCase2;
import edu.illinois.geoalarm.*;

import com.jayway.android.robotium.solo.Solo;
import android.test.suitebuilder.annotation.Smoke;

/**
 * Tests signaling an alarm based on a location
 * @author GeoAlarm
 *
 */

public class LocationBasedAlarmTest extends ActivityInstrumentationTestCase2<GeoAlarm> 
{
	Activity mActivity;
	Activity mCurrentActivity;
	Solo solo;
	LocationManager manager;
	
	public LocationBasedAlarmTest()
	{
		super("edu.illinois.geoalarm", GeoAlarm.class);
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		mActivity.finish();
		super.tearDown();
	}
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();		
		mActivity = this.getActivity();
		manager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
		solo = new Solo(getInstrumentation(), getActivity());
	}
	
	/**
	 * Plan a trip
	 */
	@Smoke
	public void testPlanTrip()
	{		
		solo.clickOnImage(0); // Corresponds to plan trip button
		solo.assertCurrentActivity("Expected TripPlannerBus Activity", TripPlanner.class);		
		mCurrentActivity = solo.getCurrentActivity();
		selectGoldLine();
		selectStart();
		selectDestination();
		setAlarmOptionsPopUp();
		setAlarm();
		waitForAlarm();
	}	
	
	/**
	 * Select a line
	 */
	public void selectGoldLine()
	{		
		solo.clickOnEditText(0);
		solo.enterText(0, "Gold");
		solo.sendKey(Solo.ENTER);
		assertTrue("Selected Gold", solo.searchText("Gold")); // make sure Gold was selected
	}	
	
	/**
	 * Select starting location
	 */
	public void selectStart()
	{		
		solo.clickOnEditText(1);
		solo.enterText(1, "First & Gregory (NE Corner)");
		solo.sendKey(Solo.ENTER);
		assertTrue("Selected First & Gregory", solo.searchText("First & Gregory"));
	}	
	
	/**
	 * Select destination location
	 */
	public void selectDestination()
	{		
		solo.clickOnEditText(2);
		solo.enterText(2, "Springfield & Gregory St. (NE Corner)");
		solo.sendKey(Solo.ENTER);		
		solo.goBack();
		solo.goBack();
		assertTrue("Selected Springfield & Gregory", solo.searchText("Springfield & Gregory"));	
	}
	
	/**
	 * Select alarm options
	 */
	public void setAlarmOptionsPopUp()
	{
		solo.clickOnText("Alarm Options");
		thisWait(1000);
		solo.clickInList(0);
		thisWait(2000);
		solo.clickInList(0);
			
		solo.clickOnText("At Stop");		
		solo.clickOnText("PopUp Message");		
	}
	
	/**
	 * Set an alarm
	 */
	public void setAlarm()
	{
		solo.clickOnText("Set Alarm");
		solo.assertCurrentActivity("Expected RouteMap Activity", RouteMap.class);
		mCurrentActivity = solo.getCurrentActivity();			
	}	
	
	/**
	 * Send fake location information to the activity.  Then poll until we see pop-up indicating that
	 * we have arrived
	 */
	public void waitForAlarm()
	{		
		double latitude = 40.11282333;
		double longitude = -88.22055;
		Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);		
		
		try
		{
			manager.addTestProvider(LocationManager.GPS_PROVIDER, true, true, true, true, true, true, true, 0, 5);
			manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
			manager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null,System.currentTimeMillis());
		}
		catch (Exception ex) {}
			
		int count = 0;
		boolean found = false;
		while(count < 100  && !found)
		{
			fakeLocation.setLatitude(latitude);
			fakeLocation.setLongitude(longitude);
			manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fakeLocation);	
			found =solo.waitForText("YOU HAVE ARRIVED", 0, 1000);	
			count++;
			fakeLocation.setLatitude(latitude - 1000);
			fakeLocation.setLongitude(longitude - 1000);
			manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fakeLocation);
		}
		
		assertTrue(found);
		solo.goBackToActivity("GeoAlarm");
	}
	
	/**
	 * Sleeps the thread for milliseconds
	 * @param millis milliseconds to sleep
	 */
	private void thisWait(long millis)
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException e) 
		{			
			e.printStackTrace();
		}
		
	}

}
