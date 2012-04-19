What is TiOsm?
--------------------
TiOsm is a titanium mobile module enabling a titanium mobile application on Android devices to access [open streat map](http://www.openstreetmap.org/).
In appcelerator's open mobile marketplace, there're already [OpenStreetMap Module](https://marketplace.appcelerator.com/apps/2039), but it currently supports only iOS platform and [will not support Android in the near future](https://marketplace.appcelerator.com/apps/2039#questions).
So I decide to write a titanium module to bridge [osmdroid](http://code.google.com/p/osmdroid/).


Prerequired
--------------------
You have to download the following java libraries and place them under android/lib
* [osmdroid-android-3.0.7.jar](http://code.google.com/p/osmdroid/downloads/list)
* [slf4j-android-1.5.8.jar](http://www.slf4j.org/android/)


How to build
--------------------
First of all, edit titanium platform, Android SDK and NDK paths in the build.properties to fit your environment.
Then just type 
    
    ant

You'll get the module binary in the following directory.

    android/dist/net.tiosm-android-0.1.zip


How to use
--------------------
To access this module from JavaScript, see the following example:

	android/example/app.js

The following command will build and install an example app into the android emulator.

    ant run


TODOs:
--------------------
- MyLocationOverlay
- Annotations
- To support more features osmdroid has