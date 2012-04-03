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
		message: "Creating an example Proxy",
		backgroundColor: "red",
		width: 200,
		height: 200,
		top: 50,
		left: 50
	});
	window.add(mapview);
}

