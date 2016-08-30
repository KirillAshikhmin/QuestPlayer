package com.qsp.player;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) { 
    	preference.setSummary((CharSequence)newValue); 
    	return true; 
    }

}
