# QuestPlayer
Fork of BOOMik's Quest Player for Android

Quest Player: Android apk that runs QSP games.

This will basically be a series of bug fixes to BOOMik's Quest Player for Android, tested on a 7" Amazon Fire Tablet (Android 5.1.1). Requires a minimum of Android 4.4 to function.

TO DO:
1. Give player option to change max height of images relative to screen size
2. Auto-play videos on page
3. Fix table formatting

Version 1.5.0 changes:
1. Multi-language support added (partial Russian translation, Chinese translation soon)
2. Game creates separate save sets for each game (so, max of 5 saves per game instead of 5 saves total)
3. Fixed game crash due to presence of "%" symbol

Version 1.4.3 changes:
1. Non-ASCII character support
2. Fixed issue with multiple-command link not parsing commands properly

Stuff Fixed as of 1.4.2:
1. Inventory menu icon doesn't turn orange any more. Character description menu icon no longer animates on update (still turns orange).
2. Displayed images scaled to fit screen height as well as width.
3. Found and translated all Russian texts to English (I think).
4. Changed TextView to WebView (Android 4.4+ only) -> Center/Tables codes implemented
5. Most images will zoom on a long-click
6. GIFs animate properly on Android OS 4.4.4
7. Video files load properly and loop when started

