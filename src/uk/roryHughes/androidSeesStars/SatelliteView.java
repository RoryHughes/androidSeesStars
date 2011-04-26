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
	
	private static final int mCentreRadius = 10;
	
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
	private static final double rEarth 			= 6378.1; 		//average radius in KM (cheap and cheerful from google)
									//TODO use better model of earth than assuming sphere
	private boolean turnRight = true;
	private double mHeadingDif = Double.NaN;
	
	private Paint mTextPaint;
	private Paint mLinePaint;
	private Paint mCentreCirclePaint;

	Coord centreCoord = new Coord();
	
	
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
		this.invalidate();
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
		
		mLinePaint = new Paint(mTextPaint);
		mLinePaint.setStrokeWidth(5);
		
		
		mCentreCirclePaint = new Paint(mLinePaint);
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
				
				canvas.drawCircle(centreCoord.x, centreCoord.y, mCentreRadius, mCentreCirclePaint);
				
				calcGroundDistance();
				
				calcUserHeading();
				calcReqHeading();
				
				drawHeadingArrows(canvas, height, width);

				calcUserElevation();
				calcReqElevation(); //TODO
				
				drawElevationArrows(canvas, height, width); //TODO
				
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
		
	}
	
	private void calcReqHeading()
	{
		//TODO find reference for this (got it from http://www.movable-type.co.uk/scripts/latlong.html )
		double deltaLon = Math.max(mSatellite.getSatLon(), mLon) - Math.min(mSatellite.getSatLon(), mLon);
		
		double y = Math.sin(Math.toRadians(deltaLon))*Math.cos(Math.toRadians(mSatellite.getSatLat()));
		double x = Math.cos(Math.toRadians(mLat))*Math.sin(Math.toRadians(mSatellite.getSatLat())) -
					Math.sin(Math.toRadians(mLat))*Math.cos(Math.toRadians(mSatellite.getSatLat())*Math.cos(Math.toRadians(deltaLon)));
		
		mReqHeading = Math.atan2(y, x);		
	}
	
	private void calcUserHeading()
	{
		//get heading from orientation sensors		
		//convert from PI/2 = west, -PI/2 = east to PI/2 = west, 3PI/2 = east etc.
		//makes calcs simpler
		if(Double.compare(mOrientation[0], 0) < 0)
		{
			mUserHeading = 2*Math.PI + mOrientation[0];
		}
		else
		{
			mUserHeading = mOrientation[0];
		}
		
	}
	
	private void calcReqElevation()
	{
		//VERY poor model of earth here - need to find a better method
		
		//mReqElevation = 90-chord angle - isoceles angle
		
		//using law of cosines to find user-sat distance a = sqrt( b^2+c^2 - 2bc Cos(A) )
		//once, side lengths are known, find Elevation angle
		//law of cosines A = arccos( (b^2+c^2-a^2)/2bc )

		double b = rEarth+mSatellite.getSatAltitude();
		double c = rEarth;
		double thetaA = rEarth/mSurfaceDistance;
		double a = Math.sqrt( Math.pow(b,2)+ Math.pow(c,2) - 2*b*c*Math.cos(thetaA) ); //user-sat distance
		
		double a_ = b;
		double b_ = c;
		double c_ = a;
		double thetaA_ = Math.acos( (Math.pow(b_,2) + Math.pow(c_,2) - Math.pow(a_,2)) / 2*b_*c_ ); 
		
		mReqElevation = thetaA_;
		
		Log.d(TAG, "req elevation = "+mReqElevation);
	}
	
	private void calcUserElevation()
	{
		//*-1 so using +ve numbers for "infront" of user, makes it easier for me to think!
		mUserElevation = mOrientation[2]*-1;
	}

	private void drawHeadingArrows(Canvas canvas, int height, int width)
	{
		//TODO improve - say when pointing in right direction etc etc
		// if clockwise differnce > PI, point "faster" direction
		
		mHeadingDif = Math.max(mUserHeading, mReqHeading) - Math.min(mUserHeading, mReqHeading);
		
		if(mHeadingDif > Math.toRadians(5))
		{
			if(Double.compare(mUserHeading, mReqHeading) < 0)
				turnRight = true;
			else
				turnRight = false;
			if(mHeadingDif > Math.PI)
				turnRight = !turnRight;
	
			if(turnRight)
			{
				//point right
				canvas.drawLine(centreCoord.x + (width/4), centreCoord.y, centreCoord.x + (width/4)*2, centreCoord.y, mLinePaint);
				canvas.drawLine(centreCoord.x + (width/4)*2, centreCoord.y, centreCoord.x + (width/4), centreCoord.y + (height/4), mLinePaint);
				canvas.drawLine(centreCoord.x + (width/4)*2, centreCoord.y, centreCoord.x + (width/4), centreCoord.y - (height/4), mLinePaint);
			}
			else
			{
				//point left
				canvas.drawLine(centreCoord.x - (width/4), centreCoord.y, centreCoord.x - (width/4)*2, centreCoord.y, mLinePaint);
				canvas.drawLine(centreCoord.x - (width/4)*2, centreCoord.y, centreCoord.x - (width/4), centreCoord.y + (height/4), mLinePaint);
				canvas.drawLine(centreCoord.x - (width/4)*2, centreCoord.y, centreCoord.x - (width/4), centreCoord.y - (height/4), mLinePaint);
			}
			mCentreCirclePaint.setColor(Color.WHITE);
			canvas.drawCircle(centreCoord.x, centreCoord.y, mCentreRadius, mCentreCirclePaint);
		}
		else
		{
			mCentreCirclePaint.setColor(Color.GREEN);
			canvas.drawCircle(centreCoord.x, centreCoord.y, mCentreRadius, mCentreCirclePaint);
		}
	}

	private void drawElevationArrows(Canvas canvas, int height, int width)
	{
		//draw!
	}
}