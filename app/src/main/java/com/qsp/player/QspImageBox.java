package com.qsp.player;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;


/**
 * Show an image called by "VIEW" keyword from a game.
 */
public class QspImageBox extends Activity implements OnClickListener {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
        
        //set layout
        setContentView(R.layout.image_box);
        
        //set image click listener to activity click listener
        ImageView box = (ImageView) findViewById(R.id.imagebox); 
        box.setOnClickListener(this);

        //load image
        Bundle b = this.getIntent().getExtras();
        String file=b.getString("imageboxFile");
        ErrorReporter.getInstance().putCustomData("ImageBoxFile", file);
        Drawable drawable = Drawable.createFromPath(file);
        if (drawable == null)
        {
        	finish();
        	return;
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        
        //assign to view
        box.setImageDrawable(drawable);
    }

	@Override
	public void onClick(View arg0) {
		//Closed by any click		
		finish();
		ErrorReporter.getInstance().removeCustomData("ImageBoxFile");
	}
}
