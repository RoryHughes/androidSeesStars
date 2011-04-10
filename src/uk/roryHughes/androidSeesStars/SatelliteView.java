package uk.roryHughes.androidSeesStars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/*
 * TODO Fix lat/lon grid after satellites are working. isnt totally essential, and sats are more important
 * also... fml.
 */


public class SatelliteView extends View
{
	private static final String TAG = "Satellite View";
	
	private boolean mUpdatingDisplay = false;
	
	//private Paint mSatPaint;
	private Paint mLatGridPaint;
	private Paint mLonGridPaint;
	private Paint mTextPaint;
	private Paint mTextPaint1;
	private Paint mCentreCirclePaint;

	private static double[] lonRadians = new double[360/5 +1];
	private static double[] latRadians = new double[180/5 +1];
	private static String[] lonLabels = new String[360/5 + 1];
	private static String[] latLabels = new String[180/5 + 1];
	
	private float[] mOrientation = new float[3];
	
	Coord centreCoord = new Coord();
	Coord currCoord   = new Coord();
	Coord prevCoord   = new Coord();
	
	////////////////////////////////////////////////////////////////////////////////////
	
	private class Coord
	{
		@SuppressWarnings("unused")
		float x;
		@SuppressWarnings("unused")
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
		//this line because droid Sat did it... don't know if its helping me here or not
		//_orientation[1] = _orientation[1]*-1 -90;
		this.mOrientation = _orientation;
		
		SensorManager.remapCoordinateSystem(mOrientation, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, new float[mOrientation.length*10]);
		this.invalidate();
		
	}
	
	public float[] getOrientation()
	{
		return this.mOrientation;
	}
	
	private void setUpView()
	{
		//set up paints
		mLatGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLatGridPaint.setColor(Color.GREEN);
		mLatGridPaint.setStyle(Paint.Style.STROKE);
		mLatGridPaint.setFakeBoldText(true);
		mLatGridPaint.setSubpixelText(true);
		
		mLonGridPaint = new Paint(mLatGridPaint);
		mLonGridPaint.setColor(Color.RED);
		
		mTextPaint = new Paint(mLatGridPaint);	
		mTextPaint1 = new Paint(mLatGridPaint);
		mTextPaint1.setColor(Color.RED);
		
		mCentreCirclePaint = new Paint(mLatGridPaint);
		mCentreCirclePaint.setColor(Color.WHITE);
		
		///////////////////////////////////////////////////////////////
		//set up arrays for drawing grids
		for(int count = 0; count < lonLabels.length; count++)
			lonLabels[count] = " ";
		
		int j = 0;
		for(int i = 0; i < 360; i += 5)
		{
			lonRadians[j] = Math.toRadians(i);
			switch(i)
			{
			case 0   : lonLabels[j] = "N"; break;
			case 90  : lonLabels[j] = "E"; break;
			case 180 : lonLabels[j] = "S"; break;
			case 270 : lonLabels[j] = "W"; break;
			case 360 : lonLabels[j] = " "; break;
			default  : lonLabels[j] = String.valueOf(i);
			}
			j++;
		}
		
		for(int count = 0; count < latLabels.length; count++)
			latLabels[count] = " ";
		
		j = 0;
		for(int i = -90; i <= 90; i += 5)
		{
			latRadians[j] = Math.toRadians(i);
			latLabels[j] = String.valueOf(i);
			j++;
		}
	}
		
	@Override
	public void onDraw(Canvas canvas)
	{
		//draw grid lines, then the chosen satellite(s) - how hard can this be...
		synchronized(this)
		{
			if(!mUpdatingDisplay)//TODO fix this - this line is bollocks
			{
				mUpdatingDisplay = true;
				
				int width  = getWidth();
				int height = getHeight();
				centreCoord.x = width/2;
				centreCoord.y = height/2;
				canvas.drawCircle(centreCoord.x, centreCoord.y, 10, mCentreCirclePaint);
				
				drawLatLabels(canvas, width, height);
				drawLatGrid(canvas, width, height);
				
				drawLonLabels(canvas, width, height);
				drawLonGrid(canvas, width, height);
				mUpdatingDisplay = false;
			}
		}
	}
		
