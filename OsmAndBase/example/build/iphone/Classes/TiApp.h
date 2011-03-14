/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */

#import <UIKit/UIKit.h>

#import "TiHost.h"
#import "KrollBridge.h"
#ifdef USE_TI_UIWEBVIEW
	#import "XHRBridge.h"
#endif
#import "TiRootViewController.h"

@interface TiApp : TiHost <UIApplicationDelegate> 
{
	UIWindow *window;
	UIImageView *loadView;
	BOOL splashAttached;
	BOOL loaded;
	BOOL handledModal;
	
	KrollBridge *kjsBridge;

#ifdef USE_TI_UIWEBVIEW
	XHRBridge *xhrBridge;
#endif
	
	NSMutableDictionary *launchOptions;
	NSTimeInterval started;
	
	int networkActivityCount; //We now can use atomic increment/decrement instead. This value is 0 upon initialization anyways.
	
	// TODO: Create a specialized SplitView controller if necessary
	UIViewController<TiRootController> *controller;
	NSString *userAgent;
	NSString *remoteDeviceUUID;
	
	id remoteNotificationDelegate;
	NSDictionary* remoteNotification;
	
	NSString *sessionId;

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	UIBackgroundTaskIdentifier bgTask;
	NSMutableArray *backgroundServices;
	NSMutableArray *runningServices;
	UILocalNotification *localNotification;
#endif	
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, assign) id remoteNotificationDelegate;
@property (nonatomic, readonly) NSDictionary* remoteNotification;
@property (nonatomic, retain) UIViewController<TiRootController>* controller;

+(TiApp*)app;
//Convenience method
+(UIViewController<TiRootController>*)controller;

-(void)attachXHRBridgeIfRequired;

-(BOOL)isSplashVisible;
-(void)hideSplash:(id)event;
-(UIView*)splash;
-(void)loadSplash;
-(UIView*)attachSplash;
-(NSDictionary*)launchOptions;
-(NSString*)remoteDeviceUUID;

-(void)startNetwork;
-(void)stopNetwork;

-(void)showModalError:(NSString*)message;

-(void)showModalController:(UIViewController*)controller animated:(BOOL)animated;
-(void)hideModalController:(UIViewController*)controller animated:(BOOL)animated;


-(NSString*)userAgent;
-(NSString*)sessionId;

-(KrollBridge*)krollBridge;


#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0

-(void)beginBackgrounding;
-(void)endBackgrounding;
-(void)registerBackgroundService:(TiProxy*)proxy;
-(void)unregisterBackgroundService:(TiProxy*)proxy;
-(void)stopBackgroundService:(TiProxy*)proxy;
-(UILocalNotification*)localNotification;
#endif

@end

