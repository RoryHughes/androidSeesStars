package uk.roryHughes.androidSeesStars;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class TLEDownloader
{
	private static final String TAG = "TLEDownloader";
	
	private Context mContext;
	private boolean success = false;
	private int attempts = 0;
	
	public TLEDownloader(Context context)
	{
		this.mContext = context;
	}
	
	
	/**
	 *  downlods TLE Set of files from given url
	 *  
	 *  @param files String Array holding file names of TLEs
	 *  @param prefix String of the directory the fiels are stored at (URL)
	 *  @param conManager ConnectivityManager for connection
	 *  
	 *  @return returns true if download successfull
	 */
	public boolean downloadTLESet(String[] files, String prefix, ConnectivityManager conManager)
	{
		success = false;
		if(conManager.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED || 
        	conManager.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED )
        {
	        success = true;
			for(String tle : files)
	        {
	        	for(attempts = 0; attempts <= 2; attempts++)
	        	{
	        		if(downloadFromUrl(prefix+tle+".txt", tle+".txt", mContext))
	        		{
	        			attempts = 3;
	        		}
	        		else
	        		{
	        			if(attempts == 2)
	        				success = false;
	        		}
	        	}
	        }
        }
		return success;
	}
	
	/**downloader method */
	private boolean downloadFromUrl(String TLEUrl, String fileName, Context _context)
	{
		
		try
		{
			Log.d(TAG, "Downloading "+ TLEUrl +" to "+ fileName);
			URL url   = new URL(TLEUrl);
			URLConnection conn = url.openConnection();
			
			//define input streams to read from URLConnection
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			
			//read bytes
			ByteArrayBuffer bab = new ByteArrayBuffer(50);
			int curr = 0;
			while((curr = bis.read()) != -1)
			{
				bab.append((byte) curr);
			}
			
			//convert bytes to string
			try
			{
				FileOutputStream fos = _context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
				fos.write(bab.toByteArray());
				fos.close();
				return true;
			}
			catch(FileNotFoundException e)
			{
				Log.d("TLEDownloader", "error "+e);
				return false;
			}
		}
		catch(IOException e)
		{
			Log.d("TLEDownloader", "Error: "+e);
			return false;
		}
	}
}
