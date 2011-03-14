/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#include <stdio.h>
#include <execinfo.h>

#import "TiApp.h"
#import "Webcolor.h"
#import "TiBase.h"
#import "TiErrorController.h"
#import "NSData+Additions.h"
#import "TiDebugger.h"
#import "ImageLoader.h"
#import <QuartzCore/QuartzCore.h>
#import <AVFoundation/AVFoundation.h>

#import <libkern/OSAtomic.h>

TiApp* sharedApp;

extern NSString * const TI_APPLICATION_DEPLOYTYPE;
extern NSString * const TI_APPLICATION_NAME;
extern NSString * const TI_APPLICATION_VERSION;

extern void UIColorFlushCache();

#define SHUTDOWN_TIMEOUT_IN_SEC	3
#define TIV @"TiVerify"

//
// thanks to: http://www.restoroot.com/Blog/2008/10/18/crash-reporter-for-iphone-applications/
//
void MyUncaughtExceptionHandler(NSException *exception) 
{
	static BOOL insideException = NO;
	
	// prevent recursive exceptions
	if (insideException==YES)
	{
		exit(1);
		return;
	}
	
	insideException = YES;
    NSArray *callStackArray = [exception callStackReturnAddresses];
    int frameCount = [callStackArray count];
    void *backtraceFrames[frameCount];
	
    for (int i=0; i<frameCount; i++) 
	{
        backtraceFrames[i] = (void *)[[callStackArray objectAtIndex:i] unsignedIntegerValue];
    }
	
	char **frameStrings = backtrace_symbols(&backtraceFrames[0], frameCount);
	
	NSMutableString *stack = [[NSMutableString alloc] init];
	
	[stack appendString:@"[ERROR] The application has crashed with an unhandled exception. Stack trace:\n\n"];
	
	if(frameStrings != NULL) 
	{
		for(int x = 0; x < frameCount; x++) 
		{
			if(frameStrings[x] == NULL) 
			{ 
				break; 
			}
			[stack appendFormat:@"%s\n",frameStrings[x]];
		}
		free(frameStrings);
	}
	[stack appendString:@"\n"];
			 
	NSLog(@"%@",stack);
		
	[stack release];
	
	//TODO - attempt to report the exception
	insideException=NO;
}

@implementation TiApp

@synthesize window, remoteNotificationDelegate, controller;

+ (TiApp*)app
{
	return sharedApp;
}

+(UIViewController<TiRootController>*)controller;
{
	return [sharedApp controller];
}

