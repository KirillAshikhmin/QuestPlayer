package com.qsp.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.w3c.dom.Text;
import org.xml.sax.XMLReader;

import pl.droidsonroids.gif.GifDrawable;
import android.text.Html.TagHandler;

public class Utility {

    public static Spanned AttachGifCallback(Spanned html, Drawable.Callback callback) {
        boolean updated = false;
        SpannableStringBuilder ssb = new SpannableStringBuilder(html);
        for (ImageSpan img : ssb.getSpans(0, ssb.length(), ImageSpan.class)) {
            Drawable d = img.getDrawable();
            if (d instanceof GifDrawable) {
                GifDrawable gd = (GifDrawable) d;
                gd.setCallback(callback);
                gd.start();
                updated = true;
            }
        }
        return updated ? ssb : html;
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public static String ConvertGameTitleToCorrectFolderName(String title) {
        // Обрезаем многоточие
        String folder = title.endsWith("...") ? title.substring(0, title.length() - 3) : title;
        // Меняем двоеточие на запятую
        folder = folder.replace(':', ',');
        // Убираем кавычки
        folder = folder.replace('"', '_');
        // Убираем знак вопроса
        folder = folder.replace('?', '_');
        // Убираем звездочку
        folder = folder.replace('*', '_');
        // Убираем вертикальную черту
        folder = folder.replace('|', '_');
        // Убираем знак "меньше"
        folder = folder.replace('<', '_');
        // Убираем знак "больше"
        folder = folder.replace('>', '_');
        return folder;
    }

//Replacing this code with QspStrToWebView
    public static Spanned QspStrToHtml(String str, ImageGetter imgGetter, String srcDir) {
        if (str != null && str.length() > 0) {
            str = str.replaceAll("\r", "<br>");
            str = str.replaceAll("(?i)</td>", " ");
            str = str.replaceAll("(?i)</tr>", "<br>");

            str = fixImagesSize(str,srcDir,true);

            return Html.fromHtml(str, imgGetter, null);

        }
        return Html.fromHtml("");
    }

    public static String QspStrToWebView(String str, String srcDir) {
        if (str != null && str.length() > 0) {
            str = str.replaceAll("\r", "<br>");
            str = str.replaceAll("(?i)</td>", " ");
            str = str.replaceAll("(?i)</tr>", "<br>");

            str = fixImagesSize(str,srcDir,false);

            str = fixVideosLinks(str,srcDir);

            str = "<html><head>"
                    + "<style type=\"text/css\">body{color: white; background-color: black;}"
                    + "</style></head>"
                    + "<body>"
                    + str
                    + "</body></html>";

            return str;

        }
        return "";
    }

    private static String fixImagesSize(String str, String srcDir, boolean isForTextView) {
        boolean hasImg = str.contains("<img");

//        Utility.WriteLog("fixImagesSize: "+str);

        String endOfStr = str;
        String newStr= str;
        if (!hasImg) return str;
        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        do {
            int firstImg = endOfStr.indexOf("<img");
            if (firstImg==-1) {
                hasImg=false;
                continue;
            }
            hasImg = firstImg >=0;
            String curStr = endOfStr.substring(firstImg);
            int endImg = curStr.indexOf(">");
            curStr = curStr.substring(0,endImg+1);
            endOfStr = endOfStr.substring(firstImg+curStr.length());

            newStr = newStr.substring(0,newStr.indexOf(curStr));
            Matcher matcher = pattern.matcher(curStr);

            if (matcher.groupCount()==0) continue;

            String src = null, widthS = null, heightS = null;
            try {
                while (matcher.find()) {
                    String group = matcher.group();
                    if (group.startsWith("src=")) src = group;
                    else if (group.startsWith("width=")) widthS = group.substring(6);
                    else if (group.startsWith("height=")) heightS = group.substring(7);
                }

                if (isNullOrEmpty(src)) {
                    newStr += curStr + endOfStr;
                    continue;
                }

                //if this is for a TextView, don't use a URL locator
                if (isForTextView) {
                    String iconSrc = src;
                    //change [src=..."] to [src="]
                    if (iconSrc.indexOf("\"") > 3)
                        iconSrc = iconSrc.substring(0, iconSrc.indexOf("src=") + 4) + iconSrc.substring(iconSrc.indexOf("\""));
                    //Then add in the root directory for the game files
                    src = iconSrc.replace("src=\"/", "src=\"" + srcDir);
                    curStr = curStr.replace(src, iconSrc);

                    newStr += curStr+endOfStr;
                    continue;
                }

                //if this is not a system icon, treat as a URL
                String newSrc = src;
                //change [src=..."] to [src="]
                if (newSrc.indexOf("\"") > 3)
                    newSrc = newSrc.substring(0, newSrc.indexOf("src=") + 4) + newSrc.substring(newSrc.indexOf("\""));
                //then change [src="] to a [src="file://] URL
                if ((newSrc.indexOf("src=\"") == 0) && newSrc.length() > 5) {
                    if (newSrc.substring(5, 6).matches("^[a-zA-Z0-9]")) {
                        newSrc = newSrc.replace("src=\"", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    } else if (newSrc.indexOf("src=\"/") == 0) {
                        newSrc = newSrc.replace("src=\"/", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    }
                }

                // Lock the image to the screen width
                int paddingW = Math.round(16 * (Resources.getSystem().getDisplayMetrics().xdpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int paddingH = Math.round(16 * (Resources.getSystem().getDisplayMetrics().ydpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int maxW = Resources.getSystem().getDisplayMetrics().widthPixels - paddingW;
                int maxH = Resources.getSystem().getDisplayMetrics().heightPixels - paddingH;

                if (isNullOrEmpty(widthS) && isNullOrEmpty(heightS)) {
                    newStr += curStr.replace(">","style=\"width: auto; max-width: "+maxW+"px; height: auto; max-height: "+maxH+"px; \">") + endOfStr;
                    continue;
                }

                // ** Remove leading quote (") character if present **
                if (!isNullOrEmpty(widthS)) {
                    if (widthS.startsWith("\"")) { widthS = widthS.substring(1); }
                }
                if (!isNullOrEmpty(heightS)) {
                    if (heightS.startsWith("\"")) { heightS = heightS.substring(1); }
                }

                int w = isNullOrEmpty(widthS) ? 0 : Integer.parseInt(widthS);
                int h = isNullOrEmpty(heightS) ? 0 : Integer.parseInt(heightS);

                //if width/height are set, reduce them to maxW/maxH
                if (w > maxW) {
                    if (!isNullOrEmpty(heightS)) h = Math.round(h*maxW/w);
                    w = maxW;
                }
                if (h > maxH) {
                    if (!isNullOrEmpty(widthS)) w = Math.round(w*maxH/h);
                    h = maxH;
                }

                curStr = curStr.replace(newSrc, String.format("%s\"PUTSPACEHEREwidth=%sPUTSPACEHEREheight=%s",newSrc,w,h));
                curStr = curStr.replace("PUTSPACEHERE"," ");
                curStr = curStr.replace(">","style=\"width: auto; max-width: "+maxW+"px; height: auto; max-height: "+maxH+"px; \">");
                newStr += curStr + endOfStr;
//                newStr += curStr.replace(newSrc, String.format("%s\" width=%s height=%s",newSrc,w,h)) + endOfStr;


            } catch (Exception e) {
                Log.e("fixImagesSize","unable parse "+curStr,e);
            }
        } while (hasImg);
        return newStr;
    }

    private static String fixVideosLinks (String str, String srcDir) {

        boolean hasVid = str.contains("<video");

        String endOfStr = str;
        String newStr= str;
        if (!hasVid) return str;

        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        do {
            int firstVid = endOfStr.indexOf("<video");
            if (firstVid==-1) {
                hasVid=false;
                continue;
            }
            hasVid = firstVid >=0;
            String curStr = endOfStr.substring(firstVid);
            int endVid = curStr.indexOf(">");
            curStr = curStr.substring(0,endVid+1);
            endOfStr = endOfStr.substring(firstVid+curStr.length());

            newStr = newStr.substring(0,newStr.indexOf(curStr));
            Matcher matcher = pattern.matcher(curStr);

            if (matcher.groupCount()==0) continue;

            //Make sure the video starts and loops automatically
            if (!curStr.contains(" autoplay ") && !curStr.contains(" autoplay>"))
                curStr = curStr.replace(">"," autoplay>");
            if (!curStr.contains(" loop ") && !curStr.contains(" loop>"))
                curStr = curStr.replace(">"," loop>");

            String src = null, widthS = null, heightS = null, widthBase = null, heightBase = null;
            try {
                while (matcher.find()) {
                    String group = matcher.group();
                    if (group.startsWith("src=")) src = group;
                    else if (group.startsWith("width=")) {
                        widthBase = group;
                        widthS = group.substring(6);
                    }
                    else if (group.startsWith("height=")) {
                        heightBase = group;
                        heightS = group.substring(7);
                    }
                }

                if (isNullOrEmpty(src)) {
                    newStr += curStr + endOfStr;
                    continue;
                }

                String newSrc = src;
                //change [src=..."] to [src="]
                if (newSrc.indexOf("\"") > 3)
                    newSrc = newSrc.substring(0, newSrc.indexOf("src=") + 4) + newSrc.substring(newSrc.indexOf("\""));
                //then change [src="] to a [src="file://] URL
                if ((newSrc.indexOf("src=\"") == 0) && newSrc.length() > 5) {
                    if (newSrc.substring(5, 6).matches("^[a-zA-Z0-9]")) {
                        newSrc = newSrc.replace("src=\"", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    } else if (newSrc.indexOf("src=\"/") == 0) {
                        newSrc = newSrc.replace("src=\"/", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    }
                }

                // Lock the image to the screen width
                int paddingW = Math.round(16 * (Resources.getSystem().getDisplayMetrics().xdpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int paddingH = Math.round(16 * (Resources.getSystem().getDisplayMetrics().ydpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int maxW = Resources.getSystem().getDisplayMetrics().widthPixels - paddingW;
                int maxH = Resources.getSystem().getDisplayMetrics().heightPixels - paddingH;

                // ** Remove leading quote (") character if present **
                if (!isNullOrEmpty(widthS)) {
                    if (widthS.startsWith("\"")) { widthS = widthS.substring(1); }
                }
                if (!isNullOrEmpty(heightS)) {
                    if (heightS.startsWith("\"")) { heightS = heightS.substring(1); }
                }

                int w = isNullOrEmpty(widthS) ? 0 : Integer.parseInt(widthS);
                int h = isNullOrEmpty(heightS) ? 0 : Integer.parseInt(heightS);

                //if width and height are not both present, set width only
                //width = maxW if video > maxW or width <= 0
                if (isNullOrEmpty(widthS) || isNullOrEmpty(heightS)) {
                    if ((w <= 0) || (w > maxW)) w = maxW;
                    curStr = curStr.replace(">", " width=\"" + w + "\">");
                    newStr += curStr + endOfStr;
                    continue;
                }

                //if width/height are set, reduce them to maxW/maxH
                if (w > maxW) {
                    if (!isNullOrEmpty(heightS)) h = Math.round(h*maxW/w);
                    w = maxW;
                }
                if (h > maxH) {
                    if (!isNullOrEmpty(widthS)) w = Math.round(w*maxH/h);
                    h = maxH;
                }

                curStr = curStr.replace(widthBase, "width=\"" + w);
                curStr = curStr.replace(heightBase, "height=\"" + h);

                newStr += curStr + endOfStr;


            } catch (Exception e) {
                Log.e("fixImagesSize","unable parse "+curStr,e);
            }
        } while (hasVid);
        return newStr;

    }

    private static boolean isNullOrEmpty(String string) {
        return (string == null || string.equals(""));
    }

    public static String QspStrToStr(String str) {
        String result = "";
        if (str != null && str.length() > 0) {
            result = str.replaceAll("\r", "");
        }
        return result;
    }

    public static String QspPathTranslate(String str) {
        if (str == null)
            return null;
        //In QSP, the folders are separated by the \ sign, as in DOS and Windows, for Android we translate this into /.
        //T.k. The first argument is regexp, then we escape twice.
        String result = str.replaceAll("\\\\", "/");
        result = result.replace("src=\"file:///","/");
        return result;
    }

    private static void CheckNoMedia(String path) {
        //Создаем в папке QSP пустой файл .nomedia
        File f = new File(path);
        if (f.exists())
            return;
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String GetDefaultPath(Context context) {
        //Возвращаем путь к папке с играми.
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return null;

		// ** original code for checking games directory **
        // File sdDir = Environment.getExternalStorageDirectory();
		// ** begin replacement code for checking storage directory **

		String strSDCardPath = System.getenv("SECONDARY_STORAGE");
		if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
			strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
		}
        File sdDir = new File (strSDCardPath);
		// ** end replacement code for checking storage directory **




        if (sdDir.exists() && sdDir.canWrite()) {
            String flashCard = sdDir.getPath();
            String tryFull1 = flashCard + "/qsp/games";
            String tryFull2 = tryFull1 + "/";
            String noMedia = flashCard + "/qsp/.nomedia";
            File f = new File(tryFull1);
            if (f.exists()) {
                CheckNoMedia(noMedia);
                return tryFull2;
            } else {
                if (f.mkdirs()) {
                    CheckNoMedia(noMedia);
                    return tryFull2;
                }
            }
        }
        return null;
    }

    public static String GetGamesPath(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String path = settings.getString("gamesdir", null);
        return (path != null && !TextUtils.isEmpty(path)) ? path : GetDefaultPath(context);
    }

    public static void WriteLog(String msg) {
        Log.i("QSP", msg);
    }

    public static void ShowError(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public static void ShowInfo(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void DeleteRecursive(File f) {
        if ((f == null) || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File currentFile : files)
                DeleteRecursive(currentFile);
        }
        f.delete();
    }

    static final String DEBUGKEY =
            "3082030d308201f5a003020102020477110239300d06092a864886f70d01010b05003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3131303933303038353133325a170d3431303932323038353133325a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730820122300d06092a864886f70d01010105000382010f003082010a0282010100f6f9003587afdadb16c3e929e5f9eb61becb48b982d7481b74e997539f3e0a64e36098b34d8b03d9ac1ad36f947c8d3de341a484393ae2e1164b1bed736b73f700e0019831cb1b889f17361694e46c73ecaffcde8dec93fc2ab0bc905947602286e8251971f4205345d3d386cc8ea5cc8bceec248ba7d947728375604d981c76ad69edf020f683a7898d6312df58948a351376c8f5ce030f2b9f8a520445840145647ee9121f41bff315d7ea7d314992356e5d01eefbe16de2d4fb1f978d8df06f148a4b4848cc9a6f63d79291cadaa201eaf3b80a1501184e99a94f42fc5915e7149866124eda49fde4b41b9865fd1b0d7efeb4959b8c84b7b61041e378f7450203010001a321301f301d0603551d0e04160414a08e0f7ad0c9295a95d6011696065cdd09af55fa300d06092a864886f70d01010b050003820101005c335d7f65db96806ac34517eaaa2d7df5a4f273090a7f3881e213e91535790e0e957b66bcdebeab7d23b8198160b2e8d06736d3c57ca807a39e67c58719ae5f8570bed452b71bffe0c3b491bc0cce35e81ed8951c028e6b7296345337b4285dc383a77938c6611f27f67f338c9a0f6355af6e158bb40b328a81c9a22fbd3b7a4e48a57a93f8350712935d88b295dee9523c25a9d3cb819bfff953de5d348d8ec0a6ff861a6bcaf1df65eb168d02ffc88a797132c203ccf06195a6c783f71b757d929dc30c989701b10488182a6ceb39ed89d6988ecc3f714a57df6d0e08e9e4ac706ca1f43edaaff3b1a71edfcd260d0db1a4416af7b1d79459d4028131c525";

    public static boolean signedWithDebugKey(Context context, Class<?> cls) {
        boolean result = false;
        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature sigs[] = pinfo.signatures;
            for (int i = 0; i < sigs.length; i++)
                WriteLog(sigs[i].toCharsString());
            if (DEBUGKEY.equals(sigs[0].toCharsString())) {
                result = true;
                WriteLog("package has been signed with the debug key");
            } else {
                WriteLog("package signed with a key other than the debug key");
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
        return result;
    }

    //decodes image and scales it to reduce memory consumption 
    //(пока что не используется)
    public static Bitmap decodeImageFile(File f) {
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            //Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

    /*
    * @return boolean return true if the application can access the internet
    */
    public static boolean haveInternet(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return false;
        }
        return true;
    }
}