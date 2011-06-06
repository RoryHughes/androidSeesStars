package uk.roryHughes.androidSeesStars;

import android.util.Log;

public class TLEParser
{
	public static final String TAG = "TLEParser";
	
	public TLEParser()
	{
		
	}
	
	public Satellite parseTLE(String fileName, String satName, Satellite satellite)
	{
		try
		{
			Log.v(TAG, "parsing '"+fileName+" for '"+satName);
			satellite.mSDP4.NoradByName(fileName, satName);
		}
		catch(Exception e)
		{
			Log.e(TAG, "caused "+e);
			e.printStackTrace();
			return null;
		}
		Log.v(TAG, "parsed with SDP4");
		return satellite;
	}
}
