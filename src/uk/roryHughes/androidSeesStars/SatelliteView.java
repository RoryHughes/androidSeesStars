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
	//TODO check accuracy for all caclulations
	//TODO find references for formulas used
	
	
	private static final String TAG = "Satellite View";
	private static int failCount = 0;
	private static String failedOn = "";
	
	private static final int mCentreRadius = 10;
	private static final double ANGLE_DIF = Math.toRadians(5);
	
	private SeeStars mSeeStars = null;
	
	private double mReqHeading		= Double.NaN;	//rads
	private double mSurfaceDistance = Double.NaN;	//rads
	private double mReqElevation	= Double.NaN;	//rads
	
	private boolean turnRight = true;
	private double mHeadingDif = Double.NaN;
	private double mElevationDif = Double.NaN;
	private boolean mHeadingOk = false;
	private boolean mElevationOk = false;
	
	private Paint mTextPaint;
	private Paint mLinePaint;
	private Paint mCentrePaintWhite;
	private Paint mCentrePaintGreen;

	Coord centreCoord = new Coord();
	
	
	/**
	 * hold coordinates from drawing on screen
	 */
	private class Coord
	{
		float x;
		float y;
	}

	public void setSeeStars(SeeStars _seeStars)
	{
		this.mSeeStars = _seeStars;
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
		
		mCentrePaintGreen = new Paint(mLinePaint);
		
		mCentrePaintWhite = new Paint(mCentrePaintGreen);
		mCentrePaintWhite.setColor(Color.WHITE);
	}
		
	@Override
	public void onDraw(Canvas canvas)
	{
		synchronized(this)
		{
			if(isReady())
			{
				int width = getWidth();
				int height = getHeight();
				centreCoord.x = width/2;
				centreCoord.y = height/2;
				
				//TODO if running too slow, only do these calcs when needed
				mSeeStars.getSat().calcPos();
				calcGroundDistance();
				//calcUserHeading();
					//should be done by viewpoint whenever orientation changes
				calcReqHeading();
				//calcUserElevation();
					//should be done by viewpoint whenever orientation changes
				calcReqElevation();
				
				drawHeadingArrows(canvas, height, width);
				drawElevationArrows(canvas, height, width);
				
				if(mHeadingOk && mElevationOk)
				{
					canvas.drawCircle(centreCoord.x, centreCoord.y, mCentreRadius, mCentrePaintGreen);
				}
				else
				{
					canvas.drawCircle(centreCoord.x, centreCoord.y, mCentreRadius, mCentrePaintWhite);
				}
				
			}
			else
			{ //for Debugging - remove when done
				failCount++;
				if(failCount == 30)
				{
					failedOn = "";
					if(mSeeStars.getSat() != null)
					{	
						if(Double.compare(mSeeStars.getViewpoint().getLat(), Double.NaN) == 0)
							failedOn += " mLat";
						if(Double.compare(mSeeStars.getViewpoint().getLon(), Double.NaN) == 0)
							failedOn += " mLon";
						if(Double.compare(mSeeStars.getSat().getLat(), Double.NaN) == 0)
							failedOn += " SatLat";
						if(Double.compare(mSeeStars.getSat().getLon(), Double.NaN) == 0)
							failedOn += " SatLon";
					}
					else
					{
						failedOn = "Sat is NULL";
					}
					Log.d(TAG, "onDraw not ready - failed on "+failedOn);
					failCount = 0;
				}
			}
		}
	}
	
	/**
	 * if all vars are set up, we are ready to draw
	 * makes it easier to read on draw without masssive if(...)
	 */
	private boolean isReady()
	{
		if(mSeeStars.getSat() != null)
		{
			if( (Double.compare(mSeeStars.getViewpoint().getLat(), Double.NaN) != 0)
					&&(Double.compare(mSeeStars.getViewpoint().getLon(), Double.NaN) != 0)
					&& (Double.compare(mSeeStars.getSat().getLat(), Double.NaN) != 0)
					&& (Double.compare(mSeeStars.getSat().getLon(), Double.NaN) != 0))
			{
				return true;
			}

		}
		return false;
	}
 
	private void calcGroundDistance()
	{
		//calc surface distance using spherical law of cosines
		//TODO change this if not using spherical earth model (Vincenty formula)
		//find reference for write up
		//this might be close enough even with nonspherical earth, check if its ok
		//		assuming spherical earth for this part - probably close enough if far enough away from the poles
		
		mSurfaceDistance = Math.acos(Math.sin(Math.toRadians(mSeeStars.getViewpoint().getLat()))
				*Math.sin(Math.toRadians(mSeeStars.getSat().getLat()))
				+ Math.cos(Math.toRadians(mSeeStars.getViewpoint().getLat()))
				* Math.cos(Math.toRadians(mSeeStars.getSat().getLat()))
				* Math.cos(Math.toRadians(mSeeStars.getSat().getLon())
				- Math.toRadians(mSeeStars.getViewpoint().getLon())))
				* Viewpoint.rEarth;
		
	}
	
	private void calcReqHeading()
	{
		//find reference for this (got it from http://www.movable-type.co.uk/scripts/latlong.html )
		//this should be fine even when no longer using spherical earth (lat/lon still give same distances)
		double deltaLon = Math.max(mSeeStars.getSat().getLon(), mSeeStars.getViewpoint().getLon())
						  - Math.min(mSeeStars.getSat().getLon(), mSeeStars.getViewpoint().getLon());
		
		double y = Math.sin(Math.toRadians(deltaLon))*Math.cos(Math.toRadians(mSeeStars.getSat().getLat()));
		double x = Math.cos(Math.toRadians(mSeeStars.getViewpoint().getLat()))
					* Math.sin(Math.toRadians(mSeeStars.getSat().getLat()))
					- Math.sin(Math.toRadians(mSeeStars.getViewpoint().getLat()))
					* Math.cos(Math.toRadians(mSeeStars.getSat().getLat())
					* Math.cos(Math.toRadians(deltaLon)));
		
		mReqHeading = Math.atan2(y, x);		
	}
	
	private void calcReqElevation()
	{
		/*TODO improve the maths to take non-spherical earth into account
		 * 		can probably use some of the Sputnik library to do this
		 * 		might have to make use of Station Class for this
		 *
		 * VERY poor model of earth here
		 *
		 *using law of cosines to find user-sat distance a = sqrt( b^2+c^2 - 2bc Cos(A) )
		 *once, side lengths are known, find Elevation angle
		 *law of cosines B = arccos( (a^2+c^2-b^2)/2ac )
		 *elevation is angle from 0, where 0 is pointing down (facing ground)
		*/
		
		double b = Viewpoint.rEarth+mSeeStars.getSat().getAltitude();
		double c = Viewpoint.rEarth;
		double thetaA = mSurfaceDistance/Viewpoint.rEarth;
		double a = Math.sqrt( (Math.pow(b,2)+ Math.pow(c,2)) - (2*b*c*Math.cos(thetaA)) ); //user-sat distance
		
		double thetaB = Math.acos((Math.pow(a,2) + Math.pow(c,2)- Math.pow(b,2)) / (2*a*c));

		mReqElevation = thetaB;
	}

	private void drawHeadingArrows(Canvas canvas, int height, int width)
	{
		mHeadingDif = Math.max(mSeeStars.getViewpoint().getHeading(), mReqHeading) - Math.min(mSeeStars.getViewpoint().getHeading(), mReqHeading);
		
		if(mHeadingDif > ANGLE_DIF)
		{
			mHeadingOk = false;
			if(Double.compare(mSeeStars.getViewpoint().getHeading(), mReqHeading) < 0)
				turnRight = true;
			else
				turnRight = false;
			if(mHeadingDif > Math.PI)
				turnRight = !turnRight;
	
			if(turnRight)
			{
				//point right
				canvas.drawLine(centreCoord.x + (width/4), centreCoord.y, centreCoord.x + (width/2), centreCoord.y, mLinePaint);
				canvas.drawLine(centreCoord.x + (width/2), centreCoord.y, centreCoord.x + (width/2)-(width/8), centreCoord.y + (height/8), mLinePaint);
				canvas.drawLine(centreCoord.x + (width/2), centreCoord.y, centreCoord.x + (width/2)-(width/8), centreCoord.y - (height/8), mLinePaint);
			}
			else
			{
				//point left
				canvas.drawLine(centreCoord.x - (width/4), centreCoord.y, centreCoord.x - (width/2), centreCoord.y, mLinePaint);
				canvas.drawLine(centreCoord.x - (width/2), centreCoord.y, centreCoord.x - (width/2)+(width/8), centreCoord.y + (height/8), mLinePaint);
				canvas.drawLine(centreCoord.x - (width/2), centreCoord.y, centreCoord.x - (width/2)+(width/8), centreCoord.y - (height/8), mLinePaint);
			}
		}
		else
		{
			mHeadingOk = true;
		}
	}

	private void drawElevationArrows(Canvas canvas, int height, int width)
	{		
		mElevationDif = Math.max(mSeeStars.getViewpoint().getElevation(), mReqElevation) - Math.min(mSeeStars.getViewpoint().getElevation(), mReqElevation);
		
		if(mElevationDif > ANGLE_DIF)
		{
			mElevationOk = false;
			if(Double.compare(mSeeStars.getViewpoint().getElevation(), mReqElevation) < 0)
			{
				//point up
				canvas.drawLine(centreCoord.x, centreCoord.y - (height/4), centreCoord.x, centreCoord.y - (height/2), mLinePaint);
				canvas.drawLine(centreCoord.x, centreCoord.y - (height/2), centreCoord.x - (width/16), centreCoord.y - (3* (height/8)), mLinePaint);
				canvas.drawLine(centreCoord.x, centreCoord.y - (height/2), centreCoord.x + (width/16), centreCoord.y - (3* (height/8)), mLinePaint);
			}
			else
			{
				//doint down
				canvas.drawLine(centreCoord.x, centreCoord.y + (height/4), centreCoord.x, centreCoord.y + (height/2), mLinePaint);
				canvas.drawLine(centreCoord.x, centreCoord.y + (height/2), centreCoord.x + (width/16), centreCoord.y + (3* (height/8)), mLinePaint);
				canvas.drawLine(centreCoord.x, centreCoord.y + (height/2), centreCoord.x - (width/16), centreCoord.y + (3* (height/8)), mLinePaint);
			}
		}
		else
		{
			mElevationOk = true;
		}
	}

}