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
import android.util.Log;

public class TLEDownloader
{
	//private final String PATH = "/data/data/uk.roryHughes.adroidSeesStars/";
		//file location
	
	/**downloader method */
	public void downloadFromUrl(String TLEUrl, String fileName, Context context)
	{
		try
		{
			URL url   = new URL(TLEUrl);
			//File file = new File(fileName);
			
			long startTime = System.currentTimeMillis();
			Log.d("TLEDownloader", "Start download from "+TLEUrl);
			
			//open url connection
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
				FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
				fos.write(bab.toByteArray());
				fos.close();
				Log.d("TLEDownloader", "Download attempt took "+((System.currentTimeMillis() - startTime)/1000)+" sec");
			}
			catch(FileNotFoundException e)
			{
				Log.d("TLEDownloader", "error "+e);
			}
		}
		catch(IOException e)
		{
			Log.d("TLEDownloader", "Error: "+e);
		}
	}
}