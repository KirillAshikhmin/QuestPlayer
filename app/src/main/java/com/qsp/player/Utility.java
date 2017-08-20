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
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.PermissionChecker;
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
    public static Spanned QspStrToHtml(String str, ImageGetter imgGetter, String srcDir, int maxW, int maxH, boolean fitToWidth) {
        if (str != null && str.length() > 0) {
            str = str.replaceAll("\r", "<br>");
            str = str.replaceAll("(?i)</td>", " ");
            str = str.replaceAll("(?i)</tr>", "<br>");

            str = fixImagesSize(str,srcDir,true,maxW,maxH,fitToWidth);

            return Html.fromHtml(str, imgGetter, null);

        }
        return Html.fromHtml("");
    }

    public static String QspStrToWebView(String str, String srcDir, int maxW, int maxH, boolean audioIsOn,boolean fitToWidth) {
        if (str != null && str.length() > 0) {
//            Utility.WriteLog(str);
            str = str.replaceAll("\r", "<br>");
//            str = str.replaceAll("(?i)</td>", " ");
//            str = str.replaceAll("(?i)</tr>", "<br>");

            str = fixImagesSize(str,srcDir,false,maxW,maxH,fitToWidth);

            str = fixVideosLinks(str,srcDir,maxW,maxH,audioIsOn);


//            str = QspPlayerStart.freshPageURL.replace("REPLACETEXT", str);
//Utility.WriteLog("toWebView:\n"+ str);

            return str;

        }
        return "";
    }

    public static String addSpacesWithChar(String str, String target,boolean addBefore, boolean addAfter) {

//Utility.WriteLog("[href] = \""+str+"\", [target] = \""+target+"\"");

        //Check if the string has the target character set
        boolean hasTarget = str.toLowerCase().contains(target.toLowerCase());
        if ( !hasTarget || (!addBefore && !addAfter) ) return str;

        int targetLength = target.length();
        String endOfStr = str;
        String newStr = "";

        do {
            int targetIndex = endOfStr.toLowerCase().indexOf(target.toLowerCase());

            //Add to newStr any text up to the target
            if (targetIndex > 0)
                newStr += endOfStr.substring(0,targetIndex);
            //Set endOfStr to everything after the target, but be sure not to go past
            //the length of endOfStr
            if (endOfStr.length() > targetIndex + targetLength)
                endOfStr = endOfStr.substring(targetIndex + targetLength);
            else endOfStr = "";

//Utility.WriteLog("[newStr] = \"" +newStr+"\", [target] = \""+target+"\", [endOfStr] = \""+endOfStr+"\", [targetIndex] = "+targetIndex+", [targetLength] = "+targetLength);

            //addBefore: if there are characters before the target, add a space if there isn't one
            if ((addBefore) && (newStr.length() > 0) && (newStr.charAt(newStr.length()-1) != ' '))
                newStr += " ";

            newStr += target;

            //addAfter: if there are characters after the target, add a space if there isn't one
            if( (addAfter) && (endOfStr.length() > 0) && (endOfStr.charAt(0) != ' ') )
                newStr += " ";

            hasTarget = endOfStr.toLowerCase().contains(target.toLowerCase());
        } while (hasTarget);

        //finish the string
        newStr += endOfStr;

//Utility.WriteLog("[newStr] = \""+newStr+"\"");
        return newStr;
    }

    //This encodes all (+) symbols found in Href tags to (%2b) for URLDecoder; all other (+)
    //symbols are changed to QSPPLUSSYMBOLCODE for later replacement
    public static String replaceHrefPlusSymbols(String str) {
        String curStr = "";
        String newStr = "";

        if (!str.toLowerCase().contains("href") || !str.contains("<") || !str.contains(">")) return str;

        int startStr = 0;
        int endStr = 0;

        String remainingStr = str;

        do {
            //Mark the next (<)
            endStr = remainingStr.indexOf("<");
            //if (<) is not at the end of remainingStr, encode all (+), save up to (<) and then
            //delete it from remainingStr
            if (endStr < remainingStr.length()) {
                if (endStr > 0) newStr += remainingStr.substring(0, endStr).replace("+","QSPPLUSSYMBOLCODE");
                remainingStr = remainingStr.substring(endStr);
            }
            //if (<) is at the end, just add the rest of remainingStr to newStr and exit
            else break;

            //Same as with (<), but mark to (>) and save (<...>) as curStr for processing
            if (remainingStr.contains(">")) {
                endStr = remainingStr.indexOf(">");
                curStr = remainingStr.substring(0,endStr);
                remainingStr = remainingStr.substring(endStr);
            }
            else break;

            //Replace all the (+) symbols within the (<...href...>) block with (%2b) then
            //add the processed curStr to newStr
            if (curStr.contains("href") && curStr.contains("+")) {
                newStr += curStr.replace("+","%2b");
            }
            //if (href) is not present in (<...>), add curStr to newStr and continue
            else newStr += curStr;

        } while (remainingStr.toLowerCase().contains("<"));

        newStr += remainingStr.replace("+","QSPPLUSSYMBOLCODE");
        return newStr;
    }

    //This function corrects all "exec:" commands so that the '+' is not lost when the URL is
    //loaded into WebView but instead becomes "%2b"
    public static String encodeExec(String str) {
        boolean hasExec = str.contains("exec:");
        if (!hasExec) return str;

        String endOfExecStr = str;
        String newStr = "";
        String execStr;
        int execIndex;
        int quoteIndex;

        do {
            execIndex = endOfExecStr.toLowerCase().indexOf("exec:");
            newStr += addSpacesWithChar(replaceHrefPlusSymbols(endOfExecStr.substring(0, execIndex)),"&",true,true);


            quoteIndex = endOfExecStr.indexOf("\"");

            //execStr includes 'exec:' to the next '"' or to the end of endOfStr
            //endOfStr starts after execStr or becomes ""
            if (quoteIndex > execIndex) {
                execStr = endOfExecStr.substring(execIndex, quoteIndex);
                endOfExecStr = endOfExecStr.substring(quoteIndex);
            }
            else {
                execStr = endOfExecStr.substring(execIndex);
                endOfExecStr = "";
            }

            //Replace all '+' with the URL-codable '+' and attach to newStr

            newStr += addSpacesWithChar(replaceHrefPlusSymbols(execStr),"&",true,true);
//            newStr += addSpacesWithChar(execStr.replace("+","%2b"),"&",true,true);

            hasExec = endOfExecStr.contains("exec:");
        } while (hasExec);
        //Utility.WriteLog("testend");

        newStr += endOfExecStr;
        return newStr;
    }

    public static String fixImagesSize(String str, String srcDir, boolean isForTextView, int maxW, int maxH, boolean fitToWidth) {
        boolean hasImg = str.contains("<img");
        boolean countedImg = false;
        int inTable = 0;
        int imgsInLine = 0;

        int fisCycles = 0;

        Utility.WriteLog("fixImagesSize: "+str);

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

            //First, if there is a table starting/ending, take care of it
            // ** START TABLE CHECK **
            int openTable = endOfStr.indexOf("<table");
            int closeTable = endOfStr.indexOf("</table");

            //if <table is found before <img or </table, inTable++
            if ((openTable >= 0)
                    && (openTable < firstImg)
                    && ( (openTable < closeTable) || (closeTable < 0) )) {
                inTable++;
                Utility.WriteLog("inTable+ = "+inTable);

                String tableStr = endOfStr.substring(openTable);
                int endTableTag = tableStr.indexOf(">");
                tableStr = tableStr.substring(0,endTableTag+1);
                endOfStr = endOfStr.substring(openTable+tableStr.length());

                newStr += tableStr;
                continue;
            }
            //if inside a <table> AND </table is found before <img or <table, inTable--
            if ((inTable > 0)
                    && (closeTable >= 0)
                    && (closeTable < firstImg)
                    && ((closeTable < openTable) || (openTable < 0))) {
                inTable--;
                Utility.WriteLog("inTable- = "+inTable);

                String tableStr = endOfStr.substring(closeTable);
                int endTableTag = tableStr.indexOf(">");
                tableStr = tableStr.substring(0,endTableTag+1);
                endOfStr = endOfStr.substring(closeTable+tableStr.length());

                newStr += tableStr;
                continue;
            }
            // ** END TABLE CHECK **

            //Second, if this is a new line (after a <br> or very start of the string), count all
            //the <img tags for this line; skip if currently inside a table
            // ** START OF COUNT IMGS FOR LINE
            if (inTable == 0) {
                int breakIndex = endOfStr.indexOf("<br");
                //all <br> before <img covered, so count <img if <img not counted yet
                if (!countedImg) {
                    //count to the next <br, or end of string if no more <br
                    if ((breakIndex < 0))
                        breakIndex = endOfStr.length();
                    String imgStr = endOfStr.substring(0,breakIndex);

                    imgsInLine = getImgsInLine(imgStr);

                    countedImg = true;
//Utility.WriteLog("imgStr = " + imgStr);
//Utility.WriteLog("imgsInLine = " + imgsInLine);
                }
                //if <br> comes before <img AND already counted images
                if ((breakIndex >= 0) && (breakIndex < firstImg) && (countedImg)) {
                    countedImg = false;

                    String breakStr = endOfStr.substring(breakIndex);
                    int endBreak = breakStr.indexOf(">");
                    breakStr = breakStr.substring(0, endBreak + 1);
                    endOfStr = endOfStr.substring(breakIndex + breakStr.length());

                    newStr += breakStr;
                    continue;
                }
            }
            //** END OF COUNT IMGS FOR LINE

            hasImg = firstImg >=0;
            String curStr = endOfStr.substring(firstImg);
            int endImg = curStr.indexOf(">");
            curStr = curStr.substring(0,endImg+1);
            endOfStr = endOfStr.substring(firstImg+curStr.length());

            newStr = newStr.substring(0,newStr.indexOf(curStr));
            Matcher matcher = pattern.matcher(curStr);


            if (matcher.groupCount()==0) continue;

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

                curStr = "<img "+src+"\" >";

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
                    boolean testURIOK = newSrc.contains("://");
                    if (newSrc.substring(5, 6).matches("^[a-zA-Z0-9]") && !testURIOK) {
                        newSrc = newSrc.replace("src=\"", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    } else if (newSrc.indexOf("src=\"/") == 0) {
                        newSrc = newSrc.replace("src=\"/", "src=\"file://" + srcDir);
                        curStr = curStr.replace(src, newSrc);
                    }
                }

//Utility.WriteLog(newSrc);
                if (!newSrc.contains("///")) {
                    newStr += curStr + endOfStr;
                    continue;
                }
//Utility.WriteLog(newSrc.substring(newSrc.indexOf("///")+2));
                BitmapFactory.Options imgDim = getImageDim(newSrc.substring(newSrc.indexOf("///")+2));
                if (imgDim == null) {
//Utility.WriteLog("imgDim is null");
                    newStr += curStr + endOfStr;
                }
                else {
//Utility.WriteLog("imgDim.outWidth = " + imgDim.outWidth + ", imgDim.outHeight = " + imgDim.outHeight);


//                if (isNullOrEmpty(widthS) && isNullOrEmpty(heightS)) {
//                    newStr += curStr.replace(">","style=\"width: 100%; max-width: "+maxW+"px; height: auto; max-height: "+maxH+"; \">") + endOfStr;
                    int h = imgDim.outHeight;
                    int w = imgDim.outWidth;
                    if (maxH > 0) {
                        if ((fitToWidth && (w < maxW)) || (w > maxW)) {
                            h = Math.round(h * maxW / w);
                            w = maxW;
                        }
                        if (h > maxH) {
                            w = Math.round(w * maxH / h);
                            h = maxH;
                        }
                    }
                    //skip height adjustment if maxH <= 0
                    else if ((fitToWidth && (w < maxW)) || (w > maxW))
                        w = maxW;

                        //if in a table, use 100% for the width so to not override the table
                    if (inTable > 0)
                        newStr += curStr.replace(">", "width = \"100%\">") + endOfStr;
                    //if not in a table AND more than one image in the line AND Image Fit to Width,
                    //multiply all the images by 1/imgsInLine. Otherwise, let WebView handle it
                    else {
                        if ((imgsInLine > 1) && (fitToWidth)) {
                            w = Math.round(w/imgsInLine);
                            h = Math.round(h/imgsInLine);
                        }
                        if (maxH > 0) //add width and height if maxH > 0
                            newStr += curStr.replace(">", "width = \"" + w + "\" height = \"" + h + "\">") + endOfStr;
                        else //add width only if maxH <= 0
                            newStr += curStr.replace(">", "width = \"" + w + "\" >") + endOfStr;
                    }
                    fisCycles++;
                    Utility.WriteLog("endOfStr "+fisCycles+": "+endOfStr);
                    continue;
                }

            } catch (Exception e) {
                Log.e("fixImagesSize","unable to parse "+curStr,e);
            }

        } while (hasImg);
