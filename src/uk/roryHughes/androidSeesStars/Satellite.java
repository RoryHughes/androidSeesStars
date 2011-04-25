package uk.roryHughes.androidSeesStars;

import android.util.Log;


/**
 * @author Rory
 *
 * Class to hold parsed TLE data etc for a satellite
 *
 */
public class Satellite
{
	
	//TODO add getter and setter methods (& make vars private (maybe))
	//like this because I need to get on with other stuff and save some time
	//should use a better method than setter array
	
	private static final String TAG = "Satellite";
	
	public String line1;
	public String satNumLine1;
	public String classification;
	public String idYear;
	public String idLaunchNum;
	public String idLaunchPiece;
	public String epochYear;
	public String epochDay;
	public String firstTimeDerivMeanMotion;
	public String secondTimeDerivMeanMotion;
	public String bstarDragTerm;
	public String ephemerisType;
	public String elementNumber;
	public String checkSumLine1;
	public String[] line1Arr = new String[] {line1, satNumLine1, classification, idYear, idLaunchNum, idLaunchPiece, epochYear, epochDay,
								firstTimeDerivMeanMotion, secondTimeDerivMeanMotion, bstarDragTerm, ephemerisType, elementNumber, checkSumLine1};
								//convinece for operations on all vars - get rid of at end of testing if not needed
	
	public String line2;
	public String satNumLine2;
	public String inclination;
	public String rightAscensionAscendingNode; //degrees
	public String eccentricity;
	public String argOfPedigree;
	public String meanAnomaly;
	public String meanMotion;
	public String revNumAtEpoch;
	public String checkSumLine2;
	public String[] line2Arr = new String[] {line2, satNumLine2, inclination, rightAscensionAscendingNode, eccentricity, argOfPedigree, meanAnomaly,
								meanMotion, revNumAtEpoch, checkSumLine2};
								//convinece for operations on all vars - get rid of at end of testing if not needed
	
	private double mLat = Double.NaN;
	private double mLon = Double.NaN;
	private double mHeight = Double.NaN; 
	
	public Satellite()
	{
		
	}
	
	public double getSatHeight()
	{
		return 229.5;
		//mid height of operating orbit of ISS
		//TODO calculate & return current height
		//return this.mHeight;
	}
	
	public double getSatLat()
	{
		return 41.90368244;
		//lat of vatican city (had to pick somewhere to test!)
		//TODO calculate & return current lat
		//return this.mLat;
	}
	
	public double getSatLon()
	{
		return 12.45334625;
		//lon of vatican city (had to pick somewhere to test!)
		//TODO calculate & return current lon
		//return this.mLon;
	}
	
	public void setUp(String[] arr1, String[] arr2)
	{
		for(int i = 0; i < line1Arr.length; i++)
		{
			arr1[i] = arr1[i].trim();
			line1Arr[i] = arr1[i];
		}
		for(int j = 0; j < line2Arr.length; j++)
		{
			arr2[j] = arr2[j].trim();
			line2Arr[j] = arr2[j];
		}
		
		calcPos();
		
		Log.d(TAG, "Setup finished!");
	}
	
	private void calcPos()
	{
		//TODO calculate lat/lon from TLE data
	}
	
	public void showData()
	{
		Log.d(TAG, "**Line 1**");
		for(String data : line1Arr)
		{
			Log.d(TAG, data);
		}
		Log.d(TAG, "**Line 2**");
		for(String data : line2Arr)
		{
			Log.d(TAG, data);
		}
	}
	
}
