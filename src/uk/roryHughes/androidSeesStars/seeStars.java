package uk.roryHughes.androidSeesStars;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
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
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class seeStars extends Activity implements SensorEventListener
{
    private LocationManager mLocManager;
    private LocationListener mLocListener;
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
	
	private TextView mGPSlat;
	private TextView mGPSlon;    
	
	//TLE stuff
	//TODO - change to res file - can change files to use & prefix without rewriting any code
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
        
        //TODO - cleanup views - this seems very messy!
        TextView tvTest = (TextView)findViewById(R.id.textTest);
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
        
        //get TLEs
        //TODO - check if files exist & are up to date (if less than e.g. 1 day old don't bother)
        mConManager =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mTLEGetter = new TLEDownloader(getApplicationContext());
        tvTest.setText(R.string.getting_tles);
        new Thread(new Runnable() {
        	public void run() {
        		mTLEGetter.downloadTLESet(mCelestrakTles, mCelestrakPrefix, mConManager);
        	}
        }).start();
        
        //setup sensor manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
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

        
    }
   
    public void onResume()
    {
    	super.onResume();
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mSensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), mSensorManager.SENSOR_DELAY_GAME);
        mPreview.mCamera = Camera.open();
    }
    
    public void onPause()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    	mPreview.mCamera.release();
    	mPreview.mCamera = null;
    	super.onPause();
    }
    
    public void onStop()
    {
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    	mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
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
            //mGPSlat.setText("GPS ENABLED");
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
                    //mOutputXa.setText("x:"+Float.toString(event.values[0]));
                    //mOutputYa.setText("y:"+Float.toString(event.values[1]));
                    //mOutputZa.setText("z:"+Float.toString(event.values[2]));
                break;
            case Sensor.TYPE_ORIENTATION:
                    //mOutputXo.setText("x:"+Float.toString(event.values[0]));
                    //mOutputYo.setText("y:"+Float.toString(event.values[1]));
                    //mOutputZo.setText("z:"+Float.toString(event.values[2]));
            break;
            }
        }
    }
    
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		 //for orientation/accelerometer
		//TODO - do stuff here if needed
	}

}