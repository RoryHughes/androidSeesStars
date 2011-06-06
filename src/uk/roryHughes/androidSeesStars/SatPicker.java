package uk.roryHughes.androidSeesStars;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SatPicker extends Activity
{
	private static final String TAG = "SatPicker";
	
	private LinearLayout list;
	private ArrayList<String> file;
	private ArrayList<Button> buttons;
	private ArrayList<OnClickListener> clickListeners;
	private LayoutParams params;
	private int k = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sat_picker);
		list = (LinearLayout)findViewById(R.id.sat_list);
		file = new ArrayList<String>();
		buttons = new ArrayList<Button>();
		
		Intent intent = getIntent();
		file = intent.getStringArrayListExtra("file");
		
		Button b;
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
		Log.d(TAG, "File length = "+ file.size());
		
		for(int i = 1; i <file.size(); i++)
		{
			b = new Button(this);
			b.setLayoutParams(params);
			b.setText(file.get(i));
			b.setId(i);
			buttons.add(b);
			list.addView(b);
		}		
		
		clickListeners = new ArrayList<OnClickListener>();
		OnClickListener cl;
		
		for(k = 0; k<buttons.size(); k++)
		{
			cl = new OnClickListener()
			{
				int id = k;
				public void onClick(View v)
				{
					Log.d(TAG, "pressed button "+id);
					Log.d(TAG, "file name = "+file.get(0));
					Log.d(TAG, "sat name = "+file.get(id+1));
					Toast toast = Toast.makeText(getApplicationContext(), "Tracking "+file.get(id+1), Toast.LENGTH_LONG);
					toast.show();
					Intent i = new Intent();
					i.putExtra("sat_name", file.get(id+1));
					i.putExtra("file_name", file.get(0));
					setResult(RESULT_OK, i);
					finish();
				}
			};
			clickListeners.add(cl);
		}
		
		for(int x = 0; x<buttons.size(); x++)
		{
			buttons.get(x).setOnClickListener(clickListeners.get(x));
		}		
	}
}
