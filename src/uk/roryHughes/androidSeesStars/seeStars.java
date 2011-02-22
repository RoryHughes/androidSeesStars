package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class seeStars extends Activity
{
    private LocationManager mLocManager;
    private LocationListener mLocListener;
    private TLEDownloader mTLEGetter;

    //private PowerManager mPowerManager;
    //private WakeLock mWakeLock;
    //private SensorManager mSensorManager;
    //private Sensor mAccelerometer;

    private ConnectivityManager mConManager;
    
    private static final String[] mCelestrakTles =
	{"tle-new","stations","visual","1999-025","iridium-33-debris","cosmos-2251-debris",
		"weather","noaa","goes","resource","sarsat","dmc","tdrss","geo","intelsat",
		"gorizont","raduga","molniya","iridium","orbcomm","globalstar","amateur",
		"x-comm","other-comm","gps-ops","glo-ops","galileo","sbas","nnss","musson",
		"science","geodetic","engineering","military","radar","cubesat","other"
	};
    private static final String mTlePrefix = "http://celestrak.com/NORAD/elements/";
    //TODO - change to res file - can change files to use & prefix without rewriting any code
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    //TODO - show logo
        setContentView(R.layout.main);
        TextView tv = (TextView)findViewById(R.id.textTest);
        tv.setText(R.string.getting_tles);
        
    //lock screen on
        //mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        //mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());
        
    //set up location
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocListener = new MyLocationListener();
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
        
    //get TLEs - if network connection exists, update files
        mConManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mTLEGetter = new TLEDownloader();
        int numFilesDownloaded = mTLEGetter.downloadTLESet(mCelestrakTles, mTlePrefix, mConManager, this);
        if(numFilesDownloaded >= 0)
        {
        	StringBuilder txt = new StringBuilder();
        	txt.append(Integer.toString(numFilesDownloaded));
        	txt.append(" TLE Files Downloaded");
        	tv.setText(txt);
        }
        else
        	tv.setText(R.string.no_connectivity);
        
    //make use of sensors
        //just playing around here TODO - make this useful
        
    }
    
    public void onResume()
    {
    	super.onResume();
    	//mWakeLock.acquire();
    	//TODO - do something here so it doesnt crash
    }
    
    public void onPause()
    {
    	//mWakeLock.release();
    	super.onPause();
    	//TODO - do something here so it doesnt crash
    }
    
    public void onStop()
    {
    	//mWakeLock.release();
    	super.onStop();
    	//TODO - do something here so it doesnt crash
    }
    
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