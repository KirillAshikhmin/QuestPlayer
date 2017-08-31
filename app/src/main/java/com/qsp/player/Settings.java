package com.qsp.player;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	final private Context uiContext = this;
    final int ACTIVITY_SELECT_DIRECTORY = 531;
    String SDPath;
    String backPath;
    ArrayList<File> qspBrowseDir;
    String selectedDirectory;
    boolean usingSDCard;

    private void setSDCard (SharedPreferences sharedPref) {
        //Get external SD card flag and get directory; set extSDCard false if no external SD card
        boolean extSDCard = sharedPref.getBoolean("storageType",true);
        if (extSDCard) {
            SDPath = System.getenv("SECONDARY_STORAGE");
            if ((null == SDPath) || (SDPath.length() == 0)) {
                SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
            }
            if ((null == SDPath) || (SDPath.length() == 0)) {
                SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                extSDCard = false;
            }
        } else
            SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        SDPath += "/";

    }

    public void setFullGamesPath (SharedPreferences sharedPref) {

        //Get external SD card flag and get directory; set extSDCard false if no external SD card
        setSDCard(sharedPref);
        //Get the relative games path and merge the two directories
        //Add a trailing "/" to relPath and remove change "//" to "/" if present
        String relPath = sharedPref.getString("relGamePath",getString(R.string.defRelPath));
        if (!relPath.endsWith("/")) relPath +="/";
        if (!relPath.startsWith("/")) relPath = "/" + relPath;
        String fullGamesPath = SDPath + relPath;
        relPath = relPath.replace("//","/");
        fullGamesPath = fullGamesPath.replace("//","/");

        //Store adjusted storage type, relative path, and complete game directory
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("compGamePath", fullGamesPath);
        editor.commit();

    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);

        CheckBoxPreference prefSDCard = (CheckBoxPreference) findPreference("storageType");
        prefSDCard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setSDCard(sharedPref);
                setFullGamesPath(sharedPref);

                String newPath = sharedPref.getString("compGamePath",getString(R.string.defGamePath));

                return true;
            }
        });


        //Directory picker for finding games path - setSummary, prepare for click
        Preference filePicker = (Preference) findPreference("relGamePath");
        filePicker.setSummary(sharedPref.getString("relGamePath",getString(R.string.defRelPath)));
        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                //Get external SD card flag and get directory; set extSDCard false if no external SD card
                boolean extSDCard = sharedPref.getBoolean("storageType",true);
                if (extSDCard) {
                    SDPath = System.getenv("SECONDARY_STORAGE");
                    if ((null == SDPath) || (SDPath.length() == 0)) {
                        SDPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
                    }
                    if ((null == SDPath) || (SDPath.length() == 0)) {
                        SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        extSDCard = false;
                    }
                } else
                    SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                SDPath += "/";

                String newPath = sharedPref.getString("compGamePath",getString(R.string.defGamePath));

                SelectDirectory(newPath,false);
                return true;
            }
        });
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) { 
    	preference.setSummary((CharSequence)newValue);
    	return true; 
    }

    private void SelectDirectory(String startpath, boolean start)
    {
        Utility.WriteLog("startpath: "+startpath);
        Utility.WriteLog("backPath: "+backPath);
        Utility.WriteLog("SDPath: "+SDPath);

        selectedDirectory = startpath;
        if (!start)
            if (SDPath.equals(startpath))
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
            backPath = "";
        }

        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File (startpath);
        if ((sdcardRoot == null) || !sdcardRoot.exists())
        {
            String failedPath = ("/"+startpath.replace(SDPath,"")).replace("//","/");
            //If at SDPath already and File is null/doesn't exist, ShowError and exit
            if (startpath.equals(SDPath)) {
                Utility.ShowError(uiContext, getString(R.string.pathNotFound).replace("-PATHNAME-", failedPath));
                return;
            }
            //If not at SPath yet, ShowInfo and drop to lowest directory
            else {
                Utility.ShowInfo(uiContext,getString(R.string.pathNotFound).replace("-PATHNAME-", failedPath));
                SelectDirectory(SDPath,true);
                return;
            }
        }
        File[] sdcardFiles = sdcardRoot.listFiles();
        qspBrowseDir = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
            if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
                qspBrowseDir.add(currentFile);
        }

        //Если мы не на самом верхнем уровне, то добавляем ссылку
        int shift = 1;
        if (!start)
            shift++;
        int total = qspBrowseDir.size() + shift;
        final CharSequence[] items = new String[total];
