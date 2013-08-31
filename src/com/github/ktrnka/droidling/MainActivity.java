package com.github.ktrnka.droidling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Formatter;

import com.actionbarsherlock.app.SherlockActivity;
import com.github.ktrnka.droidling.R;
import com.github.ktrnka.droidling.helpers.AsyncDrawable;
import com.github.ktrnka.droidling.helpers.BitmapLoaderTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity
	{
	public static final String PACKAGE_NAME = "com.github.ktrnka.droidling";
	
	public static final boolean DEVELOPER_MODE = true;
	
	public static final String TAG = "com.github.ktrnka.droidling.MainActivity";
	
	private static final long VERSION_NOT_FOUND = -1;
	
	public void onCreate(Bundle savedInstanceState)
		{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.image_main);
		loadContactPhotos();
		
		checkShowWhatsNew();
		}
	
	public void onStart()
		{
		super.onStart();
		
		loadProfilePhoto();
		}

	// TODO: run this as an asynctask
	private void loadContactPhotos()
	    {
	    TableLayout photoTable = (TableLayout) findViewById(R.id.interpersonalTable);
	    if (photoTable == null)
	    	{
	    	Log.e(TAG, "Failed to find interpersonal table");
	    	return;
	    	}
	    
	    photoTable.setOnClickListener(new OnClickListener()
			{
			public void onClick(View v)
	            {
	            Intent intent = new Intent(MainActivity.this, InterpersonalActivity.class);
				startActivity(intent);
	            }
			});
	    
	    final int COLUMNS = 3;
	    final int ROWS = 3;
	    int desiredImages = COLUMNS * ROWS;
	    String[] photoUris = new String[desiredImages];
	    int numImages = 0;
	    
	    final String[] projection = new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Contacts.PHOTO_URI, Contacts.PHOTO_THUMBNAIL_URI, Contacts.TIMES_CONTACTED };
	    final String selection = Contacts.PHOTO_URI + "!=? AND " + Contacts.TIMES_CONTACTED + ">?";
	    final String[] selectionArgs = new String[]{ "null", "0" };
	    final Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI, projection, selection, selectionArgs, Contacts.TIMES_CONTACTED + " DESC");
	    if (cursor.moveToFirst())
	    	{
	    	final int PHOTO_COL = cursor.getColumnIndex(Contacts.PHOTO_URI);
	    	final int NAME_COL = cursor.getColumnIndex(Contacts.DISPLAY_NAME);
	    	final int TIMES_CONTACTED_COL = cursor.getColumnIndex(Contacts.TIMES_CONTACTED);
	    	
	    	do {
	    		String name = cursor.getString(NAME_COL);
	    		int timesContacted = cursor.getInt(TIMES_CONTACTED_COL);
	    		String photoUri = cursor.getString(PHOTO_COL);
	    		
	    		photoUris[numImages++] = photoUri;
	    		if (numImages >= desiredImages)
	    			break;
	    		
	    		//Log.i(TAG, name + ", contacted " + timesContacted + " times, " + photoUri);
	    		} while (cursor.moveToNext());
	    	}
	    cursor.close();
	    
	    Resources res = getResources();
		int imageSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, res.getDimension(R.dimen.home_imagebutton_small_size), res.getDisplayMetrics());
	    
	    int photoIndex = 0;
	    ImageAdapter adapter = new ImageAdapter((ExtendedApplication) getApplication(), photoUris, imageSize);
	    for (int row = 0; row < ROWS; row++)
	    	{
	    	TableRow tableRow = new TableRow(this);
	    	for (int col = 0; col < COLUMNS; col++)
	    		{
	    		View photoView = adapter.getView(photoIndex, null, tableRow);
	    		
	    		TableRow.LayoutParams params = new TableRow.LayoutParams();
	    		params.width = imageSize;
	    		params.height = imageSize;
	    		
	    		tableRow.addView(photoView, params);
	    		photoIndex++;
	    		}
	    	photoTable.addView(tableRow);
	    	}
	    }

	@SuppressLint("NewApi")
	private void loadProfilePhoto()
	    {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return;
		
		ImageView profileButton = (ImageView) findViewById(R.id.personalImageButton);
		if (profileButton == null)
			{
			Log.e(TAG, "Failed to find ImageButton");
			return;
			}
		
		profileButton.setOnClickListener(new OnClickListener()
			{
			public void onClick(View v)
	            {
	            Intent intent = new Intent(MainActivity.this, PersonalActivity.class);
				startActivity(intent);
	            }
			});
		
		Resources res = getResources();
		int imageSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, res.getDimension(R.dimen.home_imagebutton_size), res.getDisplayMetrics());

	    final String[] projection = new String[]{ Profile._ID, Profile.DISPLAY_NAME_PRIMARY, Profile.PHOTO_URI, Profile.PHOTO_THUMBNAIL_URI };
	    final Cursor cursor = getContentResolver().query(Profile.CONTENT_URI, projection, null, null, null);
	    Log.i(TAG, "Number of profile entries: " + cursor.getCount());
	    if (cursor.moveToFirst())
	    	{
	    	final int PHOTO_URI_COL = cursor.getColumnIndex(Profile.PHOTO_URI);
	    	
	    	String photoUri = cursor.getString(PHOTO_URI_COL);
	    	if (photoUri == null)
	    		{
	    		Log.e(TAG, "Profile photo URI is null!");
	    		cursor.close();
	    		return;
	    		}
	    	
			Log.d(TAG, "Photo uri: " + photoUri);
            try
	            {
	            setImage(profileButton, this, Uri.parse(photoUri), imageSize, imageSize);
	            }
            catch (IOException e)
	            {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	            }
	    	}
	    cursor.close();
	    }
	
	private void setImage(ImageView imageView, Context context, Uri imageUri, int width, int height) throws IOException
		{
		if (!BitmapLoaderTask.cancelPotentialWork(imageView, imageUri))
			{
			BitmapLoaderTask task = new BitmapLoaderTask(imageView, width, height, (ExtendedApplication) getApplication());
			AsyncDrawable placeholder = new AsyncDrawable(context.getResources(), null, task);
			imageView.setImageDrawable(placeholder);
			task.execute(imageUri);
			}
		}

	/**
	 * Check if this is first install, app update, normal load and show
	 * What's New dialog if appropriate.
	 */
	private void checkShowWhatsNew()
	    {
	    try
			{
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			
			long lastRunVersion = prefs.getLong("lastRunVersionCode", VERSION_NOT_FOUND);
			
			// show the what's new dialog - updates only, not first install
			if (lastRunVersion != VERSION_NOT_FOUND && lastRunVersion < packageInfo.versionCode)
				showWhatsNew();

			// update the stored version - new install and updates
			if (lastRunVersion != packageInfo.versionCode)
				{
				// update the preference
				SharedPreferences.Editor editor = prefs.edit();
				editor.putLong("lastRunVersionCode", packageInfo.versionCode);
				editor.commit();
				}
			}
		catch (PackageManager.NameNotFoundException exc)
			{
			// This error shouldn't be possible; I don't know an accurate way to test it.
			Log.e(TAG, "PackageManager lookup failed");
			Log.e(TAG, Log.getStackTraceString(exc));
			}
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
		{
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.new_help, menu);
		return true;
		}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
		{
		switch (item.getItemId())
			{
			case R.id.helpMenu:
				Intent intent = new Intent(this, AboutActivity.class);
				startActivity(intent);
				break;
			case R.id.rateMenu:
				rateApp();
				break;
			case R.id.feedbackMenu:
				sendFeedback();
				break;
			default:
				Log.e(TAG, "Undefined menu item selected");
			}
		return false;
		}

	/**
	 * Show the What's New dialog.
	 */
	private void showWhatsNew()
		{
		new AsyncTask<Void,Void,CharSequence>()
			{
			@Override
            protected CharSequence doInBackground(Void... params)
                {
                StringBuilder builder = new StringBuilder();
				try
					{
	                BufferedReader in = new BufferedReader(new InputStreamReader(getAssets().open("changelog.txt")));
					String line;
					
					while ((line = in.readLine()) != null && !isCancelled())
						{
						builder.append(line);
						builder.append('\n');
						}
					in.close();
					}
				catch (IOException e)
					{
					return null;
					}
				
				return builder;
                }
			
			@Override
			protected void onPostExecute(CharSequence result)
				{
				if (isCancelled() || result == null)
					return;
				
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
				alertBuilder.setTitle("What's New");
				alertBuilder.setMessage(result);
				alertBuilder.setIcon(android.R.drawable.ic_menu_help);

				alertBuilder.setPositiveButton("Close", null);
				alertBuilder.show();
				}
			
			}.execute();
		}
	
	private void rateApp()
		{
		this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME)));
		}

	/**
	 * Email feedback to the development account.  Note that most of the strings
	 * aren't from strings.xml, because we don't need to localize them (cause I need to be able to read them)
	 */
	public void sendFeedback()
		{
		// special case for the feedback option
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("message/rfc822");
		sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.developer_email) });
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on " + getString(R.string.app_name));
		
		// read the config and make it pretty
		Configuration config = getResources().getConfiguration();
		StringBuilder configBuilder = new StringBuilder();

		configBuilder.append("Locale: " + config.locale.toString() + "\n");
		
		configBuilder.append("Size: ");
		switch (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
		{
		case Configuration.SCREENLAYOUT_SIZE_SMALL:
			configBuilder.append("small\n");
			break;
		case Configuration.SCREENLAYOUT_SIZE_NORMAL:
			configBuilder.append("normal\n");
			break;
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
			configBuilder.append("large\n");
			break;
		default:
			configBuilder.append("unknown (" + config.screenLayout + ")\n");
			break;
		}
		
		configBuilder.append("Model: " + android.os.Build.MODEL + "\n");
		
		configBuilder.append("Android version " + android.os.Build.VERSION.RELEASE + "\n");
		configBuilder.append("SDK version " + android.os.Build.VERSION.SDK_INT + "\n\n");
		
		// application information
		try
			{
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			configBuilder.append(getString(packageInfo.applicationInfo.labelRes) + " Version " + packageInfo.versionName + " (" + packageInfo.versionCode + ")\n");
			}
		catch (PackageManager.NameNotFoundException exc)
			{
			configBuilder.append("Version unknown\n");
			}
		
		String profiling = summarizeRuntime(getApplicationContext(), PersonalActivity.PROFILING_KEY_ORDER);
		if (profiling != null)
			{
			configBuilder.append("\nPersonal Stats Runtime Profiling (Last Run):\n");
			configBuilder.append(profiling);
			}

		profiling = summarizeRuntime(getApplicationContext(), InterpersonalActivity.PROFILING_KEY_ORDER);
		if (profiling != null)
			{
			configBuilder.append("\n\nInterpersonal Stats Runtime Profiling (Last Run):\n");
			configBuilder.append(profiling);
			}

		profiling = summarizeRuntime(getApplicationContext(), LanguageIdentificationActivity.PROFILING_KEY_ORDER);
		if (profiling != null)
			{
			configBuilder.append("\n\nLanguage Identification Runtime Profiling (Last Run):\n");
			configBuilder.append(profiling);
			}

		sendIntent.putExtra(Intent.EXTRA_TEXT, configBuilder.toString());
		
		startActivity(Intent.createChooser(sendIntent, getString(R.string.send_email_with)));
		}

	public static String summarizeRuntime(Context context, String[] PROFILING_KEYS)
		{
		StringBuilder computeBuilder = new StringBuilder();
		Formatter f = new Formatter(computeBuilder);
		double totalSeconds = 0;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		for (String key : PROFILING_KEYS)
			{
			long value = prefs.getLong(key, -1);
			
			key = key.replaceAll(".*:\\s*", "");
			
			// doesn't really need a localization; it's only for me
			f.format("%s: %.1fs\n", key, value / 1000.0);
			totalSeconds += value / 1000.0;
			}
		f.format("Total: %.1fs", totalSeconds);
		return computeBuilder.toString();
		}
	}