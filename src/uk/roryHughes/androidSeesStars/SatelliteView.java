package uk.roryHughes.androidSeesStars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;



public class SatelliteView extends View
{
	//TODO check accuracy!!
	
	private static final String TAG = "Satellite View";
	
	private Satellite mSatellite = null;
	
	private float[] mOrientation 	= new float[3];
	private double mLat				= Double.NaN;
	private double mLon				= Double.NaN;
	private double mUserHeading 	= Double.NaN;	//rads
	private double mReqHeading		= Double.NaN;	//rads
	private double mSurfaceDistance = Double.NaN;
	private double mDistance 		= Double.NaN; 	//straight line distance "through" earth
	private double mUserElevation	= Double.NaN;
	private double mReqElevation	= Double.NaN;
	private double rEarth 			= 6378.1; 		//average radius in KM (cheap and cheerful from google)
									//TODO use better model of earth than assuming sphere
	
	private Paint mTextPaint;
	private Paint mCentreCirclePaint;

	Coord centreCoord = new Coord();
	
	private int tempCount = 50;
	
	/**
	 * hold coordinates from drawing on screen
	 */
	private class Coord
	{
		float x;
		float y;
	}

	public SatelliteView(Context context)
	{
		super(context);
		setUpView();
	}
	
	public SatelliteView(Context context, AttributeSet atts)
	{
		super(context, atts);	
		setUpView();
	}
	
	public void setOrientation(float[] _orientation)
	{
		this.mOrientation = _orientation;
		//might not need this line - here for now just incase - remove after rewrite
		//SensorManager.remapCoordinateSystem(mOrientation, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, new float[mOrientation.length*10]);
		this.invalidate();
		
	}
	
	public float[] getOrientation()
	{
		return this.mOrientation;
	}
	
	public void setLocation(double _lat, double _lon)
	{
		this.mLat = _lat;
		this.mLon = _lon;
	}
	
	public void setSatellite(Satellite _satellite)
	{
		Log.d(TAG, "Satellite Set");
		this.mSatellite = _satellite;
	}
	
	public Satellite getSatellite()
	{
		return this.mSatellite;
	}
		
	private void setUpView()
	{
		//set up paints
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(Color.GREEN);
		mTextPaint.setStyle(Paint.Style.STROKE);
		mTextPaint.setFakeBoldText(true);
		mTextPaint.setSubpixelText(true);
		
		
		mCentreCirclePaint = new Paint(mTextPaint);
		mCentreCirclePaint.setColor(Color.WHITE);
		
		
	}
		
	@Override
	public void onDraw(Canvas canvas)
	{
		synchronized(this)
		{
			if(isReady())
			{
				//draw!
				int width = getWidth();
				int height = getHeight();
				centreCoord.x = width/2;
				centreCoord.y = height/2;
				
				canvas.drawCircle(centreCoord.x, centreCoord.y, 10, mCentreCirclePaint);
				
				calcGroundDistance();
				
				//calculate users compass bearing
				calcUserHeading();
				calcReqHeading();
				
				
				//calculate required phone elevation
				calcUserElevation();
				calcReqElevation();
				//draw "arrows"
				
			}
		}
	}
	
	/**
	 * if all vars are set up, we are ready to draw
	 * makes it easier to read on draw without masssive if(...)
	 */
	private boolean isReady()
	{
		if( (Double.compare(mLat, Double.NaN) != 0) && (Double.compare(mLon, Double.NaN) != 0)
				&& (Double.compare(mSatellite.getSatLat(), Double.NaN) != 0) && (Double.compare(mSatellite.getSatLon(), Double.NaN) != 0) 
				&& mSatellite != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
 
	private void calcGroundDistance()
	{
		//calc surface distance using spherical law of cosines
		//TODO change this if not using spherical earth model (Vincenty formula)
		//TODO find reference for write up
		
		mSurfaceDistance = Math.acos(Math.sin(Math.toRadians(mLat))*Math.sin(Math.toRadians(mSatellite.getSatLat())) +
				Math.cos(Math.toRadians(mLat))*Math.cos(Math.toRadians(mSatellite.getSatLat())) *
				Math.cos(Math.toRadians(mSatellite.getSatLon())-Math.toRadians(mLon))) * rEarth;
		
		//straight line distance "through" eath = 2R sin(1/2 theta)
		// theta = s/R
		//		=2 R sin((s/R)/2))
		
		mDistance = 2 * rEarth * Math.sin((mSurfaceDistance/rEarth)/2);
		
		if(tempCount == 50)
		{
			Log.d(TAG, "mSurfaceDistance = "+mSurfaceDistance);
			Log.d(TAG, "mDistance        = "+mDistance);
			tempCount = 0;
		}
		tempCount++;
	}
	
	private void calcReqHeading()
	{
		//TODO find reference for this (got it from http://www.movable-type.co.uk/scripts/latlong.html )
		double deltaLat = Math.max(mSatellite.getSatLat(), mLat) - Math.min(mSatellite.getSatLat(), mLat);
		double deltaLon = Math.max(mSatellite.getSatLon(), mLon) - Math.min(mSatellite.getSatLon(), mLon);
		
		double y = Math.sin(Math.toRadians(deltaLon))*Math.cos(Math.toRadians(mSatellite.getSatLat()));
		double x = Math.cos(Math.toRadians(mLat))*Math.sin(Math.toRadians(mSatellite.getSatLat())) -
					Math.sin(Math.toRadians(mLat))*Math.cos(Math.toRadians(mSatellite.getSatLat())*Math.cos(Math.toRadians(deltaLon)));
		
		mReqHeading = Math.atan2(y, x);
		
		//Log.d(TAG, "required bearing = "+Math.toDegrees(mUserHeading));
		
	}
	
	private void calcUserHeading()
	{
		//get heading from orientation sensors
		
		//assuming getOrietnation[0] (x) = heading
		//in rads counter clockwise
		double temp = 0;
		
		if(Double.compare(mOrientation[0], 0) < 0)
		{
			mUserHeading = 2*Math.PI + mOrientation[0];
		}
		else
		{
			mUserHeading = mOrientation[0];
		}
		Log.d(TAG, "User Heading rads = "+mUserHeading);
		Log.d(TAG, "User Heading degs = "+Math.toDegrees(mUserHeading));
		
	}
	
	private void calcReqElevation()
	{
		
	}
	
	private void calcUserElevation()
	{
		
	}
}