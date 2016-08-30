package com.qsp.player;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=dd0e611c", formKey = "") 
public class QspApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
//        if (!Utility.signedWithDebugKey(this,this.getClass()))
//        	ACRA.init(this);
        super.onCreate();
    }
}
