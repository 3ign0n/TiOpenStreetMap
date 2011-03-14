/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by OsmmExample, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_FACEBOOK
#import "FacebookModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiBlob.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "FBConnect/Facebook.h"
#import "TiFacebookRequest.h"
#import "TiFacebookDialogRequest.h"
#import "TiFacebookLoginButtonProxy.h"

/**
 * Good reference for access_tokens and what all this crap means
 * http://benbiddington.wordpress.com/2010/04/23/facebook-graph-api-getting-access-tokens/
 */

@implementation FacebookModule

@synthesize facebook;

#pragma mark Sessions

-(void)_save 
{
	VerboseLog(@"[DEBUG] facebook _save");
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	if ((uid != (NSString *) [NSNull null]) && (uid.length > 0)) {
		[defaults setObject:uid forKey:@"FBUserId"];
	} else {
		[defaults removeObjectForKey:@"FBUserId"];
	}
	
	NSString *access_token = facebook.accessToken;
	if ((access_token != (NSString *) [NSNull null]) && (access_token.length > 0)) {
		[defaults setObject:access_token forKey:@"FBAccessToken"];
	} else {
		[defaults removeObjectForKey:@"FBAccessToken"];
	}
	
	NSDate *expirationDate = facebook.expirationDate;  
	if (expirationDate) {
		[defaults setObject:expirationDate forKey:@"FBSessionExpires"];
	} else {
		[defaults removeObjectForKey:@"FBSessionExpires"];
	}
	
	if (appid) {
		[defaults setObject:appid forKey:@"FBAppId"];
	}else {
		[defaults removeObjectForKey:@"FBAppId"];
	}
	
	[defaults synchronize];
}

-(void)_unsave 
{
	VerboseLog(@"[DEBUG] facebook _unsave");
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	[defaults removeObjectForKey:@"FBUserId"];
	[defaults removeObjectForKey:@"FBAccessToken"];
	[defaults removeObjectForKey:@"FBSessionExpires"];
	[defaults removeObjectForKey:@"FBAppId"];
	[defaults synchronize]; 
}

-(id)_restore 
{
	VerboseLog(@"[DEBUG] facebook _restore");
	RELEASE_TO_NIL(uid);
	RELEASE_TO_NIL(facebook);
	RELEASE_TO_NIL(appid);
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	NSString *uid_ = [defaults objectForKey:@"FBUserId"];
	VerboseLog(@"[DEBUG] facebook _restore, uid = %@",uid_);
	facebook = [[Facebook alloc] init];
	if (uid_) 
	{
		NSDate* expirationDate = [defaults objectForKey:@"FBSessionExpires"];
		VerboseLog(@"[DEBUG] facebook _restore, expirationDate = %@",expirationDate);
		if (!expirationDate || [expirationDate timeIntervalSinceNow] > 0) {
			uid = [uid_ copy];
			facebook.accessToken = [defaults stringForKey:@"FBAccessToken"];
			facebook.expirationDate = expirationDate;
			loggedIn = YES;
			appid = [[defaults stringForKey:@"FBAppId"] copy];
			[self performSelector:@selector(fbDidLogin)];
		}
	}
	return facebook;
}

#pragma mark Lifecycle

-(void)dealloc
{
	RELEASE_TO_NIL(facebook);
	RELEASE_TO_NIL(stateListeners);
	RELEASE_TO_NIL(appid);
	RELEASE_TO_NIL(permissions);
	RELEASE_TO_NIL(uid);
	[super dealloc];
}

-(BOOL)handleRelaunch
{
	NSDictionary *launchOptions = [[TiApp app] launchOptions];
	if (launchOptions!=nil)
	{
		NSString *urlString = [launchOptions objectForKey:@"url"];
		if (urlString!=nil && [urlString hasPrefix:@"fb"])
		{
			// if we're resuming under the same URL, we need to ignore
			if (url!=nil && [urlString isEqualToString:url])
			{
				return YES;
			}
			RELEASE_TO_NIL(url);
			url = [urlString copy];
			[facebook handleOpenURL:[NSURL URLWithString:urlString]];
			return YES;
		}
	}
	return NO;
}

-(void)resumed:(id)note
{
	VerboseLog(@"[DEBUG] facebook resumed");
	
	[self handleRelaunch];
}

-(void)startup
{
	VerboseLog(@"[DEBUG] facebook startup");
	
	[super startup];
	[self _restore];
	[self handleRelaunch];
}

-(void)shutdown:(id)sender
{
	VerboseLog(@"[DEBUG] facebook shutdown");

	[[NSNotificationCenter defaultCenter] removeObserver:self];
	[super shutdown:sender];
}

-(BOOL)isLoggedIn
{
	return (facebook!=nil) && ([facebook isSessionValid]) && loggedIn;
}

#pragma mark Internal

