package com.qsp.player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.util.ByteArrayBuffer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class QspGameStock extends TabActivity {

	public class GameItem {
		//Parsed
		String game_id;
		String list_id;
		String author;
		String ported_by;
		String version;
		String title;
		String lang;
		String player;
		String file_url;
		int file_size;
		String desc_url;
		String pub_date;
		String mod_date;
		//Flags
		boolean downloaded;
		boolean checked;
		//Local
		String game_file;
		GameItem()
		{
			game_id = "";
			list_id = "";
			author = "";
			ported_by = "";
			version = "";
			title = "";
			lang = "";
			player = "";
			file_url = "";
			file_size = 0;
			desc_url = "";
			pub_date = "";
			mod_date = "";
			downloaded = false;
			checked = false;
			game_file = "";
		}
	}
	
	final private Context uiContext = this;
	private String xmlGameListCached;
	private boolean openDefaultTab;
	private boolean gameListIsLoading;
	private boolean triedToLoadGameList;
	private GameItem selectedGame;
	
	private boolean gameIsRunning;
    private boolean startingGSUp = true;

    public static final int MAX_SPINNER = 1024;
    public static final int DOWNLOADED_TABNUM = 0;
    public static final int STARRED_TABNUM = 1;
    public static final int ALL_TABNUM = 2;
    
    public static final String GAME_INFO_FILENAME = "gamestockInfo";
    
    private boolean isActive;
    private boolean showProgressDialog;
	
	private String _zipFile; 
	private String _location; 
	
    String					startRootPath;
    String					backPath;
    ArrayList<File> 		qspGamesBrowseList;
    ArrayList<File> 		qspGamesToDeleteList;

	SharedPreferences settings;
	String userSetLang;
	String curLang = Locale.getDefault().getLanguage();

	HashMap<String, GameItem> gamesMap;
	
	ListView lvAll;
	ListView lvDownloaded;
	ListView lvStarred;
    ProgressDialog downloadProgressDialog = null;
    Thread downloadThread = null;

	private NotificationManager mNotificationManager;
	private int QSP_NOTIFICATION_ID;
    
    public QspGameStock() {
    	Utility.WriteLog("[G]constructor\\");
    	gamesMap = new HashMap<String, GameItem>();
    }

	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		//orientationRecreate = true;
		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}
	}

	private void setGSLocale(String lang) {
		Locale myLocale;
		Utility.WriteLog("Base Language = "+lang);

        //if TAIWAN
		if (lang.equals("zh-rTW")) {
			myLocale = Locale.TAIWAN;
			Utility.WriteLog("Language = TAIWAN, "+lang);
		}
		//if CHINA
		else if (lang.equals("zh-rCN")) {
            myLocale = Locale.CHINA;
            Utility.WriteLog("Language = CHINA, "+lang);
        }
        //if lang doesn't contain a region code
        else if (!lang.contains("-r"))
            myLocale = new Locale(lang);
        //if lang is not TAIWAN, CHINA, or short, use country+region
        else {
			String myRegion = lang.substring(lang.indexOf("-r")+2);
			String myLang = lang.substring(0,2);
			Utility.WriteLog("Language = "+myLang+", Region = "+myRegion);
			myLocale = new Locale(lang,myRegion);
		}
		Resources newRes = getResources();
		DisplayMetrics dm = newRes.getDisplayMetrics();
		Configuration conf = newRes.getConfiguration();
		conf.locale = myLocale;
		newRes.updateConfiguration(conf, dm);
		setTitle(getString(R.string.menu_gamestock));
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
    	Utility.WriteLog("[G]onCreate\\");
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);

        Intent gameStockIntent = getIntent();
        gameIsRunning = gameStockIntent.getBooleanExtra("game_is_running", false);
        startingGSUp = true;
        
        isActive = false;
        showProgressDialog = false;
        
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        TabHost tabHost = getTabHost();
        LayoutInflater.from(getApplicationContext()).inflate(R.layout.gamestock, tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec("downloaded")
                .setIndicator(getResources().getString(R.string.tab_downloaded))
                .setContent(R.id.downloaded_tab));
        tabHost.addTab(tabHost.newTabSpec("starred")
                .setIndicator(getResources().getString(R.string.tab_starred))
                .setContent(R.id.starred_tab));
        tabHost.addTab(tabHost.newTabSpec("all")
                .setIndicator(getResources().getString(R.string.tab_all))
                .setContent(R.id.all_tab));
        
    	openDefaultTab = true;
    	gameListIsLoading = false;
    	triedToLoadGameList = false;
    	
    	xmlGameListCached = null;

    	InitListViews();
    	
    	setResult(RESULT_CANCELED);
        
        //TODO: 
        // 1v. Отображение статуса "Загружено", например цветом фона.
        // 2. Авто-обновление игр
        // 3. Кэширование списка игр
        // 4v. Доступ к играм, даже когда сервер недоступен
        // 5. Вывод игр в папке "Загруженные" в порядке последнего доступа к ним
        // 6v. Возможность открыть файл из любой папки(через специальное меню этой активити)
        // 7v. Доступ к настройкам приложения через меню этой активити
    	Utility.WriteLog("[G]onCreate/");
    }
    
    @Override
    public void onResume()
    {
		Utility.WriteLog("[G]onResume\\");

		//Set the language if it has changed
        boolean langChanged = false;
		userSetLang = settings.getString("lang","en");
		Utility.WriteLog("userSetLang = "+userSetLang+", curLang = "+curLang);
		if (!curLang.equals(userSetLang)) {
			Utility.WriteLog("GameStock:"+userSetLang+" <> "+curLang+", setting language");
			curLang = userSetLang;
            langChanged = true;
			setGSLocale(userSetLang);
            settings = PreferenceManager.getDefaultSharedPreferences(this);
			Utility.WriteLog(curLang+" <- "+userSetLang);
		} else
			Utility.WriteLog("GameStock:"+userSetLang+" == "+curLang+", no change");

    	super.onResume();
    	isActive = true;

        //Refresh QspGameStock if the user changed the language
        Utility.WriteLog("startingUpQSP: "+startingGSUp+", langChanged: "+langChanged);
        if ((!startingGSUp) && (langChanged)) {
            Utility.WriteLog("FreeResources*");
            Utility.WriteLog("FreeResources*");
            Utility.WriteLog("RECREATE*");
            setTitle(getString(R.string.menu_gamestock));
//            recreate();
            Utility.WriteLog("RECREATE/");
        }
        else startingGSUp = false;


        Utility.WriteLog("[G]onResume/");
    }
    
    @Override
    public void onPostResume()
    {
    	Utility.WriteLog("[G]onPostResume\\");
    	super.onPostResume();
		if (showProgressDialog && (downloadProgressDialog != null))
		{
			downloadProgressDialog.show();
		}
    	Utility.WriteLog("[G]onPostResume/");
    }
    
    @Override
    public void onPause() {
    	Utility.WriteLog("[G]onPause\\");
    	isActive = false;
		if (showProgressDialog && (downloadProgressDialog != null))
		{
			downloadProgressDialog.dismiss();
		}
    	super.onPause();
    	Utility.WriteLog("[G]onPause/");
    }

    @Override
    public void onDestroy()
    {
    	Utility.WriteLog("[G]onDestroy\\");
    	Utility.WriteLog("[G]onDestroy/");
    	super.onDestroy();
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        final String gameListToRecreatedActivity = xmlGameListCached;
        return gameListToRecreatedActivity;
    }

    @Override
    public void onNewIntent(Intent intent)
    {
    	Utility.WriteLog("[G]onNewIntent\\");
    	super.onNewIntent(intent);
    	Utility.WriteLog("[G]onNewIntent/");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Загружаем меню из XML-файла
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gamestock_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
    	//Прячем или показываем пункт меню "Продолжить игру"
		MenuItem resumeGameItem = menu.findItem(R.id.menu_resumegame);
		resumeGameItem.setVisible(gameIsRunning);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	//Контекст UI
        switch (item.getItemId()) {
            case R.id.menu_resumegame:
                //Продолжить игру
    			setResult(RESULT_CANCELED, null);
    			finish();
                return true;

            case R.id.menu_options:
                Intent intent = new Intent();
                intent.setClass(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.menu_about:
            	Intent updateIntent = null;
        		updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_details_url)));
        		startActivity(updateIntent); 
                return true;

            case R.id.menu_openfile:
            // ** original code for BrowseGame directory checking **
			//	BrowseGame(Environment.getExternalStorageDirectory().getPath(), true);

			// ** begin replacement code for checking storage directory **
				String strSDCardPath = System.getenv("SECONDARY_STORAGE");
								if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
					strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
				}
				BrowseGame(strSDCardPath, true);
			// ** end replacement code for checking storage directory **

                return true;

            case R.id.menu_deletegames:
            	DeleteGames();
                return true;

        }        
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	//Ловим кнопку "Back", и не закрываем активити, а только
    	//отправляем в бэкграунд (как будто нажали "Home"), 
    	//при условии, что запущен поток скачивания игры
        if ((downloadThread != null) && downloadThread.isAlive() && 
        		(keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
	    	moveTaskToBack(true);
        	return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void InitListViews()
    {
		lvAll = (ListView)findViewById(R.id.all_tab);
		lvDownloaded = (ListView)findViewById(R.id.downloaded_tab);
		lvStarred = (ListView)findViewById(R.id.starred_tab);

        lvDownloaded.setTextFilterEnabled(true);
        lvDownloaded.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvDownloaded.setOnItemClickListener(gameListClickListener);
        lvDownloaded.setOnItemLongClickListener(gameListLongClickListener);
		
        lvStarred.setTextFilterEnabled(true);
        lvStarred.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvStarred.setOnItemClickListener(gameListClickListener);
        lvStarred.setOnItemLongClickListener(gameListLongClickListener);

        lvAll.setTextFilterEnabled(true);
        lvAll.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvAll.setOnItemClickListener(gameListClickListener);
        lvAll.setOnItemLongClickListener(gameListLongClickListener);

        //Забираем список игр из предыдущего состояния активити, если оно было пересоздано (при повороте экрана)
        final Object data = getLastNonConfigurationInstance();        
        if (data != null) {
        	xmlGameListCached = (String)data;
        }

        getTabHost().setOnTabChangedListener(new OnTabChangeListener(){
        	@Override
        	public void onTabChanged(String tabId) {
        		int tabNum = getTabHost().getCurrentTab();
        		if (((tabNum == STARRED_TABNUM) || (tabNum == ALL_TABNUM)) && (xmlGameListCached == null) && !gameListIsLoading) {
        			if (Utility.haveInternet(uiContext))
        			{
        				gameListIsLoading = true;
        				triedToLoadGameList = true;
        				LoadGameListThread tLoadGameList = new LoadGameListThread();
        				tLoadGameList.start();
        			}
        			else
        			{
        				if (!triedToLoadGameList)
        				{
        					Utility.ShowError(uiContext, getString(R.string.gamelistLoadError));
        					triedToLoadGameList = true;
        				}
        			}
        		}
        	}
        });
        
        RefreshLists();
    }

    private void Notify(String text, String details)
    {
            Intent notificationIntent = new Intent(uiContext, QspPlayerStart.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent contentIntent = PendingIntent.getActivity(uiContext, 0, notificationIntent, 0); 

            Notification note = new Notification(
                    android.R.drawable.stat_notify_sdcard, //!!! STUB              // the icon for the status bar
                    text,                  		// the text to display in the ticker
                    System.currentTimeMillis() // the timestamp for the notification
                    ); 

            note.setLatestEventInfo(uiContext, text, details, contentIntent);
            note.flags = Notification.FLAG_AUTO_CANCEL;
            
            mNotificationManager.notify(
                       QSP_NOTIFICATION_ID, // we use a string id because it is a unique
                                            // number.  we use it later to cancel the
                                            // notification
                       note);
    }
    
    private String getGameIdByPosition(int position)
    {
		String value = "";
		int tab = getTabHost().getCurrentTab();
		switch (tab) {
		case DOWNLOADED_TABNUM:
			//Загруженные
			GameItem tt1 = (GameItem) lvDownloaded.getAdapter().getItem(position);
			value = tt1.game_id;
			break;
		case STARRED_TABNUM:
			//Отмеченные
			GameItem tt2 = (GameItem) lvStarred.getAdapter().getItem(position);
			value = tt2.game_id;
			break;
		case ALL_TABNUM:
			//Все
			GameItem tt3 = (GameItem) lvAll.getAdapter().getItem(position);
			value = tt3.game_id;
			break;
		}
		return value;
    }
    
    //Выбрана игра в списке
    private OnItemClickListener gameListClickListener = new OnItemClickListener() 
    {
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		String value = getGameIdByPosition(position);
    		ShowGameInfo(value);
    	}
    };
    
    //Выбрана игра в списке(LongClick)
    private OnItemLongClickListener gameListLongClickListener = new OnItemLongClickListener() 
    {
    	@Override
    	public boolean onItemLongClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		String value = getGameIdByPosition(position);
    		selectedGame = gamesMap.get(value);
    		if (selectedGame.downloaded){
    			//Если игра загружена, стартуем
    			Intent data = new Intent();
    			data.putExtra("file_name", selectedGame.game_file);
    			setResult(RESULT_OK, data);
    			finish();
    		}else
    			//иначе загружаем
    			DownloadGame(selectedGame.file_url, selectedGame.file_size, selectedGame.game_id);						
    		return true;
    	}
    };
    
    private void ShowGameInfo(String gameId)
    {
		selectedGame = gamesMap.get(gameId);
		if (selectedGame == null)
			return;
		
			StringBuilder txt = new StringBuilder();
			if(selectedGame.author.length()>0)
				txt.append("Author: ").append(selectedGame.author);
			if(selectedGame.version.length()>0)
				txt.append("\nVersion: ").append(selectedGame.version);
			if(selectedGame.file_size>0)
				txt.append("\nSize: ").append(selectedGame.file_size/1024).append(" Kilobytes");
			AlertDialog.Builder bld = new AlertDialog.Builder(uiContext).setMessage(txt)
			.setTitle(selectedGame.title)
			.setIcon(R.drawable.icon)
			.setPositiveButton((selectedGame.downloaded ? getString(R.string.playGameCmd) : getString(R.string.dlGameCmd)), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
	    		if (selectedGame.downloaded){
	    			//Если игра загружена, стартуем
	    			Intent data = new Intent();
	    			data.putExtra("file_name", selectedGame.game_file);
	    			setResult(RESULT_OK, data);
	    			finish();
	    		}else
	    			//иначе загружаем
	    			DownloadGame(selectedGame.file_url, selectedGame.file_size, selectedGame.game_id);						
			}
		})
			.setNegativeButton(getString(R.string.closeGameCmd), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();						
			}
		});
			bld.create().show();
    }
    
    private void DownloadGame(String file_url, int file_size, String game_id)
    {
		if (!Utility.haveInternet(uiContext))
		{
			Utility.ShowError(uiContext, getString(R.string.gameLoadNetError));
			return;
		}
		GameItem gameToDownload = gamesMap.get(game_id);
		String folderName = Utility.ConvertGameTitleToCorrectFolderName(gameToDownload.title);

		final String urlToDownload = file_url;
	final String unzipLocation = Utility.GetGamesPath(this).concat("/").concat(folderName).concat("/");
    	final String gameId = game_id;
    	final String gameName = gameToDownload.title;
	final String gamesPath = Utility.GetGamesPath(this);
    	final int totalSize = file_size;
    	downloadProgressDialog = new ProgressDialog(uiContext);
    	downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	downloadProgressDialog.setMax(totalSize);
    	downloadProgressDialog.setCancelable(false);
    	downloadThread = new Thread() {
            public void run() {
        		//set the path where we want to save the file
        		//in this case, going to save it in program cache directory
        		//on sd card.
        		
				// ** original code for checking for storage directory **
				// File SDCardRoot = Environment.getExternalStorageDirectory();
        		// File cacheDir = new File (SDCardRoot.getPath().concat("/Android/data/com.qsp.player/cache/"));
        		 
				// ** begin replacement code for checking storage directory **
				String strSDCardPath = System.getenv("SECONDARY_STORAGE");
								if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
					strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
				}
        		File cacheDir = new File (strSDCardPath.concat("/Android/data/com.qsp.player/cache/"));
				// ** end replacement code for checking storage directory **
        		
				
				if (!cacheDir.exists())
        		{
        			if (!cacheDir.mkdirs())
        			{
        				Utility.WriteLog(getString(R.string.cacheCreateError));
        				return;
        			}
        		}

        		//create a new file, specifying the path, and the filename
        		//which we want to save the file as.
        		String filename = String.valueOf(System.currentTimeMillis()).concat("_game.zip");
        		File file = new File(cacheDir, filename);
        		
        		boolean successDownload = false;

        		try {
            		//set the download URL, a url that points to a file on the internet
            		//this is the file to be downloaded
            		URL url = new URL(urlToDownload);

            		//create the new connection
            		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            		//set up some things on the connection
            		urlConnection.setRequestMethod("GET");
            		urlConnection.setDoOutput(true);

            		//and connect!
            		urlConnection.connect();

            		//this will be used to write the downloaded data into the file we created
            		FileOutputStream fileOutput = new FileOutputStream(file);

            		//this will be used in reading the data from the internet
            		InputStream inputStream = urlConnection.getInputStream();

            		//create a buffer...
            		byte[] buffer = new byte[1024];
            		int bufferLength = 0; //used to store a temporary size of the buffer
            		int downloadedCount = 0;

            		//now, read through the input buffer and write the contents to the file
            		while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
            			//add the data in the buffer to the file in the file output stream (the file on the sd card
            			fileOutput.write(buffer, 0, bufferLength);
            			//this is where you would do something to report the prgress, like this maybe
            			downloadedCount += bufferLength;
            			updateSpinnerProgress(true, gameName, getString(R.string.dlWaiting), -downloadedCount);
            		}
            		//close the output stream when done
            		fileOutput.close();
            		successDownload = totalSize == downloadedCount;
            	} catch (Exception e) {
            		e.printStackTrace();
    				Utility.WriteLog(getString(R.string.dlGameError));
            	}
            		
        		updateSpinnerProgress(false, "", "", 0);

        		final String checkGameName = gameName; 
        		final String checkGameId = gameId; 
        		
        		if (!successDownload)
        		{
        			if (file.exists())
        				file.delete();
            		runOnUiThread(new Runnable() {
            			public void run() {
        					String desc = getString(R.string.cantDlGameError).replace("-GAMENAME-",checkGameName);
            				if (isActive)
               	    			Utility.ShowError(uiContext, desc);
            				else
               					Notify(getString(R.string.genGameLoadError), desc);
            			}
            		});
            		return;
        		}

        		//Unzip
        		Unzip(file.getPath(), unzipLocation, gameName);        		
        		file.delete();
        		//Пишем информацию об игре
        		WriteGameInfo(gameId);
        	 
        		updateSpinnerProgress(false, "", "", 0);

        		runOnUiThread(new Runnable() {
        			public void run() {
        				RefreshLists();
        				GameItem selectedGame = gamesMap.get(checkGameId);
        				
        				//Если игра не появилась в списке, значит она не соответствует формату "Полки игр"
        				boolean success = (selectedGame != null) && selectedGame.downloaded;
        				
        				if (!success)
        				{
        					//Удаляем неудачно распакованную игру
						File gameFolder = new File(gamesPath.concat("/").concat(Utility.ConvertGameTitleToCorrectFolderName(checkGameName)));
        					Utility.DeleteRecursive(gameFolder);
        				}
        				
        				if (isActive)
        				{
            	    		if ( !success )
            	    		{
            	        		//Показываем сообщение об ошибке
            	    			Utility.ShowError(uiContext, getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\""+checkGameName+"\""));
            	    		}
            	    		else
            	    		{
            	    			ShowGameInfo(checkGameId);
            	    		}
        				}
        				else
        				{
        					String msg = null;
        					String desc = null;
        					if ( !success )
        					{
        						msg = getString(R.string.genGameLoadError);
        						desc = getString(R.string.cantUnzipGameError).replace("-GAMENAME-", "\""+checkGameName+"\"");
        					}
        					else
        					{
        						msg = getString(R.string.gameDlSuccess);
        						desc = getString(R.string.gameUploadSuccess).replace("-GAMENAME-","\""+checkGameName+"\"");
        					}
           					Notify(msg, desc);
        				}
        			}
        		});
            }
        };
        downloadThread.start();
    }
    
    private void Unzip(String zipFile, String location, String gameName)
    {
    	_zipFile = zipFile;
    	_location = location;

		runOnUiThread(new Runnable() {
			public void run() {
				downloadProgressDialog = new ProgressDialog(uiContext);
				downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				downloadProgressDialog.setCancelable(false);
			}
		});
    	updateSpinnerProgress(true, gameName, getString(R.string.unpackMsg), 0);

    	_dirChecker("");

    	try  { 
    		FileInputStream fin = new FileInputStream(_zipFile); 
    		ZipInputStream zin = new ZipInputStream(fin);
    		BufferedInputStream in = new BufferedInputStream(zin);

    		ZipEntry ze = null; 
    		while ((ze = zin.getNextEntry()) != null) { 
    			Log.v(getString(R.string.decompMsg), getString(R.string.unzipMsg).replace("-FILENAME-","\n"+ze.getName()+"\n"));

    			if(ze.isDirectory()) { 
    				_dirChecker(ze.getName()); 
    			} else { 
    				FileOutputStream fout = new FileOutputStream(_location + ze.getName()); 
    				BufferedOutputStream out = new BufferedOutputStream(fout);
    				byte b[] = new byte[1024];
    				int n;
    				while ((n = in.read(b,0,1024)) >= 0) {
    					out.write(b,0,n);
    					updateSpinnerProgress(true, gameName, getString(R.string.unpackMsg), n);
    				}

    				zin.closeEntry();
    				out.close();
    				fout.close();
    			} 

    		} 
    		in.close();
    		zin.close(); 
    	} catch(Exception e) { 
    		Log.e(getString(R.string.decompMsg), getString(R.string.unzipMsgShort), e);
    	} 
    }
    
    private void _dirChecker(String dir) { 
    	File f = new File(_location + dir); 

    	if(!f.isDirectory()) { 
    		f.mkdirs(); 
    	} 
    }
    
    private void WriteGameInfo(String gameId)
    {
    	//Записываем всю информацию об игре
		GameItem game = gamesMap.get(gameId);
		if (game==null)
			return;
		
		File gameFolder = new File(Utility.GetGamesPath(this).concat("/").concat(Utility.ConvertGameTitleToCorrectFolderName(game.title)));
		if (!gameFolder.exists())
			return;
		
		String infoFilePath = gameFolder.getPath().concat("/").concat(GAME_INFO_FILENAME);
		FileOutputStream fOut;
		try {
			fOut = new FileOutputStream(infoFilePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Utility.WriteLog(getString(R.string.gameInfoFileCreateError));
			return;
		}
		OutputStreamWriter osw = new OutputStreamWriter(fOut);	
		
		try {
			osw.write("<game>\n");
			osw.write("\t<id><![CDATA[".concat(game.game_id.substring(3)).concat("]]></id>\n"));
			osw.write("\t<list_id><![CDATA[".concat(game.list_id).concat("]]></list_id>\n"));
			osw.write("\t<author><![CDATA[".concat(game.author).concat("]]></author>\n"));
			osw.write("\t<ported_by><![CDATA[".concat(game.ported_by).concat("]]></ported_by>\n"));
			osw.write("\t<version><![CDATA[".concat(game.version).concat("]]></version>\n"));
			osw.write("\t<title><![CDATA[".concat(game.title).concat("]]></title>\n"));
			osw.write("\t<lang><![CDATA[".concat(game.lang).concat("]]></lang>\n"));
			osw.write("\t<player><![CDATA[".concat(game.player).concat("]]></player>\n"));
			osw.write("\t<file_url><![CDATA[".concat(game.file_url).concat("]]></file_url>\n"));
			osw.write("\t<file_size><![CDATA[".concat(String.valueOf(game.file_size)).concat("]]></file_size>\n"));
			osw.write("\t<desc_url><![CDATA[".concat(game.desc_url).concat("]]></desc_url>\n"));
			osw.write("\t<pub_date><![CDATA[".concat(game.pub_date).concat("]]></pub_date>\n"));
			osw.write("\t<mod_date><![CDATA[".concat(game.mod_date).concat("]]></mod_date>\n"));
			osw.write("</game>");

			osw.flush();
			osw.close();
		} catch (IOException e) {
			e.printStackTrace();
			Utility.WriteLog(getString(R.string.gameInfoFileWriteError));
			return;
		}
    }

    private void updateSpinnerProgress(boolean enabled, String title, String message, int nProgress)
    {
    	final boolean show = enabled;
    	final String dialogTitle = title;
    	final String dialogMessage = message;
    	final int nCount = nProgress;
		runOnUiThread(new Runnable() {
			public void run() {
				showProgressDialog = show;
				if (!isActive || (downloadProgressDialog == null))
					return;
				if (!show && downloadProgressDialog.isShowing())
				{
					downloadProgressDialog.dismiss();
					downloadProgressDialog = null;
					return;
				}
				if (nCount>=0)
					downloadProgressDialog.incrementProgressBy(nCount);
				else
					downloadProgressDialog.setProgress(-nCount);
				if (show && !downloadProgressDialog.isShowing())
				{
					downloadProgressDialog.setTitle(dialogTitle);
					downloadProgressDialog.setMessage(dialogMessage);
					downloadProgressDialog.show();
					downloadProgressDialog.setProgress(0);
				}
			}
		});
    }

    private void RefreshLists()
    {
    	gamesMap.clear();
    	
		ParseGameList(xmlGameListCached);

		if (!ScanDownloadedGames())
		{
			Utility.ShowError(uiContext, getString(R.string.noCardAccess));
			return;
		}
		
		GetCheckedGames();
		
		
		RefreshAllTabs();
    }
    
    private boolean ScanDownloadedGames()
    {
    	//Заполняем список скачанных игр
    	
	String path = Utility.GetGamesPath(this);
    	if (TextUtils.isEmpty(path))
    		return false;
    	
        File gameStartDir = new File (path);
        File[] sdcardFiles = gameStartDir.listFiles();        
        ArrayList<File> qspGameDirs = new ArrayList<File>();
        ArrayList<File> qspGameFiles = new ArrayList<File>();
        //Сначала добавляем все папки
		if (sdcardFiles!=null)
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        	{
        		//Из папок добавляем только те, в которых есть игра
                File[] curDirFiles = currentFile.listFiles();        
                for (File innerFile : curDirFiles)
                {
                	if (!innerFile.isHidden() && (innerFile.getName().endsWith(".qsp") || innerFile.getName().endsWith(".gam")))
                	{
                		qspGameDirs.add(currentFile);
                		qspGameFiles.add(innerFile);
                		break;
                	}
                }
        	}
        }

        //Ищем загруженные игры в карте
        for (int i=0; i<qspGameDirs.size(); i++)
        {
        	File d = qspGameDirs.get(i);
        	GameItem game = null;
        	File infoFile = new File(d.getPath().concat("/").concat(GAME_INFO_FILENAME));
        	if (infoFile.exists())
        	{
				String text = "";
        		try {
        			FileInputStream instream = new FileInputStream(infoFile.getPath());
        			if (instream != null) {
        				InputStreamReader inputreader = new InputStreamReader(instream);
        				BufferedReader buffreader = new BufferedReader(inputreader);
        				boolean exit = false;
        				while (!exit) {
        					String line = null;
							try {
								line = buffreader.readLine();
							} catch (IOException e) {
								e.printStackTrace();
			        			Utility.WriteLog(getString(R.string.gameInfoFileReadError));
							}
        					exit = line == null;
        					if (!exit)
        						text = text.concat(line);
        				}
    					buffreader.close();
        			}
					instream.close();
        		} catch (IOException e) {
        			e.printStackTrace();
        			Utility.WriteLog(getString(R.string.gameInfoFileReadError));
        			continue;
				}
            	game = ParseGameInfo(text);
        	}
        	if (game == null)
        	{
        		game = new GameItem();
            	String displayName = d.getName();
        		game.title = displayName;
        		game.game_id = displayName;
        	}
        	File f = qspGameFiles.get(i);
    		game.game_file = f.getPath();
    		game.downloaded = true;
    		gamesMap.put(game.game_id, game);
        }
    
        return true;
    }
    
    private void GetCheckedGames()
    {
    	//!!! STUB
    	//Заполняем список отмеченных игр
    	
    }
   
    private void RefreshAllTabs()
    {
    	//Выводим списки игр на экран

    	//Все
        ArrayList<GameItem> gamesAll = new ArrayList<GameItem>();
        for (HashMap.Entry<String, GameItem> e : gamesMap.entrySet())
        {
        	gamesAll.add(e.getValue());
        }
        lvAll.setAdapter(new GameAdapter(uiContext, R.layout.game_item, gamesAll));
        //Загруженные
        ArrayList<GameItem> gamesDownloaded = new ArrayList<GameItem>();
        for (HashMap.Entry<String, GameItem> e : gamesMap.entrySet())
        {
        	if (e.getValue().downloaded)
        		gamesDownloaded.add(e.getValue());
        }
        lvDownloaded.setAdapter(new GameAdapter(uiContext, R.layout.game_item, gamesDownloaded));
        
        //Отмеченные
        //!!! STUB
        ArrayList<GameItem> gamesStarred = new ArrayList<GameItem>();
        lvStarred.setAdapter(new GameAdapter(uiContext, R.layout.game_item, gamesStarred));
        //Определяем, какую вкладку открыть
        if (openDefaultTab)
        {
        	openDefaultTab = false;
        	
        	int tabIndex = 0;//Загруженные
        	if (lvDownloaded.getAdapter().isEmpty())
        	{
        		if (lvStarred.getAdapter().isEmpty())
        			tabIndex = 2;//Все
        		else
        			tabIndex = 1;//Отмеченные
        	}
        	
        	getTabHost().setCurrentTab(tabIndex);
        }
    }
    
    private GameItem ParseGameInfo(String xml)
    {
    	//Читаем информацию об игре
    	GameItem resultItem = null;
    	GameItem curItem = null;
    	try {
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser xpp = factory.newPullParser();

    		xpp.setInput( new StringReader ( xml ) );
    		int eventType = xpp.getEventType();
    		boolean doc_started = false;
    		boolean game_started = false;
    		String lastTagName = "";
    		while (eventType != XmlPullParser.END_DOCUMENT) {
    			if(eventType == XmlPullParser.START_DOCUMENT) {
    				doc_started = true;
    			} else if(eventType == XmlPullParser.END_DOCUMENT) {
    				//Never happens
    			} else if(eventType == XmlPullParser.START_TAG) {
    				if (doc_started)
    				{
    					lastTagName = xpp.getName();
						if (lastTagName.equals("game"))
						{
    						game_started = true;
							curItem = new GameItem();
						}
    				}
    			} else if(eventType == XmlPullParser.END_TAG) {
    				if (doc_started && game_started)
    				{
    					if (xpp.getName().equals("game"))
    						resultItem = curItem;
    					lastTagName = "";
    				}
    			} else if(eventType == XmlPullParser.CDSECT) {
    				if (doc_started && game_started)
    				{
    					String val = xpp.getText();
    					if (lastTagName.equals("id"))
    						curItem.game_id = "id:".concat(val);
    					else if (lastTagName.equals("list_id"))
    						curItem.list_id = val;
    					else if (lastTagName.equals("author"))
    						curItem.author = val;
    					else if (lastTagName.equals("ported_by"))
    						curItem.ported_by = val;
    					else if (lastTagName.equals("version"))
    						curItem.version = val;
    					else if (lastTagName.equals("title"))
    						curItem.title = val;
    					else if (lastTagName.equals("lang"))
    						curItem.lang = val;
    					else if (lastTagName.equals("player"))
    						curItem.player = val;
    					else if (lastTagName.equals("file_url"))
    						curItem.file_url = val;
    					else if (lastTagName.equals("file_size"))
    						curItem.file_size = Integer.parseInt(val);
    					else if (lastTagName.equals("desc_url"))
    						curItem.desc_url = val;
    					else if (lastTagName.equals("pub_date"))
    						curItem.pub_date = val;
    					else if (lastTagName.equals("mod_date"))
    						curItem.mod_date = val;
    				}
    			}
    			eventType = xpp.nextToken();
    		}
    	} catch (XmlPullParserException e) {
			String errTxt = getString(R.string.parseGameInfoXMLError);
			errTxt = errTxt.replace("-LINENUM-",String.valueOf(e.getLineNumber()));
			errTxt = errTxt.replace("-COLNUM-",String.valueOf(e.getColumnNumber()));
    		Utility.WriteLog(errTxt);
    	} catch (Exception e) {
    		Utility.WriteLog(getString(R.string.parseGameInfoUnkError));
    	}
    	return resultItem;
    }
    
    private boolean ParseGameList(String xml)
    {
    	boolean parsed = false;
    	if (xml != null)
    	{
        	GameItem curItem = null;
	    	try {
	    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	    		factory.setNamespaceAware(true);
	    		XmlPullParser xpp = factory.newPullParser();
	
	    		xpp.setInput( new StringReader ( xml ) );
	    		int eventType = xpp.getEventType();
	    		boolean doc_started = false;
	    		boolean list_started = false;
	    		String lastTagName = "";
	    		String listId = "unknown";
	    		String listTitle = "";
	    		while (eventType != XmlPullParser.END_DOCUMENT) {
	    			if(eventType == XmlPullParser.START_DOCUMENT) {
	    				doc_started = true;
	    			} else if(eventType == XmlPullParser.END_DOCUMENT) {
	    				//Never happens
	    			} else if(eventType == XmlPullParser.START_TAG) {
	    				if (doc_started)
	    				{
	    					lastTagName = xpp.getName();
	    					if (lastTagName.equals("game_list"))
	    					{
	    						list_started = true;
	    						listId = xpp.getAttributeValue(null, "id");
	    						listTitle = xpp.getAttributeValue(null, "title");
	    					}
	    					if (list_started)
	    					{
	    						if (lastTagName.equals("game"))
	    						{
	    							curItem = new GameItem();
	    							curItem.list_id = listId;
	    						}
	    					}            		 
	    				}
	    			} else if(eventType == XmlPullParser.END_TAG) {
	    				if (doc_started && list_started)
	    				{
	    					if (xpp.getName().equals("game"))
	    					{
	    						gamesMap.put(curItem.game_id, curItem);
	    					}
	    					if (xpp.getName().equals("game_list"))
	    						parsed = true;
	    					lastTagName = "";
	    				}
	    			} else if(eventType == XmlPullParser.CDSECT) {
	    				if (doc_started && list_started)
	    				{
	    					String val = xpp.getText();
	    					if (lastTagName.equals("id"))
	    						curItem.game_id = "id:".concat(val);
	    					else if (lastTagName.equals("author"))
	    						curItem.author = val;
	    					else if (lastTagName.equals("ported_by"))
	    						curItem.ported_by = val;
	    					else if (lastTagName.equals("version"))
	    						curItem.version = val;
	    					else if (lastTagName.equals("title"))
	    						curItem.title = val;
	    					else if (lastTagName.equals("lang"))
	    						curItem.lang = val;
	    					else if (lastTagName.equals("player"))
	    						curItem.player = val;
	    					else if (lastTagName.equals("file_url"))
	    						curItem.file_url = val;
	    					else if (lastTagName.equals("file_size"))
	    						curItem.file_size = Integer.parseInt(val);
	    					else if (lastTagName.equals("desc_url"))
	    						curItem.desc_url = val;
	    					else if (lastTagName.equals("pub_date"))
	    						curItem.pub_date = val;
	    					else if (lastTagName.equals("mod_date"))
	    						curItem.mod_date = val;
	    				}
	    			}
	   				eventType = xpp.nextToken();
	    		}
	    	} catch (XmlPullParserException e) {
				String errTxt = getString(R.string.parseGameInfoXMLError);
				errTxt = errTxt.replace("-LINENUM-",String.valueOf(e.getLineNumber()));
				errTxt = errTxt.replace("-COLNUM-",String.valueOf(e.getColumnNumber()));
	    		Utility.WriteLog(errTxt);
	    	} catch (Exception e) {
				Utility.WriteLog(getString(R.string.parseGameInfoUnkError));
	    	}
    	}
    	if ( !parsed && isActive && triedToLoadGameList )
    	{
    		//Показываем сообщение об ошибке
    		Utility.ShowError(uiContext, getString(R.string.gamelistLoadError));
    	}
    	return parsed;
    }

    private class LoadGameListThread extends Thread {
    	//Загружаем список игр
        public void run() {
    		runOnUiThread(new Runnable() {
    			public void run() {
    				downloadProgressDialog = new ProgressDialog(uiContext);
    				downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    				downloadProgressDialog.setCancelable(false);
    			}
    		});
            try {
            	updateSpinnerProgress(true, "", getString(R.string.gamelistLoadWait), 0);
            	URL updateURL = new URL("http://qsp.su/tools/gamestock/gamestock.php");
                URLConnection conn = updateURL.openConnection();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(1024);

                int current = 0;
                while((current = bis.read()) != -1){
                    baf.append((byte)current);
                }

                /* Convert the Bytes read to a String. */
                final String xml = new String(baf.toByteArray());
    			runOnUiThread(new Runnable() {
    				public void run() {
    					xmlGameListCached = xml;
    					RefreshLists();
    	    			updateSpinnerProgress(false, "", "", 0);
    					gameListIsLoading = false;
    				}
    			});
            } catch (Exception e) {
            	Utility.WriteLog(getString(R.string.gamelistLoadExcept));
    			runOnUiThread(new Runnable() {
    				public void run() {
    					RefreshLists();
    	    			updateSpinnerProgress(false, "", "", 0);
    					gameListIsLoading = false;
    				}
    			});
            }
        }
    };
    
    
    //***********************************************************************
    //			выбор файла "напрямую", через пролистывание папок
    //***********************************************************************
    android.content.DialogInterface.OnClickListener browseFileClick = new DialogInterface.OnClickListener()
    {
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
			if (!((AlertDialog)dialog).isShowing())
				return;
			dialog.dismiss();
			boolean canGoUp = !backPath.equals("");
			int shift = 0;
			if (canGoUp)
				shift = 1;
			if (which == 0 && canGoUp)
				BrowseGame(backPath, false);
			else
			{
				File f = qspGamesBrowseList.get(which - shift);
				if (f.isDirectory())
					BrowseGame(f.getPath(), false);
				else
				{
					//Запускаем файл
	    			Intent data = new Intent();
	    			data.putExtra("file_name", f.getPath());
	    			setResult(RESULT_OK, data);
	    			finish();
				}
			}
		}    	
    };
    
    private void BrowseGame(String startpath, boolean start)
    {
    	if (startpath == null)
    		return;
    	
    	if (!startpath.endsWith("/"))
    		startpath = startpath.concat("/");
    	
    	//Устанавливаем путь "выше"    	
    	if (!start)
    		if (startRootPath.equals(startpath))
    			start = true;
    	if (!start)
    	{
    		int slash = startpath.lastIndexOf(File.separator, startpath.length() - 2);
    		if (slash >= 0)
    			backPath = startpath.substring(0, slash + 1);
    		else
    			start = true;
    	}
    	if (start)
    	{
    		startRootPath = startpath;
    		backPath = "";
    	}
    	
        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File (startpath);
        if ((sdcardRoot == null) || !sdcardRoot.exists())
        {
        	Utility.ShowError(uiContext, getString(R.string.pathNotFound).replace("-PATHNAME-",startpath));
        	return;
        }
        File[] sdcardFiles = sdcardRoot.listFiles();        
        qspGamesBrowseList = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        		qspGamesBrowseList.add(currentFile);
        }
        //Потом добавляем все QSP-игры
        for (File currentFile : sdcardFiles)
        {
        	if (!currentFile.isHidden() && (currentFile.getName().endsWith(".qsp") || currentFile.getName().endsWith(".gam")))
        		qspGamesBrowseList.add(currentFile);
        }
        
        //Если мы не на самом верхнем уровне, то добавляем ссылку 
        int shift = 0;
        if (!start)
        	shift = 1;
        int total = qspGamesBrowseList.size() + shift;
        final CharSequence[] items = new String[total];
        if (!start)
            items[0] = "[..]";
        for (int i=shift; i<total; i++)
        {
        	File f = qspGamesBrowseList.get(i - shift);
        	String displayName = f.getName();
        	if (f.isDirectory())
        		displayName = "["+ displayName + "]";
        	items[i] = displayName;
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gameFileSelect));
        builder.setItems(items, browseFileClick);
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    //***********************************************************************
    //			удаление игр (выбор папки с игрой)
    //***********************************************************************
    private void DeleteGames()
    {
        //Ищем папки в /qsp/games
        File sdcardRoot = new File (Utility.GetGamesPath(this));
        File[] sdcardFiles = sdcardRoot.listFiles();
        qspGamesToDeleteList = new ArrayList<File>();
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        		qspGamesToDeleteList.add(currentFile);
        }
        
        int total = qspGamesToDeleteList.size();
        final CharSequence[] items = new String[total];
        for (int i=0; i<total; i++)
        {
        	File f = qspGamesToDeleteList.get(i);
        	items[i] = f.getName();
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);
        builder.setTitle(getString(R.string.gameFileDeleteTitle));
        builder.setItems(items, new DialogInterface.OnClickListener()
	        {
	    		@Override
	    		public void onClick(DialogInterface dialog, int which) 
	    		{
	    	        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(uiContext);
	    			final File f = qspGamesToDeleteList.get(which);
    				if ((f == null) || !f.isDirectory())
    					return;
	    	        confirmBuilder.setMessage(getString(R.string.gameFileDeleteQuery).replace("-GAMENAME-","\""+f.getName()+"\""));
	    	        confirmBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) {
    						if (whichButton==DialogInterface.BUTTON_POSITIVE)
    						{
    		    				Utility.DeleteRecursive(f);
    		    				Utility.ShowInfo(uiContext, getString(R.string.gameFileDeleteSuccess));
    		    				RefreshLists();
    						}
    					}
    				});
    		        confirmBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int whichButton) { }
    				});
    				
    		        AlertDialog confirm = confirmBuilder.create();
    		        confirm.show();
	    		}    	
	        }
        );
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		});
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class GameAdapter extends ArrayAdapter<GameItem> {

    	private ArrayList<GameItem> items;
    	
        public GameAdapter(Context context, int textViewResourceId, ArrayList<GameItem> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.game_item, null);
            }
            GameItem o = this.items.get(position);
            if (o != null) {
                    TextView tt = (TextView) v.findViewById(R.id.game_title);
                    if (tt != null) {
                          tt.setText(o.title);
                          if (o.downloaded)
                        	  tt.setTextColor(0xFFE0E0E0);
                          else
                        	  tt.setTextColor(0xFFFFD700);
                    }
                	tt = (TextView) v.findViewById(R.id.game_author);
                    if(o.author.length()>0)
                    	tt.setText(new StringBuilder().append(getString(R.string.gameAuthorText).replace("-AUTHOR-",o.author)));//.append("  (").append(o.file_size).append(" байт)"));
                    else
                    	tt.setText("");
            }
            return v;
        }
    }
}