-(void)startNetwork
{
	ENSURE_UI_THREAD_0_ARGS;
	networkActivityCount ++;
	if (networkActivityCount == 1)
	{
		[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
	}
}

-(void)stopNetwork
{
	ENSURE_UI_THREAD_0_ARGS;
	networkActivityCount --;
	if (networkActivityCount == 0)
	{
		[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
	}
}

-(NSDictionary*)launchOptions
{
	return launchOptions;
}

- (UIImage*)loadAppropriateSplash
{
	UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
	
	UIImage* image = nil;

	if([TiUtils isIPad])
	{
		// Specific orientation check
		switch (orientation) {
			case UIDeviceOrientationPortrait:
				image = [UIImage imageNamed:@"Default-Portrait.png"];
				break;
			case UIDeviceOrientationPortraitUpsideDown:
				image = [UIImage imageNamed:@"Default-PortraitUpsideDown.png"];
				break;
			case UIDeviceOrientationLandscapeLeft:
				image = [UIImage imageNamed:@"Default-LandscapeLeft.png"];
				break;
			case UIDeviceOrientationLandscapeRight:
				image = [UIImage imageNamed:@"Default-LandscapeRight.png"];
				break;
		}
		if (image != nil) {
			return image;
		}
			
		// Generic orientation check
		if (UIDeviceOrientationIsPortrait(orientation)) {
			image = [UIImage imageNamed:@"Default-Portrait.png"];
		}
		else if (UIDeviceOrientationIsLandscape(orientation)) {
			image = [UIImage imageNamed:@"Default-Landscape.png"];
		}
			
		if (image != nil) {
			return image;
		}
	}
	
	// Default 
	return [UIImage imageNamed:@"Default.png"];
}

- (UIView*)attachSplash
{
	UIView * controllerView = [controller view];
	
	RELEASE_TO_NIL(loadView);

	CGRect destRect;

	if([TiUtils isIPad]) //iPad, 1024*748 or 748*1004, under the status bar.
	{
		destRect = [controllerView bounds];
	}
	else //iPhone: 320*480, placing behind the statusBar.
	{
		destRect = [controllerView convertRect:[[UIScreen mainScreen] bounds] fromView:nil];
		destRect.origin.y -= [[UIApplication sharedApplication] statusBarFrame].size.height;
	}

	loadView = [[UIImageView alloc] initWithFrame:destRect];
	[loadView setContentMode:UIViewContentModeScaleAspectFill];
	loadView.image = [self loadAppropriateSplash];
	[controller.view addSubview:loadView];
	splashAttached = YES;
	return loadView;
}

- (void)loadSplash
{
	sharedApp = self;
	
	// attach our main view controller... IF we haven't already loaded the main window.
	if (!loaded) {
		[self attachSplash];
	}
	[window addSubview:controller.view];

    [window makeKeyAndVisible];
}

- (BOOL)isSplashVisible
{
	return splashAttached;
}

-(UIView*)splash
{
	return loadView;
}

- (void)hideSplash:(id)event
{
	// this is called when the first window is loaded
	// and should only be done once (obviously) - the
	// caller is responsible for setting up the animation
	// context before calling this and committing it afterwards
	if (loadView!=nil && splashAttached)
	{
		splashAttached = NO;
		loaded = YES;
		[loadView removeFromSuperview];
		RELEASE_TO_NIL(loadView);
	}
}

-(void)initController
{
	sharedApp = self;
	
	// attach our main view controller
	controller = [[TiRootViewController alloc] init];
	
	// Force view load
	controller.view.backgroundColor = [UIColor clearColor];
	
	if (![TiUtils isiPhoneOS3_2OrGreater]) {
		[self loadSplash];
	}
}

-(void)attachXHRBridgeIfRequired
{
#ifdef USE_TI_UIWEBVIEW
	if (xhrBridge==nil)
	{
		xhrBridge = [[XHRBridge alloc] initWithHost:self];
		[xhrBridge boot:self url:nil preload:nil];
	}
#endif
}

- (void)boot
{
	NSLog(@"[INFO] %@/%@ (%s.2732315...)",TI_APPLICATION_NAME,TI_APPLICATION_VERSION,TI_VERSION_STR);
	
	sessionId = [[TiUtils createUUID] retain];

#ifdef DEBUGGER_ENABLED
	[[TiDebugger sharedDebugger] start];
#endif
	
	kjsBridge = [[KrollBridge alloc] initWithHost:self];
	
	[kjsBridge boot:self url:nil preload:nil];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	if ([TiUtils isIOS4OrGreater])
	{
		[[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
	}
#endif
}

- (void)validator
{
	[[[NSClassFromString(TIV) alloc] init] autorelease];
}

- (void)booted:(id)bridge
{
	if ([bridge isKindOfClass:[KrollBridge class]])
	{
		NSLog(@"[DEBUG] application booted in %f ms", ([NSDate timeIntervalSinceReferenceDate]-started) * 1000);
		fflush(stderr);
		[self performSelectorOnMainThread:@selector(validator) withObject:nil waitUntilDone:YES];
	}
}

- (void)applicationDidFinishLaunching:(UIApplication *)application 
{
	NSSetUncaughtExceptionHandler(&MyUncaughtExceptionHandler);
	[self initController];
	[self boot];
}

- (void)generateNotification:(NSDictionary*)dict
{
	// Check and see if any keys from APS and the rest of the dictionary match; if they do, just
	// bump out the dictionary as-is
	remoteNotification = [[NSMutableDictionary alloc] initWithDictionary:dict];
	NSDictionary* aps = [dict objectForKey:@"aps"];
	for (id key in aps) 
	{
		if ([dict objectForKey:key] != nil) {
			NSLog(@"[WARN] Conflicting keys in push APS dictionary and notification dictionary `%@`, not copying to toplevel from APS", key);
			continue;
		}
		[remoteNotification setValue:[aps valueForKey:key] forKey:key];
	}
	NSLog(@"[WARN] Accessing APS keys from toplevel of notification is deprecated");
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions_
{
	started = [NSDate timeIntervalSinceReferenceDate];
	NSSetUncaughtExceptionHandler(&MyUncaughtExceptionHandler);

	// nibless window
	window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];

	[self initController];

	// get the current remote device UUID if we have one
	NSString *curKey = [[NSUserDefaults standardUserDefaults] stringForKey:@"APNSRemoteDeviceUUID"];
	if (curKey!=nil)
	{
		remoteDeviceUUID = [curKey copy];
	}

	launchOptions = [[NSMutableDictionary alloc] initWithDictionary:launchOptions_];
	
	NSURL *urlOptions = [launchOptions objectForKey:UIApplicationLaunchOptionsURLKey];
	NSString *sourceBundleId = [launchOptions objectForKey:UIApplicationLaunchOptionsSourceApplicationKey];
	NSDictionary *notification = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
	
	// reset these to be a little more common if we have them
	if (urlOptions!=nil)
	{
		[launchOptions setObject:[urlOptions absoluteString] forKey:@"url"];
		[launchOptions removeObjectForKey:UIApplicationLaunchOptionsURLKey];
	}
	if (sourceBundleId!=nil)
	{
		[launchOptions setObject:sourceBundleId forKey:@"source"];
		[launchOptions removeObjectForKey:UIApplicationLaunchOptionsSourceApplicationKey];
	}
	if (notification!=nil)
	{
		[self generateNotification:notification];
	}
	
	[self boot];
	
	return YES;
}

- (BOOL)application:(UIApplication *)application handleOpenURL:(NSURL *)url
{
	[launchOptions removeObjectForKey:UIApplicationLaunchOptionsURLKey];	
	[launchOptions setObject:[url absoluteString] forKey:@"url"];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
	NSNotificationCenter * theNotificationCenter = [NSNotificationCenter defaultCenter];

	//This will send out the 'close' message.
	[theNotificationCenter postNotificationName:kTiWillShutdownNotification object:self];
	
	NSCondition *condition = [[NSCondition alloc] init];

#ifdef USE_TI_UIWEBVIEW
	[xhrBridge shutdown:nil];
#endif	

	//These shutdowns return immediately, yes, but the main will still run the close that's in their queue.	
	[kjsBridge shutdown:condition];

	// THE CODE BELOW IS WRONG.
	// It only waits until ONE context has signialed that it has shut down; then we proceed along our merry way.
	// This might lead to problems like contexts not getting cleaned up properly due to premature app termination.
	// Plus, it blocks the main thread... meaning that we can have deadlocks if any context is currently executing
	// a request that requires operations on the main thread.
	[condition lock];
	[condition waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:SHUTDOWN_TIMEOUT_IN_SEC]];
	[condition unlock];

	//This will shut down the modules.
	[theNotificationCenter postNotificationName:kTiShutdownNotification object:self];
	
	RELEASE_TO_NIL(condition);
	RELEASE_TO_NIL(kjsBridge);
#ifdef USE_TI_UIWEBVIEW 
	RELEASE_TO_NIL(xhrBridge);
#endif	
	RELEASE_TO_NIL(remoteNotification);
	RELEASE_TO_NIL(sessionId);
}

- (void)applicationDidReceiveMemoryWarning:(UIApplication *)application
{
	[Webcolor flushCache];
	// don't worry about KrollBridge since he's already listening
#ifdef USE_TI_UIWEBVIEW
	[xhrBridge gc];
#endif 
}

-(void)applicationWillResignActive:(UIApplication *)application
{
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiSuspendNotification object:self];
	
	// suspend any image loading
	[[ImageLoader sharedLoader] suspend];
	
	[kjsBridge gc];
	
#ifdef USE_TI_UIWEBVIEW
	[xhrBridge gc];
#endif 
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
	// NOTE: Have to fire a separate but non-'resume' event here because there is SOME information
	// (like new URL) that is not passed through as part of the normal foregrounding process.
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiResumedNotification object:self];
	
	// resume any image loading
	[[ImageLoader sharedLoader] resume];
}

-(void)applicationDidEnterBackground:(UIApplication *)application
{
	[TiUtils queueAnalytics:@"ti.background" name:@"ti.background" data:nil];

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	
	if (backgroundServices==nil)
	{
		return;
	}
	
	UIApplication* app = [UIApplication sharedApplication];
	TiApp *tiapp = self;
	bgTask = [app beginBackgroundTaskWithExpirationHandler:^{
        // Synchronize the cleanup call on the main thread in case
        // the task actually finishes at around the same time.
        dispatch_async(dispatch_get_main_queue(), ^{
            if (bgTask != UIBackgroundTaskInvalid)
            {
                [app endBackgroundTask:bgTask];
                bgTask = UIBackgroundTaskInvalid;
            }
        });
    }];
	// Start the long-running task and return immediately.
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
		
        // Do the work associated with the task.
		[tiapp beginBackgrounding];
    });
