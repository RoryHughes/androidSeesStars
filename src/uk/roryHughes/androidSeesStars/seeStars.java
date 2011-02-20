package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class seeStars extends Activity
{
    private LocationManager lm;
    private LocationListener locListener;
    
    //private String TLEUrl = "http://celestrak.com/NORAD/elements/stations.txt";
    //private String TLEFileName = "isstle.txt";
    
    private static final String[] celestrakTles =
	{"tle-new","stations","visual","1999-025","iridium-33-debris","cosmos-2251-debris",
		"weather","noaa","goes","resource","sarsat","dmc","tdrss","geo","intelsat",
		"gorizont","raduga","molniya","iridium","orbcomm","globalstar","amateur",
		"x-comm","other-comm","gps-ops","glo-ops","galileo","sbas","nnss","musson",
		"science","geodetic","engineering","military","radar","cubesat","other"
	}; //TODO - change to res file - can change files to use without rewriting any code
    private static final String tlePrefix = "http://celestrak.com/NORAD/elements/";
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //TODO - show logo
        
        //set up location
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locListener = new MyLocationListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        
        //get TLEs
        TLEDownloader TLEGetter = new TLEDownloader();
        int count = 0;
        for(String tle : celestrakTles)
        {
        	TLEGetter.downloadFromUrl(tlePrefix+tle+".txt", tle+".txt", this);
        	count++;
        }
        
        TextView tv = (TextView)findViewById(R.id.textTest);
		tv.setText(Integer.toString(count));
        
        /*TLEGetter.downloadFromUrl(TLEUrl, TLEFileName, this);
        	//will loop through all tle files @ celestrack, calling this each time
        	//overwrite any existing files, only use old ones if no network connection
        displayTLEFile();*/
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

    /*private void displayTLEFile()
    {
    	try
    	{
    		StringBuilder text = new StringBuilder();
    		
    		FileInputStream fis = getBaseContext().openFileInput(TLEFileName);
    		BufferedInputStream bis = new BufferedInputStream(fis);
    		ByteArrayBuffer bab = new ByteArrayBuffer(50);
    		int curr = 0;
			try
			{
				while((curr = bis.read()) != -1)
				{
					bab.append((byte) curr);
				}
			}
			catch (IOException e)
			{
				//TODO - do error handling
			}
			byte[] ba = bab.toByteArray();
			for(int i = 0; i <ba.length; i++)
			{
				text.append((char) ba[i]); 
			}
    		
			
			TextView tv = (TextView)findViewById(R.id.textTest);
			tv.setText(text);
    	}
    	catch(FileNotFoundException e)
    	{
    		//TODO - error handling
    	}
    	
    }*/

}