Utility.WriteLog("startpathX: "+startpath);
        String tempStr;
        if (start)
            tempStr = "/";
        else {
            tempStr = startpath.replace(SDPath,"");
            if (!tempStr.startsWith("/")) tempStr = "/" + tempStr;
        }
        if (!tempStr.endsWith("/")) tempStr += "/";// tempStr.substring(0,tempStr.length()-1);
        items[0] = getString(R.string.selectDir).replace("-DIRNAME-", tempStr);

        if (!start)
            items[1] = "[..]";
        for (int i=shift; i<total; i++)
        {
            File f = qspBrowseDir.get(i - shift);
            String displayName = f.getName();
            if (f.isDirectory())
                displayName = "["+ displayName + "]";
            items[i] = displayName;
        }
        Utility.WriteLog("test1: "+total);

        //Sort both arrays
        CharSequence[] sortedItems = new CharSequence[items.length];
        sortedItems = NameSorter(shift,items);
        qspBrowseDir = FileSorter(qspBrowseDir);

        for (int i=0; i<qspBrowseDir.size(); i++)
            Utility.WriteLog(items[i+shift]+" = "+qspBrowseDir.get(i).getName());

        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.selectDirTitle));
        builder.setItems(sortedItems, browseFolderClick);
        AlertDialog alert = builder.create();
        alert.show();
    }

    //Sort all names except for "1 to shift"
    private CharSequence[] NameSorter (int shift, CharSequence[] names) {
        //Skip if there aren't at least two names to sort
        if (names.length-shift<2) return names;

        if (names.length < 2+shift) return names;
        Arrays.sort(names, shift, names.length, new Comparator<CharSequence>() {
            public int compare(CharSequence name1, CharSequence name2) {
                return name1.toString().compareTo(name2.toString());
            }
        });
        return names;
    }

    //Sort the ArrayList of Files by file name
    private ArrayList<File> FileSorter (ArrayList<File> files) {
        //Skip if there aren't at least two files
        if (files.size() < 2) return files;

        File[] fileArray = files.toArray(new File[files.size()]);
        Arrays.sort(fileArray, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        ArrayList<File> returnedList = new ArrayList<File>();

        for(int i=0; i<fileArray.length; i++)
            returnedList.add(fileArray[i]);

        return returnedList;
    }

    android.content.DialogInterface.OnClickListener browseFolderClick = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            if (!((AlertDialog)dialog).isShowing())
                return;
            dialog.dismiss();
            boolean canGoUp = !backPath.equals("");
            int shift = 1;
            if (canGoUp)
                shift++;
            if (which == 0) {

                //Lock the new path in place, but only the relative path - remove the storage root
                String newValue = selectedDirectory;
                newValue = newValue.replace(SDPath,"");
                Utility.WriteLog("newValue: "+newValue);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(uiContext);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("relGamePath", newValue);
                editor.commit();

                if (!newValue.startsWith("/")) newValue = "/" + newValue;
                if (!newValue.endsWith("/")) newValue += "/";
                Preference pref = findPreference("relGamePath");
                pref.setSummary(newValue);

//                finish();
            }
            else if (which == 1 && canGoUp)
                SelectDirectory(backPath, false);
            else
            {
                File f = qspBrowseDir.get(which - shift);
                SelectDirectory(f.getPath(), false);
            }
        }
    };


}
