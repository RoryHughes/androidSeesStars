package uk.roryHughes.androidSeesStars;

import uk.me.chiandh.Lib.SDP4;
import android.text.format.Time;
import android.util.Log;


/**
 * @author Rory Hughes
 *
 * Class to represent a satellite
 * contains instance of SDP4 class (Which I take no credit for) to do some maths on TLE data
 *
 */
public class Satellite
{	
	//TODO - put calcs into seperate class
	
	private static final String TAG = "Satellite";
	public static final double rEarth = 6378.1; //average radius in KM (cheap and cheerful from google)
	 //use better model of earth than assuming sphere time once working

	private double[] mLLA = new double[3];
	private CoordConverter coordConverter;
	public SDP4 mSDP4;  //instance of SDP4 class to calculate satellites position etc.	
	private Time mTime;
	
	private int temp = 50;
	
	public Satellite()
	{
		coordConverter = new CoordConverter();
		
		mTime = new Time();
		mSDP4 = new SDP4();
		mSDP4.Init();
	}
	
	public SDP4 getSDP4()
	{
		return mSDP4;
	}
	
	public double getAltitude()
	{
		//mid height of operating orbit of ISS
		//mLLA[2] = 229.5;
		
		
		return mLLA[2];
	}
	
	public double getLat()
	{
		//lat of vatican city (had to pick somewhere to test!)
		//mLLA[0] = 41.90368244;
		return mLLA[0];
		
	}
	
	public double getLon()
	{
		//lon of vatican city (had to pick somewhere to test!)
		//mLLA[1] = 12.45334625;
		
		return mLLA[1];
	}
	
	public void calcPos()
	{		
		mSDP4.GetPosVel(Time.getJulianDay(mTime.toMillis(false), mTime.gmtoff)-2450000); //is this whats making it go wrong?
		// getPosVel argument to julian day - 2450000 (format required by SDP4.java)
		
		//itsR is ECI coord - convert to ECF then to LLA
		mLLA = coordConverter.ECItoLLA(mSDP4.itsR);
		
		if(temp == 50)
		{
			Log.d(TAG, "pos vector = "+ mSDP4.itsR[0] +","+ mSDP4.itsR[1] +","+mSDP4.itsR[2]);
			Log.d(TAG, "lat = "+ Math.toDegrees(mLLA[0]) +", Lon = "+ Math.toDegrees(mLLA[1]) +", Alt = "+mLLA[2]);
			temp = 0;
		}
		temp++;
		
	}
	
	public String getName()
	{
		return this.mSDP4.itsName;
	}
}
