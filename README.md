# PROJECT MAINTENANCE PAUSED

This project is no longer maintained due to the lack of time and availability. I'm a developer to and I know how frustrating can be when a tool I use no is no longer available or doesn't work any more. I'm sorry for the pain I've caused. :( Still in this repo the is a CLI tool that can work without the help of the IDE, and uses pure Dart to generate files in the same format. https://github.com/long1eu/flutter_i18n/tree/master/flutter_l10n Please give it a try, maybe it can ease your pain. 

As of today I requested Intellij to remove the plugin from the market. Thanks for all the support. 

# Synopsis

This plugin helps you internationalize you Flutter app by generating the needed boiler plate code. You just add and organize strings in files that are contained in the /res/values folder. This plugin is based on the [Internationalizing Flutter Apps](https://flutter.io/tutorials/internationalization/) tutorial and on the [Material Library Localizations](https://github.com/flutter/flutter/tree/master/packages/flutter_localizations/lib/src/l10n) package. Much of the instructions are taken from there.

# Usage

### 1. Setup you App

Setup your localizationsDelegates and your supportedLocales which allows the access to the internationalized strings.

<pre style="margin-left: 80px;">class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      onGenerateTitle: (BuildContext context) => S.of(context).app_name,
      <b>localizationsDelegates: [
            S.delegate,
            // You need to add them if you are using the material library.
            // The material components usses this delegates to provide default 
            // localization      
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,               
      ],
      supportedLocales: S.delegate.supportedLocales,</b>
      title: 'Flutter Demo',
      theme: new ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: new MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}</pre>

Optionally, you can provide a fallback Locale for the unsupported languages in case the user changes the device language to an unsupported language. The default resolution is:

1.  The first supported locale with the same Locale.languageCode.
2.  The first supported locale.

If you want to change the last step and to provided a default locale instead of choosing the first one in you supported list, you can specify that as fallows:

<pre style="margin-left: 60px;">class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      localizationsDelegates: [
            S.delegate,
            // You need to add them if you are using the material library.
            // The material components usses this delegates to provide default 
            // localization 
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
      ],
      supportedLocales: S.delegate.supportedLocales,

      <b>localeResolutionCallback:</b>
          <b>S.delegate.resolution(fallback: const Locale('en', '')),</b>
      // this is equivalent to having <b>withCountry: false</b>, as in the next call:
      <b>localeResolutionCallback:</b>
          <b>S.delegate.resolution(fallback: const Locale('en', ''), withCountry: false),</b>

      // - OR -

      <b>localeListResolutionCallback:</b>
          <b>S.delegate.listResolution(fallback: const Locale('en', '')),</b>    
      // this is equivalent to having <b>withCountry: false</b>, as in the next call:
      <b>localeListResolutionCallback:</b>
          <b>S.delegate.listResolution(fallback: const Locale('en', ''), withCountry: false),</b>

      title: 'Flutter Demo',
      theme: new ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: new MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}</pre>

<img src="https://github.com/long1eu/flutter_i18n/blob/master/extras/arb_icon.png?raw=true" width="150">

### 2.  Setup the arb files. 

ARB files extension stands for [Application Resource Bundle](https://github.com/googlei18n/app-resource-bundle) which is used by the Dart [intl](https://pub.dartlang.org/packages/intl) package. ARB files are supported by the [Google Translators Toolkit](https://translate.google.com/toolkit), thus supported by Google.

Flutter internalization only depends on a small subset of the ARB format. Each .arb file contains a single JSON table that maps from resource IDs to localized values. Filenames contain the locale that the values have been translated for. For example, material_de.arb contains German translations, and material_ar.arb contains Arabic translations. Files that contain regional translations have names that include the locale's regional suffix. For example, material_en_GB.arb contains additional English translations that are specific to Great Britain.

<b>The first English file is generated for you(/res/values/strings_en.arb)</b>. Every arb file depends on this one. If you have a string in the German arb file(/res/values/strings_de.arb) that has an ID which is <b>not found</b> in the English file, it would not be listed. So you must be sure to first have the strings in the English file and then add other translations.

To add a new arb file right click on <b>values</b> folder and select <b>New</b> -><b> Arb </b><b>File</b>. Then pick your language from the list, and region if necessary.

#### 1. Referencing the values

The ARB table's keys, called resource IDs, are valid Dart variable names. They correspond to methods from the S class. For example:

<pre style="margin-left: 60px;">Widget build(BuildContext context) {
  return new FlatButton(
    child: new Text(
      <b>S.of(context).cancelButtonLabel,</b>
    ),
  );
}</pre>

#### 2. Parameterized strings

Some strings may contain <em>$variable</em> tokens which are replaced with your values. For example:

    {   
        "aboutListTileTitle": "About $applicationName"  
    }

The value for this resource ID is retrieved with a parameterized method instead of a simple getter:  

    S.of(context).aboutListTileTitle(yourAppTitle)

#### 3. Plurals

Plural translations can be provided for several quantities: 0, 1, 2, "few", "many", "other". The variations are identified by a resource ID suffix which must be one of "Zero", "One", "Two", "Few", "Many", "Other" (case insensitive). The "Other" variation is used when none of the other quantities apply. All plural resources must include a resource with the "Other" suffix. For example the English translations ('material_en.arb') for selectedRowCountTitle in the [Material Library Localizations](https://github.com/flutter/flutter/tree/master/packages/flutter_localizations/lib/src/l10n) are:

    {
        "selectedRowCountTitleZero": "No items selected",
        "selectedRowCountTitleMany": "to many items", //not actual real
        "selectedRowCountTitleOne": "1 item selected",
        "selectedRowCountTitleOther": "$selectedRowCount items selected",</pre>
    }

Then, we can reference these strings as follows:

<pre>S.of(context).selectedRowCountTitle("many")</pre>

or

<pre>S.of(context).selectedRowCountTitle("1")</pre>

or

<pre>S.of(context).selectedRowCountTitle("$selectedRowCount")</pre>

### 3. Turning off the plugin per project.

With the release of v1.1.0 of the plugin, it is possible to turn on or off the plugin per project by adding
a new top-level configuration option in your project's **pubspec.yaml** file: **flutter_i18n**

The plugin will be turned off by default for Dart-only projects, and on by default for Flutter projects.
To change this setting, however, you have two options under the top-level **flutter_i18n** configuration:

**enable-flutter-i18n**: true / false

To activate the plugin for a Flutter project. The default setting is true.

**enable-for-dart**: true / false

To activate the plugin for a Dart-only project. The default setting is false.

### NOTES:
* The plugin also supports `${variable}` notation. Use this when the parser does not catch the parameters properly. For example:
    ```json
    {"tabLabel": "탭 $tabCount개 중 $tabIndex번째"}
    ```
    generates
    ```dart
    String tabLabel(String tabCount개, String tabIndex번째) => "탭 $tabCount개 중 $tabIndex번째";
    ```
    Which contains invalid Dart fields. In this case you should use 
    ```json
    {"tabLabel": "탭 ${tabCount}개 중 ${tabIndex}번째"}
    ```

* Also you can escape the `$` sign with `\` 
    ```json
    {"checkout_message": "You have to pay \$$price"}
    ```
# Issues

There are some performance improvements and bug fixes that this plugin could use, so feel free to PR.
