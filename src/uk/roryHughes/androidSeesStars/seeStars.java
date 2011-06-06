package uk.roryHughes.androidSeesStars;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class SeeStars extends Activity implements SensorEventListener
{
	private static final String TAG = "See Stars";
	private static final int NEW_TLES 	= 0;
	private static final int OLD_TLES 	= 1;
	private static final int NO_TLES	= 2;
	private static final int SAT_MENU_ID = 0;
	
	private LocationManager mLocManager;
    private LocationListener mLocListener;
    private Location mLocation;   
    private TLEDownloader mTLEDownloader;
    private ConnectivityManager mConManager;
    private SensorManager mSensorManager;
    
    private TextView mCurrSatTV;
    private TextView mGPSlat;
	private TextView mGPSlon;
	private TextView mLocationStatus;
	private TextView mDownloadStatus;
	
	private static String[] mTleFileNames;
	private static String mTlePrefix;   
    
    private Preview mPreview;

    private SatelliteView mSatelliteView;
    
    private float[] mOrientation = new float[] {0,0,0};
    private float[] mRotationMatrixR = new float[9];
    private float[] mGravityVector = new float[3];
    private float[] mGeoMagVector = new float[3];
    private GeomagneticField mGeomagField;
    private Date date = new Date();
    
    private TLEParser mTLEParser;
    private Thread mTLEThread;
    
    private Satellite mCurrSat = null;
    private Viewpoint mViewpoint = null;
    
    private Handler mDownloadMessageHandler;
    private ArrayList<ArrayList<String>> tleMenuSets;
    private SubMenu satMenu;
    
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.main);
        mCurrSatTV		= (TextView)findViewById(R.id.curr_sat);
        mGPSlat   		= (TextView)findViewById(R.id.TVGPSlat);
        mGPSlon   		= (TextView)findViewById(R.id.TVGPSlon);
        mLocationStatus = (TextView)findViewById(R.id.location_status);
        mDownloadStatus = (TextView)findViewById(R.id.download_status);
        mTleFileNames = getResources().getStringArray(R.array.celestrak_tles);
        mTlePrefix = getString(R.string.celestrak_prefix);
        mGPSlat.setText("User Lat  : Working...");
    	mGPSlon.setText("User Lon  : Working...");
    	mDownloadStatus.setText("Download Status - Setting Up");
    	mLocationStatus.setText("Location Status - Setting Up");
        
    	mPreview = new Preview(this, (SurfaceView)findViewById(R.id.surface));
        mSatelliteView = (SatelliteView)findViewById(R.id.sat_view);
        mSatelliteView.setSeeStars(this);
        mSatelliteView.setKeepScreenOn(true);

        // get TLEs updates every time app is started as TLEs update regularly
        mConManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mTLEDownloader = new TLEDownloader(getApplicationContext());
        mTLEParser = new TLEParser();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        mViewpoint = new Viewpoint();
        setupLocation();
        
        mDownloadMessageHandler = new Handler()
        {
        	public void handleMessage(Message msg)
        	{
        		//do things
        		Log.d(TAG, "recieved message = "+msg.what);
        		switch(msg.what)
        		{
        		case NEW_TLES : mDownloadStatus.setText("Download Status - OK");
        						mCurrSatTV.setText("Tracking " + mCurrSat.getName());
        			break;
        		case OLD_TLES : mDownloadStatus.setText("Download Status - Failed, Using Old TLEs");
								mCurrSatTV.setText("Tracking '" + mCurrSat.getName()+"'");
        			break;
        		case NO_TLES  : mDownloadStatus.setText("Download Status - Failed, No TLEs Found");
								mCurrSatTV.setText("Check Network Connection and Restart App - No Local or Remote TLEs found");
        			break;
        		}
        	}
        };
        
        mTLEThread = new Thread(new Runnable() {
        	public void run() {
        		if(mTLEDownloader.downloadTLESet(mTleFileNames, mTlePrefix, mConManager))
        		{
        			//ISS is default satellite
        			setSatellite("stations.txt", "ISS (ZARYA)");
        			mDownloadMessageHandler.sendEmptyMessage(NEW_TLES);
        		}
        		else
        		{
        			//download error - use existing files (inform user)
    				//ISS is default satellite
        			setSatellite("stations.txt", "ISS (ZARYA)");
        			if(mCurrSat != null)
        			{
        				Log.w(TAG, "Download error - using local (old) TLEs");
        				mDownloadMessageHandler.sendEmptyMessage(OLD_TLES);
        			}
        			else
        			{
        				Log.e(TAG, "TLE Download error and No Local TLEs");
        				mDownloadMessageHandler.sendEmptyMessage(NO_TLES);
        			}
        		}
        		//not downloading TLEs because its making testing take FOREVER
        		//uncomment above & remove below once working
        		//Log.d(TAG, "Skipping TLE Download!");
        		//setSatellite("stations.txt", "ISS (ZARYA)");
        		//mDownloadMessageHandler.sendEmptyMessage(OLD_TLES);
        	}
        });
        mTLEThread.start();
    }
   
    public void onResume()
    {
    	super.onResume(); 
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
        mLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mPreview.mCamera = Camera.open();
    }
    
    public void onPause()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    	mLocManager.removeUpdates(mLocListener);
    	mPreview.mCamera.release();
    	mPreview.mCamera = null;
    	super.onPause();
    }
    
    public void onStop()
    {
    	//TODO - does this need to be here?
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    	super.onStop();
    }
    
    private class MyLocationListener implements LocationListener
    {
    	public void onLocationChanged(Location loc)
    	{
    		if(loc != null)
    		{    			
				mLocationStatus.setText("Location Status - Got GPS Fix");
    			mGPSlat.setText("User Lat :"+Double.toString(loc.getLatitude()));
				mGPSlon.setText("User Lon :"+Double.toString(loc.getLongitude()));
				mViewpoint.setLat(loc.getLatitude());
				mViewpoint.setLon(loc.getLongitude());
    		}
    	}
    	
    	@Override
        public void onProviderDisabled(String provider)
    	{
    		mLocationStatus.setText("Location Status - GPS Disabled");
    		mGPSlat.setText("GPS DISABLED");
    		mGPSlon.setText("GPS DISABLED");
    		mViewpoint.setLat(Double.NaN);
			mViewpoint.setLon(Double.NaN);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        	mLocationStatus.setText("Location Status - Getting fresh GPS Fix");
        	mGPSlat.setText(Double.toString(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()));
        	mGPSlon.setText(Double.toString(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude()));
        	
        	mViewpoint.setLat(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());
        	mViewpoint.setLon(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            //TODO - Do I need this to do anything?
        	//maybe - not massivly important - leave till later
        }
    }

    public void onSensorChanged(SensorEvent event)
    {
    	synchronized (this)
    	{
    		switch (event.sensor.getType())
    		{
                case Sensor.TYPE_ACCELEROMETER:
                	mGravityVector = event.values.clone();
                	if(SensorManager.getRotationMatrix(mRotationMatrixR, null, mGravityVector, mGeoMagVector))
                	{
	                	updateOrientation(SensorManager.getOrientation(mRotationMatrixR, mOrientation));
                	}
                break;
                
                case Sensor.TYPE_MAGNETIC_FIELD:
                	mGeoMagVector = event.values.clone();
                break;
            }
            
        }
    }
    
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		 /*
		  * for orientation/accelerometer - here to keep compiler happy
		  */
	}
	
	public void updateOrientation(float[] _orientation)
	{
		//TODO check declination
		_orientation[0] += Math.toRadians(mGeomagField.getDeclination());
		mViewpoint.setOrientation(_orientation);
		mSatelliteView.invalidate();
		
	}
	
	private void setupLocation()
	{
		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocListener = new MyLocationListener();
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
		
        mLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	
        if(mLocation != null)
    	{
    		mLocationStatus.setText("Location Status - Getting GPS Fix");
        	mGPSlat.setText("User Lat :"+Double.toString(mLocation.getLatitude()));
    		mGPSlon.setText("User Lon :"+Double.toString(mLocation.getLongitude()));
    		mViewpoint.setLat(mLocation.getLatitude());
    		mViewpoint.setLon(mLocation.getLongitude());
       		mGeomagField = new GeomagneticField((float)mLocation.getLatitude(), (float)mLocation.getLongitude(), (float)mLocation.getAltitude(), date.getTime());
    	}
        else
        {
    		Log.w(TAG, "GPS failed, trying corse location");
    		mLocation = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    		if(mLocation != null)
    		{
    			mLocationStatus.setText("Location Status - No GPS Fix, Using Network Fix");
    			mGPSlat.setText("User Lat :"+Double.toString(mLocation.getLatitude()));
        		mGPSlon.setText("User Lon :"+Double.toString(mLocation.getLongitude()));
        		mViewpoint.setLat(mLocation.getLatitude());
        		mViewpoint.setLon(mLocation.getLongitude());
        		mGeomagField = new GeomagneticField((float)mLocation.getLatitude(), (float)mLocation.getLongitude(), (float)mLocation.getAltitude(), date.getTime());
    		}    	
	    	else
	    	{
	    		Log.e(TAG, "NO LOCATION AVAILABLE!");
	    		mLocationStatus.setText("Location Status - NO LOCATION DATA - enable GPS & Restart App");
	    	}
        }
	}

	public Satellite getSat()
	{
		return this.mCurrSat;
	}
	
	public Viewpoint getViewpoint()
	{
		return mViewpoint;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		satMenu = menu.addSubMenu(Menu.NONE, SAT_MENU_ID, Menu.NONE, "Select TLE Set");
		
		tleMenuSets = getAvailableSatList();
		//holds arraylist<String> for each tle file
		//first string = file name, rest = sat names
		
		if(!tleMenuSets.isEmpty())
		{
			for(ArrayList<String> file : tleMenuSets)
			{
				satMenu.add(Menu.NONE, file.get(0).hashCode(), Menu.NONE,  file.get(0).substring(0, file.get(0).length()-4));
			}
		}
		else
		{
			satMenu.add("ERROR");
			Log.e(TAG, "Error Building TLE file selection menu");
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Log.d(TAG, "Options Menu selected Item id = " + item.getItemId());
		
		for(ArrayList<String> file : tleMenuSets)
		{			
			if(file.get(0).hashCode() == item.getItemId())
			{
				Log.d(TAG, "Selected"+file.get(0));
				Intent i = new Intent(this, uk.roryHughes.androidSeesStars.SatPicker.class);
				i.putExtra("file", file);
				startActivityForResult(i, 1);
				return true;
			}
		}
		
		switch(item.getItemId())
		{
		case SAT_MENU_ID :
			break;
		default :
			Log.d(TAG, "ERROR - selected item not found");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private ArrayList<ArrayList<String>> getAvailableSatList()
	{		
		String[] files = this.fileList();
		
		ArrayList<ArrayList<String>> tleContents = new ArrayList<ArrayList<String>>();
		
		ArrayList<String> satNames; 
		String line;
		
		for(String file : files)
		{
			satNames = new ArrayList<String>();
			satNames.add(file);
			try
			{
				FileInputStream fin = openFileInput(file);
				DataInputStream in  = new DataInputStream(fin);
				BufferedReader br   = new BufferedReader(new InputStreamReader(in));
				
				try
				{
					while( (line = br.readLine()) != null)
					{
						if(!(line.startsWith("1") || line.startsWith("2")))
						{
							satNames.add(line);
						}
					}
					in.close();
				}
				catch(IOException e)
				{
					Log.e(TAG, "getSatNames caused "+e);
				}
			}
			catch (FileNotFoundException e)
			{
				Log.e(TAG, "getSatNames caused "+e);
			}
			tleContents.add(satNames);
		}
		return tleContents;
	}
	
	public void setSatellite(String _tleFile, String _satName)
	{
		mCurrSat = (mTLEParser.parseTLE(getFilesDir().getAbsolutePath()+"/"+ _tleFile, _satName, new Satellite()));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == RESULT_OK)
		{
			setSatellite(data.getStringExtra("file_name"), data.getStringExtra("sat_name"));
			mCurrSatTV.setText("Tracking " + mCurrSat.getName());
		}
	}

}