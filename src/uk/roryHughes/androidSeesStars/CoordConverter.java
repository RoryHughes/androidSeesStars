package uk.roryHughes.androidSeesStars;

import uk.me.chiandh.Sputnik.Times;
import android.util.Log;

public class CoordConverter
{
	private static final String TAG = "CoordConverter";
	
	private static final double a 	  = 6378137;
	private static final double b 	  = 6356752.31424518;
	private static final double e 	  = Math.sqrt( (Math.pow(a,2) - Math.pow(b,2))/Math.pow(a,2) );
	private static final double eDash = Math.sqrt( (Math.pow(a,2) - Math.pow(b,2))/Math.pow(b,2) );
	
	private Times mTimes;
	
	private double[][] ECItoECFtransform = new double[3][3];
	
	private double n 	 = Double.NaN;
	private double theta = Double.NaN;
	private double p	 = Double.NaN;
	
	private double[] mECF = new double[3];
	private double[] mLLA = new double[3];
	
	private double siderealTime = Double.NaN;
	
	private int temp = 0;
	
	public CoordConverter()
	{
		mTimes = new Times();
		mTimes.Init();
	}
	
	public double[] ECItoLLA(double[] _eci)
	{		
		mTimes.Init();
		siderealTime = mTimes.GetGST();
		
		if(temp == 50)
		{
			temp = 0;
			Log.d(TAG, "sidereal time = "+siderealTime);
		}
		temp++;
		
		ECItoECFtransform[0][0] = Math.cos(siderealTime);
		ECItoECFtransform[0][1] = Math.sin(siderealTime);
		ECItoECFtransform[0][2] = 0;
		ECItoECFtransform[1][0] = -Math.sin(siderealTime);
		ECItoECFtransform[1][1] = Math.cos(siderealTime);
		ECItoECFtransform[1][2] = 0;
		ECItoECFtransform[2][0] = 0;
		ECItoECFtransform[2][1] = 0;
		ECItoECFtransform[2][2] = 1;
								         
		mECF[0] = ECItoECFtransform[0][0]*_eci[0] + ECItoECFtransform[0][1]*_eci[1] + ECItoECFtransform[0][2]*_eci[2];
		mECF[1] = ECItoECFtransform[1][0]*_eci[0] + ECItoECFtransform[1][1]*_eci[1] + ECItoECFtransform[1][2]*_eci[2];
		mECF[2] = ECItoECFtransform[2][0]*_eci[0] + ECItoECFtransform[2][1]*_eci[1] + ECItoECFtransform[2][2]*_eci[2];
		
		p = Math.sqrt( (Math.pow(mECF[0],2))+(Math.pow(mECF[1],2)) );
		theta = Math.atan( (mECF[2]*a)/(p*b) );
		
		mLLA[1] = Math.atan(mECF[1]/mECF[0]);
		
		mLLA[0] = Math.atan( ( mECF[2] + Math.pow(eDash, 2) * b * sinCubed(theta) ) //no sin^3(theta) function - using derivative
				         / ( p - Math.pow(e,2) * a *  (0.25 * cosCubed(theta))));    //no cos^3(theta) function - using derivative
		
		n = a / (Math.sqrt( 1 - Math.pow(e,2) * (0.5*(1 - Math.cos(2*mLLA[2]))))); //no sin^2 function - using derivative
		
		mLLA[2] = ( p/ Math.cos(mLLA[2])) - n;
		
		//alt includes earth radius (i think) remove rEarth then return
		//TODO - check this and do it better
		
		return mLLA;
	}
	
	
	private static double sinCubed(double x)
	{
		return 0.25 * ( (3*Math.sin(x)) - Math.sin(3*x));
		
		//return Math.pow((-Math.cos((Math.PI/2)+x)),3);
	}
	
	private static double cosCubed(double x)
	{
		return 0.25 * ( (3*Math.cos(x)) + Math.cos(3*x) );
  	}
	
}