	private void drawLonLabels(Canvas canvas, int width, int height)
	{
		for(int lon = 0; lon < lonRadians.length; lon += 6)
		{
			setNextCoord(mOrientation[0],mOrientation[1], (float)lonRadians[lon],(float)0.0,currCoord);
			canvas.drawText(lonLabels[lon],currCoord.x, currCoord.y, mTextPaint1);
		}
	}	
	
	private void drawLonGrid(Canvas canvas, int width, int height)
	{
		for(int lon = 0; lon < lonRadians.length; lon += 6/6)
		{
			for(int lat = 0; lat < latRadians.length; lat += 2)
			{
				if(lat != 0)
				{
					prevCoord.x = currCoord.x;
					prevCoord.y = currCoord.y;
				}
				
				setNextCoord(mOrientation[0], mOrientation[1], (float)lonRadians[lon], (float)latRadians[lat], currCoord);
				
				if(lat != 0)
				{
					if( ((currCoord.x >= 0 && currCoord.x <=width) || (prevCoord.x >= 0 && prevCoord.x <=width)) 
							&& ((currCoord.y >= 0 && currCoord.y <= height) || (prevCoord.y >=0 && prevCoord.y <= height)) )
					{
						canvas.drawLine(prevCoord.x, prevCoord.y, currCoord.x, currCoord.y, mLonGridPaint);
						//canvas.drawLine(currCoord.x, currCoord.y, prevCoord.x, prevCoord.y, mLonGridPaint);
					}
				}
			}
		}
	}
	
	private void drawLatLabels(Canvas canvas, int width, int height)
	{
		for( int lat = 0; lat < latRadians.length; lat+= 6)
		{
			for(int lon = 0; lon < lonRadians.length; lon += (90/5))
			{
				setNextCoord(mOrientation[0], mOrientation[1], (float)lonRadians[lon], (float)latRadians[lat], currCoord);
				canvas.drawText(latLabels[lat], currCoord.x, currCoord.y, mTextPaint);
			}
		}
	}
	
	private void drawLatGrid(Canvas canvas, int width, int height)
	{
		for(int lat = 0; lat < latRadians.length; lat += 6)
		{
			for(int lon = 0; lon < lonRadians.length; lon += 1)
			{
				if(lon != 0)
				{
					prevCoord.x = currCoord.x;
					prevCoord.y = currCoord.y;
				}
				
				setNextCoord(mOrientation[0], mOrientation[1], (float)lonRadians[lon], (float)latRadians[lat], currCoord);
				
				if(lon != 0)
				{
					if( ((currCoord.x >= 0 && currCoord.x <=width) || (prevCoord.x >= 0 && prevCoord.x <=width)) 
							&& ((currCoord.y >= 0 && currCoord.y <= height) || (prevCoord.y >=0 && prevCoord.y <= height)) )
					{
						canvas.drawLine(prevCoord.x, prevCoord.y, currCoord.x, currCoord.y, mLatGridPaint);
					}
				}
			}
		}
	}
	
	
	/**
	 * Pulled from DroidSat - calculates coord to draw at
	 * will redo myself once I've got the view working properly/ need something different
	 * 
	 * @param lonCentre	heading or longitude of centre of projection
	 * @param latCentre	pitch or latitude of centre of projection
	 * @param lonProj	azimuth or longitude of point to be projected
	 * @param latProj	elevation or latitude of point to be projected
	 * @param coord		re-usable coord object to hold results.
	 */
	private void setNextCoord(float lonCentre, float latCentre, float lonProj, float latProj, Coord coord)
	{
		if (mOrientation[0] == 0){
			mOrientation[0] = 0.001f;
		}
		if (mOrientation[1] == 0){
			mOrientation[1] = 0.001f;
		}
		
		double k = (2 * 480)
		/ (1 + Math.sin(mOrientation[1]) * Math.sin(latProj) + Math.cos(mOrientation[1])
				* Math.cos(latProj) * Math.cos(lonProj - lonCentre));

		coord.x = (float) (k * Math.cos(lonProj) * Math.sin(lonProj - lonCentre))
				+ getWidth() / 2;
		coord.y = (float) (k * (Math.cos(mOrientation[1]) * Math.sin(lonProj) - Math.sin(mOrientation[1])
				* Math.cos(latProj) * Math.cos(lonProj - lonCentre)))
				* -1 + getHeight() / 2;
	}

	
 

}