package uk.roryHughes.androidSeesStars;


public class Viewpoint
{	
	public static final double rEarth = 6378.1; //average radius in KM (cheap and cheerful from google)
	 //use better model of earth than assuming sphere time once working
	
	private float[] mOrientation= new float[3];
	private double mLat			= Double.NaN;
	private double mLon			= Double.NaN;
	private double mHeading 	= Double.NaN;	//rads
	private double mElevation	= Double.NaN;
	
	public Viewpoint()
	{
	}
	
	public void setOrientation(float[] _orientation)
	{
		/* also calcs heading & elevation when orientation changes*/
		
		mOrientation = _orientation;
		
		//get heading from orientation sensors		
		//convert from PI/2 = west, -PI/2 = east to PI/2 = west, 3PI/2 = east etc.
		//makes calcs simpler
		if(Double.compare(mOrientation[0], 0) < 0)
		{
			mHeading = 2*Math.PI + mOrientation[0];
		}
		else
		{
			mHeading = mOrientation[0];
		}
		
		//mOrietnation[2]*-1 so using +ve numbers for "infront" of user, makes it easier for me to think!
		//TODO - check this is ok? might be messing up calculations
		mElevation = mOrientation[2]*-1;
	}
	
	public float[] getOrientation()
	{
		return this.mOrientation;
	}
	
	public void setLat(double _lat)
	{
		mLat = _lat;
	}
	
	public double getLat()
	{
		return mLat;
	}
	
	public void setLon(double _lon)
	{
		mLon = _lon;
	}
	
	public double getLon()
	{
		return mLon;
	}

	public double getHeading()
	{
		return mHeading;
	}
	
	public double getElevation()
	{
		return mElevation;
	}

}
