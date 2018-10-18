# googlecalx
Plugin for TV-Browser to export programs to Google Calendar.

This plugin allows to export programs shown in the [TV-Browser](http://www.tvbrowser.org/) to your Google Calendar.

Screenshot of export action:

![Screenshot of export action](https://raw.githubusercontent.com/smurf667/googlecalx/master/googlecalx/images/export.png)

Screenshot of settings:

![Screenshot of settings](https://raw.githubusercontent.com/smurf667/googlecalx/master/googlecalx/images/settings.png)


# Accessing the settings
You can access the plugin settings by selecting "Tools > Manage plugins..." from the menu. This opens the "Options" window. In the tree view under "Plugins" (left hand side) you will find "Google Calendar export". Clicking this entry shows the settings of the plugin.

## Configuration

### Setting your calendar
Before you can export to your calendar, you have to specify your "Calendar ID". Typically, you would use your Google Mail e-Mail address. If you have other calendars, you can pick the export target calendar once you've set your main calendar ID.

Once you have configured the settings you can right-click a program and export it into the calendar. If you do not wish to see the name of the calendar into which the program is exported, simply uncheck "show ID in export action".

### Setting title and body of the exported entry
The title and body of the exported entry are taken from the TV-Browser program information. You can use placeholders in curly brackets to access the properties of the program. Examples:
 * `{title}` - returns the title of the program
 * `{description}` - returns the description of the program
 * `{shortInfo}` - returns a summary of the program
 * `{channel.name}` - returns the name of the channel this program is broadcast on

You can find details on the properties of a program at http://www.tvbrowser.org/dev/api/tvbrowser_v3.3.3/doc/devplugin/Program.html

Extended text field entries can be accessed by giving the field name. Examples:
 * `{EPISODE_TYPE}` - returns the episode title
 * `{ORIGINAL_TITLE_TYPE}` - returns the original language episode title 

You can find details on these extended text field properties of a program at http://www.tvbrowser.org/dev/api/tvbrowser_v3.3.3/doc/devplugin/ProgramFieldType.html

Invalid placeholders result in their literal name, e.g. `{invalid}` returns `"invalid"`. If a value is not present (`"null"`), an empty string will be used.

### Reminders
You can choose between having no reminder at all, using your calendar's defaults for reminders, or set a specific type (email, popup, sms), time and color for the entry.

# FAQ

## Export does not work any more
If you get an error message which indicates an `invalid_token` maybe something went wrong with your credentials. These are stored locally with the extension, and may expire or you may have changed you password for Google etc.

Recommended solution: Delete the file `StoredCredentials` in `%userprofile%\AppData\Roaming\TV-Browser\<TV-Browser-version>\.store\googlecalx` and restart TV-Browser. Using the calendar export should prompt for your login credentials again.
This question has been asked (in German) in the TV-Browser forum at https://hilfe.tvbrowser.org/viewtopic.php?t=17003&start=105#p109952