Utility.WriteLog("newStr: "+newStr);
        return newStr;
    }

    private static BitmapFactory.Options getImageDim (String imgSrc) {
        File imgFile = new File(imgSrc);
        //if the imgFile doesn't exist, return null
        if(!imgFile.exists()) {
            Utility.WriteLog("Image File doesn't exist");
            return null;
        }
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgSrc, opt);

        return opt;
    }

    private static int getImgsInLine (String imgStr) {
        int firstImgIndex = 0;
        int endImgIndex = 0;
        int totalImages = 0;
        boolean spacePresent = false;

        while (firstImgIndex != -1) {
            //mark the start of the <img> tag
            firstImgIndex = imgStr.indexOf("<img", firstImgIndex);

//            Utility.WriteLog("totalImages: "+totalImages+", firstImgIndex: "+firstImgIndex+", endImageIndex: "+endImgIndex+", difference = "+(firstImgIndex-endImgIndex));
            //if there are multiple images and there are spaces or characters between the images,
            //treat the images as if on separate lines
            if ((firstImgIndex >= 0) && (firstImgIndex-endImgIndex > 1)) spacePresent = true;
            if ((totalImages > 1) && (spacePresent))
                return 1;

            //if there are no breaks between images, mark the end of the <img> tag
            endImgIndex = imgStr.indexOf(">", firstImgIndex);

            //add up each image as it comes
            if (firstImgIndex != -1) {
                totalImages++;
                firstImgIndex += "<img".length();
            }

        }

        return totalImages;
    }

    private static String fixVideosLinks (String str, String srcDir, int maxW, int maxH,boolean audioIsOn) {

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
            if (!audioIsOn && !curStr.contains(" muted ") && !curStr.contains(" muted>"))
                curStr = curStr.replace(">"," muted>");

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
/*                int paddingW = Math.round(16 * (Resources.getSystem().getDisplayMetrics().xdpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int paddingH = Math.round(16 * (Resources.getSystem().getDisplayMetrics().ydpi / Resources.getSystem().getDisplayMetrics().DENSITY_DEFAULT));
                int maxW = Resources.getSystem().getDisplayMetrics().widthPixels - paddingW;
                int maxH = Resources.getSystem().getDisplayMetrics().heightPixels - paddingH;
*/
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
Utility.WriteLog("toStr:\n"+str);
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

        File sdDir;
		String strSDCardPath = System.getenv("SECONDARY_STORAGE");
		if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
			strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
		}
        if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
            strSDCardPath = Environment.getExternalStorageDirectory().getPath();

        }
		// ** end replacement code for checking storage directory **

        sdDir = new File (strSDCardPath);



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
                .setTitle(R.string.errorTitle)
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

    //Remove all non-alphanumeric characters ("_" acceptable) from String for use as a file name
    public static String safetyString(String target) {
        if (isNullOrEmpty(target)) return "GameX";

        int i = 0;
        String newStr = target;

        if ( newStr.contains("/") && (i < newStr.length()) )
            do {
                i = newStr.substring(i).indexOf("/")+1;
                newStr = newStr.substring(i);
            } while ( newStr.contains("/") && (i < newStr.length()) );

        //Make the string alphanumeric except for "_" and "."
        newStr = newStr.replaceAll("[^a-zA-Z0-9_.]","");
        //Remove the ".qsp" extension at the end
        newStr = newStr.replace(".qsp","");

        if (newStr == "") {
            newStr = "GameX" + target.length();
            Utility.WriteLog("\""+ target + "\" could not be parsed. Using \""+newStr+"\" for save file prefix.");
        }

        return newStr;
    }


}