#endif	
	
}

-(void)applicationWillEnterForeground:(UIApplication *)application
{
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiResumeNotification object:self];
	
	[TiUtils queueAnalytics:@"ti.foreground" name:@"ti.foreground" data:nil];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	
	if (backgroundServices==nil)
	{
		return;
	}
	
	[self endBackgrounding];
	
#endif

}

-(id)remoteNotification
{
	return remoteNotification;
}

#pragma mark Push Notification Delegates

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
{
	// NOTE: this is called when the app is *running* after receiving a push notification
	// otherwise, if the app is started from a push notification, this method will not be 
	// called
	RELEASE_TO_NIL(remoteNotification);
	[self generateNotification:userInfo];
	
	if (remoteNotificationDelegate!=nil)
	{
		[remoteNotificationDelegate performSelector:@selector(application:didReceiveRemoteNotification:) withObject:application withObject:remoteNotification];
	}
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{ 
	NSString *token = [[[[deviceToken description] stringByReplacingOccurrencesOfString:@"<"withString:@""] 
						stringByReplacingOccurrencesOfString:@">" withString:@""] 
					   stringByReplacingOccurrencesOfString: @" " withString: @""];
	
	RELEASE_TO_NIL(remoteDeviceUUID);
	remoteDeviceUUID = [token copy];
	
	NSString *curKey = [[NSUserDefaults standardUserDefaults] stringForKey:@"APNSRemoteDeviceUUID"];
	if (curKey==nil || ![curKey isEqualToString:remoteDeviceUUID])
	{
		// this is the first time being registered, we need to indicate to our backend that we have a 
		// new registered device to enable this device to receive notifications from the cloud
		[[NSUserDefaults standardUserDefaults] setObject:remoteDeviceUUID forKey:@"APNSRemoteDeviceUUID"];
		NSDictionary *userInfo = [NSDictionary dictionaryWithObject:remoteDeviceUUID forKey:@"deviceid"];
		[[NSNotificationCenter defaultCenter] postNotificationName:kTiRemoteDeviceUUIDNotification object:self userInfo:userInfo];
		NSLog(@"[DEBUG] registered new device ready for remote push notifications: %@",remoteDeviceUUID);
	}
	
	if (remoteNotificationDelegate!=nil)
	{
		[remoteNotificationDelegate performSelector:@selector(application:didRegisterForRemoteNotificationsWithDeviceToken:) withObject:application withObject:deviceToken];
	}
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
	if (remoteNotificationDelegate!=nil)
	{
		[remoteNotificationDelegate performSelector:@selector(application:didFailToRegisterForRemoteNotificationsWithError:) withObject:application withObject:error];
	}
}

//TODO: this should be compiled out in production mode
-(void)showModalError:(NSString*)message
{
	if ([TI_APPLICATION_DEPLOYTYPE isEqualToString:@"production"])
	{
		NSLog(@"[ERROR] application received error: %@",message);
		return;
	}
	ENSURE_UI_THREAD(showModalError,message);
	TiErrorController *error = [[[TiErrorController alloc] initWithError:message] autorelease];
	[controller presentModalViewController:error animated:YES];
}

-(void)attachModal:(UIViewController*)modalController toController:(UIViewController*)presentingController animated:(BOOL)animated
{
	UIViewController * currentModalController = [presentingController modalViewController];

	if (currentModalController == modalController)
	{
		NSLog(@"[WARN] Trying to present a modal window that already is a modal window.");
		return;
	}
	if (currentModalController == nil)
	{
		[presentingController presentModalViewController:modalController animated:animated];
		return;
	}
	[self attachModal:modalController toController:currentModalController animated:animated];
}

-(void)showModalController:(UIViewController*)modalController animated:(BOOL)animated
{
//In the rare event that the iPad application started in landscape, has not been rotated,
//And is presenting a modal for the first time, 
		handledModal = YES;

	if(!handledModal)
	{
		handledModal = YES;
		UIView * rootView = [controller view];
		UIView * windowView = [rootView superview];
		[rootView removeFromSuperview];
		[windowView addSubview:rootView];
	}


	UINavigationController *navController = nil; //[(TiRootViewController *)controller focusedViewController];
	if (navController==nil)
	{
		navController = [controller navigationController];
	}
	// if we have a nav controller, use him, otherwise use our root controller
	if (navController!=nil)
	{
		[controller windowFocused:modalController];
		[self attachModal:modalController toController:navController animated:animated];
	}
	else
	{
		[self attachModal:modalController toController:controller animated:animated];
	}
}

-(void)hideModalController:(UIViewController*)modalController animated:(BOOL)animated
{
	UIViewController *navController = [modalController parentViewController];
	if (navController==nil)
	{
//		navController = [controller currentNavController];
	}
	[controller windowClosed:modalController];
	if (navController!=nil)
	{
		[navController dismissModalViewControllerAnimated:animated];
	}
	else 
	{
		[controller dismissModalViewControllerAnimated:animated];
	}
}


- (void)dealloc 
{
	RELEASE_TO_NIL(kjsBridge);
#ifdef USE_TI_UIWEBVIEW
	RELEASE_TO_NIL(xhrBridge);
#endif	
	RELEASE_TO_NIL(loadView);
	RELEASE_TO_NIL(window);
	RELEASE_TO_NIL(launchOptions);
	RELEASE_TO_NIL(controller);
	RELEASE_TO_NIL(userAgent);
	RELEASE_TO_NIL(remoteDeviceUUID);
	RELEASE_TO_NIL(remoteNotification);
#ifdef DEBUGGER_ENABLED
	[[TiDebugger sharedDebugger] stop];
#endif
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	RELEASE_TO_NIL(backgroundServices);
	RELEASE_TO_NIL(localNotification);
#endif	
	[super dealloc];
}

- (NSString*)userAgent
{
	if (userAgent==nil)
	{
		UIDevice *currentDevice = [UIDevice currentDevice];
		NSString *currentLocaleIdentifier = [[NSLocale currentLocale] localeIdentifier];
		NSString *currentDeviceInfo = [NSString stringWithFormat:@"%@/%@; %@; %@;",[currentDevice model],[currentDevice systemVersion],[currentDevice systemName],currentLocaleIdentifier];
		NSString *kOsmmExampleUserAgentPrefix = [NSString stringWithFormat:@"%s%s%s %s%s","Appc","eler","ator","Tita","nium"];
		userAgent = [[NSString stringWithFormat:@"%@/%s (%@)",kOsmmExampleUserAgentPrefix,TI_VERSION_STR,currentDeviceInfo] retain];
	}
	return userAgent;
}

-(NSString*)remoteDeviceUUID
{
	return remoteDeviceUUID;
}

-(NSString*)sessionId
{
	return sessionId;
}

-(KrollBridge*)krollBridge
{
	return kjsBridge;
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0

#pragma mark Backgrounding

-(void)beginBackgrounding
{
	runningServices = [[NSMutableArray alloc] initWithCapacity:[backgroundServices count]];
	
	for (TiProxy *proxy in backgroundServices)
	{
		[runningServices addObject:proxy];
		[proxy performSelector:@selector(beginBackground)];
	}
}

-(void)endBackgrounding
{
	for (TiProxy *proxy in backgroundServices)
	{
		[proxy performSelector:@selector(endBackground)];
		[runningServices removeObject:proxy];
	}
	
	RELEASE_TO_NIL(runningServices);
}

- (void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification
{
	RELEASE_TO_NIL(localNotification);
	localNotification = [notification retain];
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiLocalNotification object:notification userInfo:nil];
}

-(UILocalNotification*)localNotification
{
	return localNotification;
}

-(void)registerBackgroundService:(TiProxy*)proxy
{
	if (backgroundServices==nil)
	{
		backgroundServices = [[NSMutableArray alloc] initWithCapacity:1];
	}
	[backgroundServices addObject:proxy];
}

-(void)unregisterBackgroundService:(TiProxy*)proxy
{
	[backgroundServices removeObject:proxy];
	if ([backgroundServices count]==0)
	{
		RELEASE_TO_NIL(backgroundServices);
	}
}

-(void)stopBackgroundService:(TiProxy *)proxy
{
	[runningServices removeObject:proxy];
	[backgroundServices removeObject:proxy];
	
	if ([runningServices count] == 0)
	{
		RELEASE_TO_NIL(runningServices);
		
		// Synchronize the cleanup call on the main thread in case
		// the expiration handler is fired at the same time.
		dispatch_async(dispatch_get_main_queue(), ^{
			if (bgTask != UIBackgroundTaskInvalid)
			{
				[[UIApplication sharedApplication] endBackgroundTask:bgTask];
				bgTask = UIBackgroundTaskInvalid;
			}
		});
	}
}

#endif

@end
