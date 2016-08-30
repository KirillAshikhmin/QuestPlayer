package com.qsp.player;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        String defaultGamesDir = Utility.GetDefaultPath();
        if (defaultGamesDir != null && !TextUtils.isEmpty(defaultGamesDir)) {
            EditTextPreference gamesDirPref = (EditTextPreference) findPreference("gamesdir");
            gamesDirPref.setDefaultValue(defaultGamesDir);
            if (TextUtils.isEmpty(gamesDirPref.getText())) {
                gamesDirPref.setText(defaultGamesDir);
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) { 
    	preference.setSummary((CharSequence)newValue); 
    	return true; 
    }

}
