package com.qsp.player;

import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class QspImageGetter implements ImageGetter {
    private String mDirectory;
    private int mScreenWidth;
    private boolean mFullSize;

    public void setDirectory(String directory) {
        mDirectory = directory;
    }

    public void setScreenWidth(int width) {
        mScreenWidth = width;
    }

    public void setFullSize(boolean fullSize) {
        mFullSize = fullSize;
    }

    @Override
    public Drawable getDrawable(String source) {
        Drawable drawable = null;
        if ((source != null) && (!source.contains("://"))) {
            File path = new File(mDirectory.concat(source)); // Fallback

            // Even though File.exists may be case insensitive, for some weird reason
            // the Drawable.createFromPath may be case sensitive and will fail.
            // So, we're searching for the resource here no matter what path.exists() may return.
            File curPath = new File(mDirectory);
            String[] segments = source.split("[/\\\\]");
            boolean found = false;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                boolean lastSegment = i == segments.length - 1;
                found = false;
                for (File f : curPath.listFiles()) {
                    if ((lastSegment ? f.isFile() : f.isDirectory())
                            && f.getName().equalsIgnoreCase(segment)) {
                        found = true;
                        curPath = f;
                        break;
                    }
                }
            }
            if (found && curPath.exists()) {
                path = curPath;
            }

            if (path.exists()) {
                try {
                    drawable = Drawable.createFromPath(path.getCanonicalPath());
                    if (path.getName().toLowerCase().endsWith(".gif")) {
                        drawable = new GifDrawable(path.getCanonicalPath());
                    } else {
                        drawable = Drawable.createFromPath(path.getCanonicalPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (drawable != null) {
                    int nWidth = (int) ((int) drawable.getIntrinsicWidth());
                    int nHeight = (int) ((int) drawable.getIntrinsicHeight());

                    if (mFullSize || nWidth > mScreenWidth) {
                        float k = mScreenWidth;
                        k = k / nWidth;
                        nWidth = mScreenWidth;
                        nHeight = (int) ((int) nHeight * k);
                    }

                    drawable.setBounds(0, 0, nWidth, nHeight);
                }
            }
        }
        return drawable;
    }
}
