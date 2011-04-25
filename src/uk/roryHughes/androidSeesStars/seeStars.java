package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class seeStars extends Activity implements SensorEventListener
{
	private static final String TAG = "See Stars";
	
	private LocationManager mLocManager;
    private LocationListener mLocListener;
    private Location mLocation;   
    private TLEDownloader mTLEGetter;
    private ConnectivityManager mConManager;
    private SensorManager mSensorManager;
    
    //orientation values
    private TextView mOutputXo;
    private TextView mOutputYo;
	private TextView mOutputZo;
	//GPS values
	private TextView mGPSlat;
	private TextView mGPSlon;
	private TextView mSatLat;
	private TextView mSatLon;
	
	private static final String[] mCelestrakTles =
	{"tle-new","stations","visual","1999-025","iridium-33-debris","cosmos-2251-debris",
		"weather","noaa","goes","resource","sarsat","dmc","tdrss","geo","intelsat",
		"gorizont","raduga","molniya","iridium","orbcomm","globalstar","amateur",
		"x-comm","other-comm","gps-ops","glo-ops","galileo","sbas","nnss","musson",
		"science","geodetic","engineering","military","radar","cubesat","other"
	};
    private static final String mCelestrakPrefix = "http://celestrak.com/NORAD/elements/";   
    
    Preview mPreview;
    Camera mCamera;
    int mDefaultCameraId;
    SurfaceView mSurfaceView;
    SatelliteView mSatelliteView;
    
    private float[] mOrientation = new float[] {0,0,0};
    private float[] mRotationMatrixR = new float[9];
    private float[] mGravityVector = new float[3];
    private float[] mGeoMagVector = new float[3];
    
    private TLEParser mTLEParser;
    private Satellite mSatellite;
    
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
        
        mOutputXo = (TextView)findViewById(R.id.TVmOutputXo);
        mOutputYo = (TextView)findViewById(R.id.TVmOutputYo);
        mOutputZo = (TextView)findViewById(R.id.TVmOutputZo);
        mGPSlat   = (TextView)findViewById(R.id.TVGPSlat);
        mGPSlon   = (TextView)findViewById(R.id.TVGPSlon);
        mSatLat   = (TextView)findViewById(R.id.TVSatLat);
        mSatLon   = (TextView)findViewById(R.id.TVSatLon);
        mGPSlat.setText("Sat Lat  : Working...");
    	mGPSlon.setText("Sat Lon  : Working...");
        mSatLat.setText("Sat Lat  : Working...");
    	mSatLon.setText("Sat Lon  : Working...");
        
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocListener = new MyLocationListener();
        
        // get TLEsupdates every time app is started as TLEs update regularly
        // if no connection, use existing files
        mConManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mTLEGetter = new TLEDownloader(getApplicationContext());
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //TODO get magnetic declination - only has to be done once
        
        mPreview = new Preview(this, (SurfaceView)findViewById(R.id.surface));
        mSatelliteView = (SatelliteView)findViewById(R.id.sat_view);
        mSatelliteView.setKeepScreenOn(true);
        
        mTLEParser = new TLEParser(this);
        mSatellite = new Satellite();
        
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
        mLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        try
        {
        	mGPSlat.setText("User Lat :"+Double.toString(mLocation.getLatitude()));
        	mGPSlon.setText("User Lon :"+Double.toString(mLocation.getLongitude()));
        	mSatelliteView.setLocation(mLocation.getLatitude(), mLocation.getLongitude());
        }
        catch(NullPointerException e) { Log.i(TAG,"On Create - getLastKnownLoc caused "+e); }

        
        new Thread(new Runnable() {
        	public void run() {
        		//TODO check connection before trying to start download
        		
        		//not downloading TLEs because its making testing take FOREVER 
        		//uncomment once stuff working
        		//mTLEGetter.downloadTLESet(mCelestrakTles, mCelestrakPrefix, mConManager);
        		Log.d(TAG, "!!  SKIPPING TLE DOWNLOAD  !!");
        		
        		mTLEParser.parseTLE("stations"+".txt", "ISS (ZARYA)", mSatellite);
        		//using the ISS for testing
        		//TODO add option to change satellites via menu when working
        		mSatelliteView.setSatellite(mSatellite);
        	}
        }).start();
     
        //this wont work once using actual sat lat/lon - isn't needed once its working anyway
        mSatLat.setText("Sat Lat  :"+Double.toString(mSatellite.getSatLat()));
		mSatLon.setText("Sat Lon  :"+Double.toString(mSatellite.getSatLon()));
    	
    }
   
    public void onResume()
    {
    	super.onResume();
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        mPreview.mCamera = Camera.open();
    }
    
    public void onPause()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    	mPreview.mCamera.release();
    	mPreview.mCamera = null;
    	super.onPause();
    }
    
    public void onStop()
    {
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
    			mGPSlat.setText("User Lat :"+Double.toString(loc.getLatitude()));
				mGPSlon.setText("User Lon :"+Double.toString(loc.getLongitude()));
				mSatelliteView.setLocation(loc.getLatitude(), loc.getLongitude());
				
				mSatLat.setText("Sat Lat  :"+Double.toString(mSatellite.getSatLat()));
				mSatLon.setText("Sat Lat  :"+Double.toString(mSatellite.getSatLon()));
    		}
    	}
    	
    	@Override
        public void onProviderDisabled(String provider)
    	{
    		mGPSlat.setText("GPS DISABLED");
    		mGPSlon.setText("GPS DISABLED");
    		mSatelliteView.setLocation(Double.NaN, Double.NaN);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        	mGPSlat.setText("GPS ENABLED");
        	mGPSlon.setText("GPS ENABLED");
        	mGPSlat.setText(Double.toString(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude()));
        	mGPSlon.setText(Double.toString(mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude()));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            //Do I need this to do anything?
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
                		//accelerometer event - phone has moved so getOrientation
	                	updateOrientation(SensorManager.getOrientation(mRotationMatrixR, mOrientation));
	                	mOutputXo.setText("x:"+Float.toString(mOrientation[0]));
	                    mOutputYo.setText("y:"+Float.toString(mOrientation[1]));
	                    mOutputZo.setText("z:"+Float.toString(mOrientation[2]));
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
		  * for orientation/accelerometer
		  * probably not needed
		  */
	}
	
	public void updateOrientation(float[] _orientation)
	{
		if(mSatelliteView != null)
		{
			//TODO take magnetic declination into account
			
			mSatelliteView.setOrientation(_orientation);
			mOrientation = _orientation;
			mSatelliteView.invalidate();
		}
	}
	

}