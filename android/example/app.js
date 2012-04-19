// This is a test harness for your module
// You should do something interesting in this harness 
// to test out the module and to provide instructions 
// to users on how to use it by example.


// open a single window
var window = Ti.UI.createWindow({
	backgroundColor:'white'
});
var label = Ti.UI.createLabel();
window.add(label);
window.open();

// TODO: write your module tests here
var tiosm = require('net.tiosm');
Ti.API.info("loading module => " + tiosm);

label.text = tiosm.example();

Ti.API.info("module exampleProp is => " + tiosm.exampleProp);
tiosm.exampleProp = "This is a test value";

if (Ti.Platform.name == "android") {
	var mapview = tiosm.createView({
		// You can also path these parameters such as other TiUiView
	 	// backgroundColor: "red",
	 	// width: 200,
	 	// height: 200,
	 	// top: 50,
	 	// left: 50,

    	mapType: tiosm.MAPQUESTOSM,
    	region: {latitude:37.390749, longitude:-122.081651,
            latitudeDelta:0.01, longitudeDelta:0.01},
    	animate:true,
    	regionFit:true,
    	userLocation:true,
	});

	mapview.setMapType(tiosm.MAPNIK);
	
	// Currently, lattitudeDelta and longitudeDelta don't work.
	// I don't have any solutions now.
	// Instead, you can use mapview.zoom() method.
	// For more information about this problem, see the following post.
	// Issue 123: Problem in zoomToSpan 
	// http://code.google.com/p/osmdroid/issues/detail?id=123 
	mapview.zoom(3);
	window.add(mapview);
}

