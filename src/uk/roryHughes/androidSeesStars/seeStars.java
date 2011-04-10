package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;
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
    
    //accelerometer values - temp
    private TextView mOutputXa;
    private TextView mOutputYa;
    private TextView mOutputZa;
	//orientation values - temp
    private TextView mOutputXo;
    private TextView mOutputYo;
	private TextView mOutputZo;
	//GPS values - temp
	private TextView mGPSlat;
	private TextView mGPSlon;
	
	//TODO - change to res file - can change files & prefix without rewriting any code
	private static final String[] mCelestrakTles =
	{"tle-new","stations","visual","1999-025","iridium-33-debris","cosmos-2251-debris",
		"weather","noaa","goes","resource","sarsat","dmc","tdrss","geo","intelsat",
		"gorizont","raduga","molniya","iridium","orbcomm","globalstar","amateur",
		"x-comm","other-comm","gps-ops","glo-ops","galileo","sbas","nnss","musson",
		"science","geodetic","engineering","military","radar","cubesat","other"
	};
    private static final String mCelestrakPrefix = "http://celestrak.com/NORAD/elements/";   
    
    //video stuff
    Preview mPreview;
    Camera mCamera;
    int mDefaultCameraId;
    SurfaceView mSurfaceView;
    
    SatelliteView mSatelliteView;
    
    private float[] mOrientation = new float[] {0,0,0};
    private float[] mPrevOrientation = new float[] {0,0,0};
    private float[] mRotationMatrixR = new float[9];
    private float[] mRemappedRotationMatrixR = new float[9];
    private float[] mGravityVector = new float[3];
    private float[] mGeoMagVector = new float[3];
    
    
    private float mHeadingChange = 0;
    private int mPrevHeadingChange = 0; //TODO this should probably be called count something
    private float mAverageHeadingChange = 0;
    private float[] mHeadingChangeArr = new float[5];
    
    private float mPitchChange = 0;
    private int mPrevPitchChange = 0;//TODO this should probably be called count something
    private float mAveragePitchChange = 0;
    private float[] mPitchChangeArr = new float[5];
    
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        //TODO - lock screen on when app is focused - unlock onPause() etc.
        
        setContentView(R.layout.main);
        
        TextView tvInfo = (TextView)findViewById(R.id.textInfo);
        mOutputXa = (TextView)findViewById(R.id.TVmOutputXa);
        mOutputYa = (TextView)findViewById(R.id.TVmOutputYa);
        mOutputZa = (TextView)findViewById(R.id.TVmOutputZa);
        mOutputXo = (TextView)findViewById(R.id.TVmOutputXo);
        mOutputYo = (TextView)findViewById(R.id.TVmOutputYo);
        mOutputZo = (TextView)findViewById(R.id.TVmOutputZo);
        mGPSlat   = (TextView)findViewById(R.id.TVGPSlat);
        mGPSlon   = (TextView)findViewById(R.id.TVGPSlon);    
        
        //set up location
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocListener = new MyLocationListener();
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
        mLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        
        //TODO - show updated gps location once we have a fix
        try
        {
        	mGPSlat.setText(Double.toString(mLocation.getLatitude()));
        	mGPSlon.setText(Double.toString(mLocation.getLongitude()));
        }
        catch(NullPointerException e)
        {
        	Log.i(TAG,"On Create - getLastKnownLoc caused "+e);
        }
        //get TLEs
        //TODO - check if files exist & are up to date (if less than e.g. 1 day old don't bother)
        mConManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mTLEGetter = new TLEDownloader(getApplicationContext());
        tvInfo.setText(R.string.getting_tles);
        new Thread(new Runnable() {
        	public void run() {
        		mTLEGetter.downloadTLESet(mCelestrakTles, mCelestrakPrefix, mConManager);
        	}
        }).start();
        
        
        //setup sensor manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //TODO get magnetic declination - only has to be done once
        
        //set up video stuff
        // Find the ID of the default camera
        int numberOfCameras = Camera.getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < numberOfCameras; i++)
            {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
                {
                    mDefaultCameraId = i;
                }
            }
        mSurfaceView = (SurfaceView)findViewById(R.id.surface);
        mPreview = new Preview(this, mSurfaceView);

        
        mSatelliteView = (SatelliteView)findViewById(R.id.sat_view);
        
    }
   
    public void onResume()
    {
    	super.onResume();
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        mPreview.mCamera = Camera.open();
    }
    
    public void onPause()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    	mPreview.mCamera.release();
    	mPreview.mCamera = null;
    	super.onPause();
    }
    
    public void onStop()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    	//mPreview.mCamera.release();
    	//mPreview.mCamera = null;
    	super.onStop();
    }
    
    private class MyLocationListener implements LocationListener
    {
    	public void onLocationChanged(Location loc)
    	{
    		if(loc != null)
    		{
				mGPSlat.setText(Double.toString(loc.getLatitude()));
				mGPSlon.setText(Double.toString(loc.getLongitude()));
    		}
    	}
    	
    	@Override
        public void onProviderDisabled(String provider)
    	{
            // TODO Prompt to enable gps & click to goto settings
    		mGPSlat.setText("GPS DISABLED");
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        	mGPSlat.setText("GPS ENABLED");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // TODO Do I need this to do anything?
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
                	//mOutputXa.setText("x:"+Float.toString(mGravityVector[0]));
                    //mOutputYa.setText("y:"+Float.toString(mGravityVector[1]));
                    //mOutputZa.setText("z:"+Float.toString(mGravityVector[2]));
                break;
                
                case Sensor.TYPE_MAGNETIC_FIELD:
                	mGeoMagVector = event.values.clone();
                break;
                
                case Sensor.TYPE_ORIENTATION:
                	/* Magnetic field sensor had problems @ one point
                	 * used else block to get around it, get rid of else block after testing
                	 */
                	//if(SensorManager.getRotationMatrix(mRotationMatrixR, mRotationMatrixI, mGravityVector, mGeoMagVector))
                	if(SensorManager.getRotationMatrix(mRotationMatrixR, null, mGravityVector, mGeoMagVector))
                	{
	                	//mOrientation = SensorManager.getOrientation(mRotationMatrixR, mOrientation);
	                	//SensorManager.remapCoordinateSystem(mRotationMatrixR, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRemappedRotationMatrixR);
                		updateOrientation(SensorManager.getOrientation(mRemappedRotationMatrixR, mOrientation));
	                	mOutputXo.setText("x:"+Float.toString(mOrientation[0]));
                        mOutputYo.setText("y:"+Float.toString(mOrientation[1]));
                        mOutputZo.setText("z:"+Float.toString(mOrientation[2]));
                	}
                	else
                	{
                		Log.d(TAG, "using depreciated method - somethings gone wrong here");
                		//mOrientation = new float[] {event.values[0], event.values[1], event.values[2]};
                		updateOrientation( new float[] {event.values[0], event.values[1], event.values[2]});
                		mOutputXo.setText("x:"+Float.toString(event.values[0]));
                        mOutputYo.setText("y:"+Float.toString(event.values[1]));
                        mOutputZo.setText("z:"+Float.toString(event.values[2]));
                	}
                	
                break;
            }
            
        }
    }
    
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		 /*
		  * for orientation/accelerometer
		  * probably not needed, just here to keep compiler happy
		  */
	}
	
	public void updateOrientation(float[] _orientation)
	{
		if(mSatelliteView != null)
		{
			//TODO _orientation[0] = _orientation[0] + magnetic declination
			if(_orientation[0] > 360)
				_orientation[0] -= 360;
			else if(_orientation[0] < 0)
				_orientation[0] += 360;
			
			mSatelliteView.setOrientation(_orientation);
			mOrientation = _orientation;
		}
	}
	
	
	/*
	private void updateOrientation(float[] _orientation)
	{
		//calculations for heading/azimuth
		//TODO  change all "heading" vars to azimuth
		mHeadingChange = _orientation[0] - mPrevOrientation[0];
		if(mHeadingChange > 180)
		{
			mHeadingChange = (-360 - mHeadingChange)*-1;
		}
		else if(mHeadingChange < -180)
		{
			mHeadingChange = (-360 - mHeadingChange)*-1;
		}
		if(mPrevHeadingChange == 4)
			mPrevHeadingChange = 0;
		
		mAverageHeadingChange = 0;
		mHeadingChangeArr[mPrevHeadingChange++] = mHeadingChange;
		for(int i = 0; i <5; i++)
		{
			mAverageHeadingChange+=mHeadingChangeArr[i];
		}
		mAverageHeadingChange = mAverageHeadingChange/5; //TODO 10 should be sensor sensitivity
		//_orientation[0] = mPrevOrientation[0] + mAverageHeadingChange;
		
		//calculations for pitch
		
		mPitchChange = _orientation[1] - mPrevOrientation[1];
		if(mPrevPitchChange == 4)
			mPrevPitchChange = 0;
		
		mAveragePitchChange = 0;
		mPitchChangeArr[mPrevPitchChange++] = mPitchChange;
		for(int j = 0; j < 5; j++)
		{
			mAveragePitchChange += mPitchChangeArr[j];
		}
		mAveragePitchChange = mAveragePitchChange/5; //TODO 10 should be sensor sensitivity
		//_orientation[1] = mPrevOrientation[1] + mAveragePitchChange;
		
		if(mSatelliteView != null)
		{
			//TODO _orientation[0] = _orientation[0] + magnetic declination
			if(_orientation[0] > 360)
				_orientation[0] -= 360;
			else if(_orientation[0] < 0)
				_orientation[0] += 360;
			
			mSatelliteView.setOrientation(_orientation);
		}
		mPrevOrientation = mOrientation;
		mOrientation = _orientation;
	}
	*/
	

}