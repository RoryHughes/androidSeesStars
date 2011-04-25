package uk.roryHughes.androidSeesStars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

public class TLEParser
{
	//TODO IMPORTANT change to setter methods to update Satellite when Satellite sorted
	
	public static final String TAG = "TLEParser";
	
	private Context context;
	public StringBuilder fileContent;
	
	private int gotSat = -1;
	private String TLELine1 = null;
	private String TLELine2 = null;
	
	//tle fields
	private String line1;
	private String satNumLine1;
	private String classification;
	private String idYear;
	private String idLaunchNum;
	private String idLaunchPiece;
	private String epochYear;
	private String epochDay;
	private String firstTimeDerivMeanMotion;
	private String secondTimeDerivMeanMotion;
	private String bstarDragTerm;
	private String ephemerisType;
	private String elementNumber;
	private String checkSumLine1;
	private String[] line1Arr;
	
	private String line2;
	private String satNumLine2;
	private String inclination;
	private String rightAscensionAscendingNode; //degrees
	private String eccentricity;
	private String argOfPedigree;
	private String meanAnomaly;
	private String meanMotion;
	private String revNumAtEpoch;
	private String checkSumLine2;
	private String[] line2Arr;
	
	
	
	public TLEParser(Context _context)
	{
		this.context = _context;
		
	}
	
	public void parseTLE(String fileName, String satName, Satellite satellite)
	{
		if(getTLELines(fileName, satName))
		{
			parseLine1(TLELine1);
			parseLine2(TLELine2);
			
			line1Arr= new String[] {line1, satNumLine1, classification, idYear, idLaunchNum, idLaunchPiece, epochYear, epochDay,
					firstTimeDerivMeanMotion, secondTimeDerivMeanMotion, bstarDragTerm, ephemerisType, elementNumber, checkSumLine1};
			line2Arr = new String[] {line2, satNumLine2, inclination, rightAscensionAscendingNode, eccentricity, argOfPedigree, meanAnomaly,
					meanMotion, revNumAtEpoch, checkSumLine2};
			
			satellite.setUp(line1Arr, line2Arr);
			
		}
		else
		{
			//not got TLEs, handle error
		}
	}
	
	private void parseLine1(String line)
	{
		char[] charArr;
		//TODO change this so I dont create new char[] for each part
		
		charArr = new char[1];
		line.getChars(0, 1, charArr, 0);
		line1 = String.copyValueOf(charArr);
		
		charArr = new char[5];
		line.getChars(2, 7, charArr, 0);
		satNumLine1 = String.copyValueOf(charArr);
		
		charArr = new char[1];
		line.getChars(7, 8, charArr, 0);
		classification = String.copyValueOf(charArr);
		
		charArr = new char[2];
		line.getChars(9, 11, charArr, 0);
		idYear = String.copyValueOf(charArr);
		
		charArr = new char[3];
		line.getChars(11, 14, charArr, 0);
		idLaunchNum = String.copyValueOf(charArr);
	
		charArr = new char[3];
		line.getChars(14, 17, charArr, 0);
		idLaunchPiece = String.copyValueOf(charArr);
		
		charArr = new char[2];
		line.getChars(18, 20, charArr, 0);
		epochYear = String.copyValueOf(charArr);
		
		charArr = new char[12];
		line.getChars(20, 32, charArr, 0);
		epochDay = String.copyValueOf(charArr);
		
		charArr = new char[10];
		line.getChars(33, 43, charArr, 0);
		firstTimeDerivMeanMotion = String.copyValueOf(charArr);
		
		charArr = new char[8];
		line.getChars(44, 52, charArr, 0);
		secondTimeDerivMeanMotion = String.copyValueOf(charArr);
		
		charArr = new char[8];
		line.getChars(53, 61, charArr, 0);
		bstarDragTerm = String.copyValueOf(charArr);
		
		charArr = new char[1];
		line.getChars(62, 63, charArr, 0);
		ephemerisType = String.copyValueOf(charArr);
		
		charArr = new char[4];
		line.getChars(64, 68, charArr, 0);
		elementNumber = String.copyValueOf(charArr);
		
		charArr = new char[1];
		line.getChars(68, 69, charArr, 0);
		checkSumLine1 = String.copyValueOf(charArr);
	}
	
	private void parseLine2(String line)
	{
		char[] charArr;
		//TODO change this so I dont create new char[] for each part
		
		charArr = new char[1];
		line.getChars(0, 1, charArr, 0);
		line2 = String.copyValueOf(charArr);
		
		charArr = new char[5];
		line.getChars(2, 7, charArr, 0);
		satNumLine2 = String.copyValueOf(charArr);
		
		charArr = new char[8];
		line.getChars(8, 16, charArr, 0);
		inclination = String.copyValueOf(charArr); //degrees
		
		charArr = new char[8];
		line.getChars(17, 25, charArr, 0);
		rightAscensionAscendingNode = String.copyValueOf(charArr);
		
		charArr = new char[7];
		line.getChars(26, 33, charArr, 0);
		eccentricity = String.copyValueOf(charArr);

		charArr = new char[8];
		line.getChars(34, 42, charArr, 0);
		argOfPedigree = String.copyValueOf(charArr);

		charArr = new char[8];
		line.getChars(43, 51, charArr, 0);
		meanAnomaly = String.copyValueOf(charArr);
		
		charArr = new char[11];
		line.getChars(52, 63, charArr, 0);
		meanMotion = String.copyValueOf(charArr);
		
		charArr = new char[5];
		line.getChars(63, 68, charArr, 0);
		revNumAtEpoch = String.copyValueOf(charArr);
		
		charArr = new char[1];
		line.getChars(68, 69, charArr, 0);
		checkSumLine2 = String.copyValueOf(charArr);
	}
	
	private boolean getTLELines(String fileName, String satName)
	{
		Log.d(TAG, "parsing satellite  "+satName+"  from  "+fileName);
				
		String FILENAME = fileName;
		try
		{
			InputStream in = context.openFileInput(FILENAME);
			InputStreamReader inReader = new InputStreamReader(in);
			BufferedReader bIn = new BufferedReader(inReader);
			
			String line;
			
			try
			{
				while( ( line = bIn.readLine()) != null )
				{
					if(line.contains(satName))
					{
						gotSat = 0;
					}
					switch(gotSat)
					{
					case 0 : gotSat++; break;
					case 1 : TLELine1 = line; gotSat++; break;
					case 2 : TLELine2 = line; gotSat++; break;
					}
					if(gotSat == 3)
						break;
				}
				if (in != null)
					in.close();
			}
			catch(IOException e)
			{
				Log.d(TAG, "caused " +e);
				//TODO handle this properly
			}
		}
		catch(FileNotFoundException e)
		{
			Log.d(TAG, "caused " +e);
			//TODO handle this properly
		}
		
		Log.d(TAG, "line 1 : "+TLELine1);
		Log.d(TAG, "line 2 : "+TLELine2);
		
		//TODO checksum for line1 & line2, only return true if checksum ok
		
		if( (TLELine1 != null)&&(TLELine2 != null) )
			return true;
		else
			return false;
		
		
	}
	
}