-(NSString*)convertBlobParams:(NSMutableDictionary*)params
{
	NSString* httpMethod = nil;
	for (NSString *key in [params allKeys])
	{
		id param = [params objectForKey:key];
		
		// convert to blob
		if ([param isKindOfClass:[TiFile class]])
		{
			TiFile *file = (TiFile*)param;
			if ([file size] > 0)
			{
				param = [file toBlob:nil];
			}
			else 
			{
				// empty file?
				param = [[[TiBlob alloc] initWithData:[NSData data] mimetype:@"text/plain"] autorelease];
			}
		}
		
		// this is an attachment, we need to convert to POST and switch to blob
		if ([param isKindOfClass:[TiBlob class]])
		{
			httpMethod = @"POST";
			TiBlob *blob = (TiBlob*)param;
			VerboseLog(@"[DEBUG] detected blob with mime: %@",[blob mimeType]);
			if ([[blob mimeType] hasPrefix:@"image/"])
			{
				UIImage *image = [blob image];
				[params setObject:image forKey:key];
			}
			else
			{
				NSData *data = [blob data];
				[params setObject:data forKey:key];
			}
		}
	}
	return httpMethod;
}

#pragma mark Public APIs

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.uid);
 * 
 */
-(id)uid
{
	return uid;
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * if (facebook.loggedIn) {
 * }
 * 
 */
-(id)loggedIn
{
	return NUMBOOL([self isLoggedIn]);
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.appid = '1234';
 * alert(facebook.appid);
 * 
 */
-(id)appid
{
	return appid;
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.permissions = ['publish_stream'];
 * alert(facebook.permissions);
 * 
 */
-(id)permissions
{
	return permissions;
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.accessToken);
 * 
 */
-(id)accessToken
{
	return [facebook accessToken];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.expirationDate);
 * 
 */
-(id)expirationDate
{
	return [facebook expirationDate];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.appid = '1234';
 * alert(facebook.appid);
 * 
 */
-(void)setAppid:(id)arg
{
	RELEASE_TO_NIL(appid);
	appid = [arg copy];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.permissions = ['publish_stream'];
 * alert(facebook.permissions);
 * 
 */
-(void)setPermissions:(id)arg
{
	RELEASE_TO_NIL(permissions);
	permissions = [arg retain];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 *
 * facebook.addEventListener('login',function(e) {
 *    if (e.success) {
 *		alert('login from uid: '+e.uid+', name: '+e.data.name);
 *    }
 *    else if (e.cancelled) {
 *      // user cancelled logout
 *    }
 *    else {
 *      alert(e.error);
 *    }
 * });
 * 
 * facebook.addEventListener('logout',function(e) {
 *    alert('logged out');
 * });
 * 
 * facebook.appid = 'my_appid';
 * facebook.permissions = ['publish_stream'];
 * facebook.authorize();
 *
 */
-(void)authorize:(id)args
{
	ENSURE_UI_THREAD(authorize, args);
	
	VerboseLog(@"[DEBUG] facebook authorize");

	if ([self isLoggedIn])
	{
		// if already authorized, this should do nothing
		return;
	}
	
	if (appid==nil)
	{
		[self throwException:@"missing appid" subreason:nil location:CODELOCATION];
	}
	
	// forget in case it fails
	[self _unsave];
	
	NSArray *permissions_ = permissions == nil ? [NSArray array] : permissions;
	[facebook authorize:appid permissions:permissions_ delegate:self];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.logout();
 *
 */
-(void)logout:(id)args
{
	VerboseLog(@"[DEBUG] facebook logout");
	if ([self isLoggedIn])
	{
		[facebook logout:self];
	}
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 *
 * facebook.requestWithGraphPath('me',{}, 'post', function(e) {
 *    if (e.success) {
 *      alert('success! welcome userid: '+e.id);
 *    }
 *    else {
 *      alert(e.error);
 *    }
 * });
 *
 */
-(void)requestWithGraphPath:(id)args
{
	VerboseLog(@"[DEBUG] facebook requestWithGraphPath");

	ENSURE_ARG_COUNT(args,4);
	ENSURE_UI_THREAD_1_ARG(args);
	
	NSString* path = [args objectAtIndex:0];
	NSMutableDictionary* params = [args objectAtIndex:1];
	NSString* httpMethod = [args objectAtIndex:2];
	KrollCallback* callback = [args objectAtIndex:3];

	[self convertBlobParams:params];
	
	TiFacebookRequest* delegate = [[[TiFacebookRequest alloc] initWithPath:path callback:callback module:self graph:YES] autorelease];
	[facebook requestWithGraphPath:path andParams:params andHttpMethod:httpMethod andDelegate:delegate];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 *
 * facebook.request('photos.upload',{picture:blob},function(e) {
 *    if (e.success) {
 *      alert('success!');
 *    }
 *    else {
 *      alert(e.error);
 *    }
 * });
 *
 */
-(void)request:(id)args
{
	VerboseLog(@"[DEBUG] facebook request");

	ENSURE_ARG_COUNT(args,3);
	ENSURE_UI_THREAD_1_ARG(args);
	
	NSString* method = [args objectAtIndex:0];
	NSMutableDictionary* params = [args objectAtIndex:1];
	KrollCallback* callback = [args objectAtIndex:2];
	
	NSString *httpMethod = @"GET";
	NSString* changedHttpMethod = [self convertBlobParams:params];
	if (changedHttpMethod != nil) {
		httpMethod = changedHttpMethod;
	}
	
	TiFacebookRequest* delegate = [[[TiFacebookRequest alloc] initWithPath:method callback:callback module:self graph:NO] autorelease];
	[facebook requestWithMethodName:method andParams:params andHttpMethod:httpMethod andDelegate:delegate];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.dialog('stream.publish',{'api_key':'1234'},function(e) {
 *    if (e.success) {
 *       Ti.API.info('result was = '+JSON.stringify(e.result));
 *    }
 * });
 * 
 */
-(void)dialog:(id)args
{
	ENSURE_ARG_COUNT(args,3);
	ENSURE_UI_THREAD_1_ARG(args);

	VerboseLog(@"[DEBUG] facebook dialog");

	NSString* action = [args objectAtIndex:0];
	NSMutableDictionary* params = [args objectAtIndex:1];
	KrollCallback* callback = [args objectAtIndex:2];
	
	[self convertBlobParams:params];
	
	TiFacebookDialogRequest *delegate = [[[TiFacebookDialogRequest alloc] initWithCallback:callback module:self] autorelease];
	[facebook dialog:action andParams:params andDelegate:delegate];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * var button = facebook.createLoginButton({bottom:10});
 * window.add(button);
 * 
 */
-(id)createLoginButton:(id)args
{
	return [[[TiFacebookLoginButtonProxy alloc] _initWithPageContext:[self pageContext] args:args module:self] autorelease];
}

#pragma mark Listener work

-(void)fireLoginChange
{
	if (stateListeners!=nil)
	{
		for (id<TiFacebookStateListener> listener in [NSArray arrayWithArray:stateListeners])
		{
			if (loggedIn)
			{
				[listener login];
			}
			else 
			{
				[listener logout];
			}
		}
	}
}


#pragma mark Delegate

/**
 * Called when the user successfully logged in.
 */
- (void)fbDidLogin
{
	VerboseLog(@"[DEBUG] facebook fbDidLogin");

	[facebook requestWithGraphPath:@"me" andDelegate:self];
}

/**
 * Called when the user dismissed the dialog without logging in.
 */
- (void)fbDidNotLogin:(BOOL)cancelled
{
	VerboseLog(@"[DEBUG] facebook fbDidNotLogin: cancelled=%d",cancelled);
	loggedIn = NO;
	[self fireLoginChange];
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(cancelled),@"cancelled",NUMBOOL(NO),@"success",nil];
	[self fireEvent:@"login" withObject:event];
}

/**
 * Called when the user logged out.
 */
- (void)fbDidLogout
{
	VerboseLog(@"[DEBUG] facebook fbDidLogout");
	
	loggedIn = NO;
	[self _unsave];
	[self fireLoginChange];
	[self fireEvent:@"logout"];
}

//----------- these are only used when the login is successful to grab UID

/**
 * FBRequestDelegate
 */
- (void)request:(FBRequest2*)request didLoad:(id)result
{
	VerboseLog(@"[DEBUG] facebook didLoad");
	
	RELEASE_TO_NIL(uid);
	uid = [[result objectForKey:@"id"] copy]; 
	[self _save];
	loggedIn = YES;
	[self fireLoginChange];
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(NO),@"cancelled",NUMBOOL(YES),@"success",uid,@"uid",result,@"data",nil];
	[self fireEvent:@"login" withObject:event];
}


- (void)request:(FBRequest2*)request didFailWithError:(NSError*)error 
{
	VerboseLog(@"[DEBUG] facebook didFailWithError: %@",error);
	
	RELEASE_TO_NIL(uid);
	loggedIn = NO;
	[self fireLoginChange];
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(NO),@"cancelled",NUMBOOL(NO),@"success",error,@"error",nil];
	[self fireEvent:@"login" withObject:event];
}

#pragma mark Listeners

-(void)addListener:(id<TiFacebookStateListener>)listener
{
	if (stateListeners==nil)
	{
		stateListeners = [[NSMutableArray alloc]init];
	}
	[stateListeners addObject:listener];
}

-(void)removeListener:(id<TiFacebookStateListener>)listener
{
	if (stateListeners!=nil)
	{
		[stateListeners removeObject:listener];
		if ([stateListeners count]==0)
		{
			RELEASE_TO_NIL(stateListeners);
		}
	}
}

@end
#endif