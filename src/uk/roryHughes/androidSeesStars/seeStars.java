package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class seeStars extends Activity
{
    private LocationManager lm;
    private LocationListener locListener;
    
    private String TLEUrl = "http://celestrak.com/NORAD/elements/stations.txt";
    private String TLEFileName = "isstle.txt";
    /*very basic - will make use of some method of storing url&name of file
     * and loop through all files from celestrack (and possibly others)
	*/
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //show logo
        
        //set up location
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locListener = new MyLocationListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        
        //get TLEs
        TLEDownloader TLEGetter = new TLEDownloader();
        TLEGetter.downloadFromUrl(TLEUrl, TLEFileName, this);
        	//will loop through all tle files @ celestrack, calling this each time
        	//overwrite any existing files, only use old ones if no network connection
        
        
    }
    
    /*public void onResume()
    {
    	//TODO - do something here so it doesnt crash
    }
    
    public void onPause()
    {
    	//TODO - do something here so it doesnt crash
    }
    
    public void onStop()
    {
    	//TODO - do something here so it doesnt crash
    }
    */
    private class MyLocationListener implements LocationListener
    {
    	public void onLocationChanged(Location loc)
    	{
    		if(loc != null)
    		{
				Toast.makeText(getBaseContext(),
					"Location Changed : lat: "+ loc.getLatitude() + " Long: "+ loc.getLongitude(),
					Toast.LENGTH_LONG).show();
    		}
    	}
    	
    	@Override
        public void onProviderDisabled(String provider)
    	{
            // TODO Prompt to enable gps & click to goto settings
    		//Toast.makeText(getBaseContext(), "GPS Disabled - "+provider, Toast.LENGTH_LONG);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            // probably not needed
        	//Toast.makeText(getBaseContext(), "GPS Enabled - "+provider, Toast.LENGTH_LONG);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // TODO Do I need this to do anything?
        	//Toast.makeText(getBaseContext(), "Status Changed \n provider = "+provider+"\n status = "+status, Toast.LENGTH_LONG);
        }
    }

    

}