# QuestPlayer
Fork of BOOMik's Quest Player for Android, an apk that runs QSP games.

This is a fork of BOOMik's Quest Player, an Android port of Quest Soft Player, tested on a 7" Amazon Fire Tablet (Android 5.1.1) and a Nexus Galaxy (Android 4.4.4). Requires a minimum of Android 4.4 (KitKat). 

# Requirements:
Android 4.4 for Version 1.6.9b; Android 5.0 for Version 2.0.0+

TO DO:
1. Add support for ADDQST and DELQST actions in Quest Soft Player library
2. Find solution for Android OS Honeycomb creating blank space around main description panel
3. Finish translations of strings.xml and prefs.xml files for Simplified Chinese and Traditional Chinese
4. Fix any more bugs that show up

# Version History

Version 2.0.0 (Android 5.0)
1. "File" code replaced with "DocumentFile" and Storage Access Framework
2. Downloading games working again!
3. Save files now located to relevant game directory - if you have old save files, just move them into the game directory.
4. Update Jan 12, 2018: code fix to correct save/load crash bug
5. Update Jan 13, 2018: Select Directory option added to "No Game Directory" dialog

Version 1.6.9b (Android 4.4)
1. Fixed problem where image was not properly sized when file name contained an empty space character (" ")
2. Completed Russian and Chinese (Simplified+Traditional) translation

Version 1.6.9
1. Fixed bug that prevented some devices from accessing the External SD Card properly. Will still default to internal storage if unable to locate suitable external storage.

Version 1.6.8
1. When "Open Game File" command links to a non-existent directory, app will default to the base directory in that storage device (if possible) and display a brief alert.

Version 1.6.7
1. Fixed bug that showed "SD Card not connected" error when that was clearly not the case.

Version 1.6.6
1. Changed "auto-clear" subroutine as previous version did not correct problem.
2. Fixed crash that occurred when changing colors while in portrait mode.

Version 1.6.5
1. "Auto-Clear" settings option: activate if large empty space that sometimes appears around the actions list (only occurs on some devices). May cause some game slowdown, so the option is disabled by default.
2. Added source path correction subroutine in case of minor HTML coding errors with <img> tags.
3. More aggressive removal of unnecessary line breaks.
4. More minor bug fixes.
5. Added preview to menu for font color settings.

Version 1.6.4
1. Fixed image bugs and a few crash problems
2. Neutral image used when hiding images now conforms to the same general dimensions as the original image
3. Third panel page will now update properly on settings changes
4. Coding error in table parser (quick fix)

Version 1.6.3
1. Fixed HTML parsing so image maps will scale and be implemented properly
2. Enabled multi-file compatibility for games; if your game has multiple QSP files in a directory, each file will now appear as a separate option in the game list
3. Saved games are now localized based on game directory, not QSP file name. The save/load list created by "QSP/games/MyGame/mygame1.2.qsp" will be available to "QSP/games/mygame1.3.qsp" and vice versa.
4. Local games are now sorted by alphanumeric order
5. Fixed some menu bugs
Quick Fix!
  6. Fixed sorting error in Game Stock and improved alogrithm 
  7. Fixed bug where image and video source files with spaces didn't parse properly

Version 1.6.2
1. Reinstated Theme option for fonts style
2. Fixed bug preventing link color from updating when leaving settings
3. Changed Display Settings option to "Image Settings" and "Text Settings"
4. New Russian translations by Unregistred!
Quick fix!
  5. Update to credits page.
  6. Added new Chinese translations
  7. Fixed random crash occurring with some games

Version 1.6.1
1. Added display option to replace all game graphics with a neutral image. This allows a user to censor a game that contains violent, sexual, or obscene imagery.
2. Fixed rare data loss when a page contained tandem code repeats

Version 1.6
1. Created "About Quest Player" dialog
2. Final fixes

Version 1.5.7
1. Created directory browser to select QSP game folders directory
2. Landscape mode hides the title menu during a game to improve visibility
3. Changing the language will properly update upon exiting Settings
4. Removed non-functional "About" menu item (will add it back in later)
5. Various bug/display fixes (still going!)

Version 1.5.6 (quickfix)
1. Added settings option to switch between External SD card and Internal Storage
2. Various bug fixes (I'm detecting a pattern)

Version 1.5.5
1. Added Settings option to disable "Loading..." web display.
2. Various bug fixes

Version 1.5.4 changes:
1. App will now change to landscape or portrait mode depending on how the device is held
2. Tabulated image sizing issues corrected
3. Various bug fixes

Version 1.5.3 changes:
1. Player can limit the height of images relative to screen size
2. Loaded videos will automatically play

Version 1.5.2 changes:
1. Fixed table formatting
2. Implemented Display Settings for text/background colors, fonts, and text size; added "default" option
3. Simplified and Traditional Chinese version implemented - credit to translator illume!

Version 1.5.0 changes:
1. Bug fixes

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

