/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_MEDIA


#import <AudioToolbox/AudioToolbox.h>
#import <QuartzCore/QuartzCore.h>

#import "TiMediaVideoPlayerProxy.h"
#import "TiMediaVideoPlayer.h"
#import "TiUtils.h"
#import "Webcolor.h"
#import "TiFile.h"
#import "TiViewProxy.h"
#import "TiBlob.h"
#import "TiMediaAudioSession.h"
#import "TiApp.h"

/** 
 * Design Notes:
 *
 * Normally we'd use a ViewProxy/View pattern here ...but...
 *
 * Before 3.2, the player was always fullscreen and we were just a Proxy
 *
 * In 3.2, the player went to a different API with iPad where you could now
 * embedded the video in any view
 *
 * So, this class reflects the ability to work with both the older release
 * for older devices/apps and the newer style
 *
 */

#define RETURN_FROM_LOAD_PROPERTIES(property,default) \
{\
	id temp = [loadProperties valueForKey:property];\
	[returnCache setValue:(temp ? temp : default) forKey:@"" #property];\
	return temp ? temp : default; \
}

#define ONLY_IN_3_2_OR_GREATER(method) \
if (![TiUtils isiPhoneOS3_2OrGreater]) {\
	[self throwException:@"" #method "() is not available in pre-3.2 OS"\
		subreason:nil\
		location:CODELOCATION]; \
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
@interface TiMediaVideoPlayerProxy ()
@property(nonatomic,readwrite,copy)	NSNumber*	movieControlStyle;
@end
#endif

NSArray* moviePlayerKeys = nil;

@implementation TiMediaVideoPlayerProxy

#pragma mark Internal

-(NSArray*)keySequence
{
	if (moviePlayerKeys == nil) {
		moviePlayerKeys = [[NSArray alloc] initWithObjects:@"url",@"contentURL",nil];
	}
	return moviePlayerKeys;
}

-(void)_initWithProperties:(NSDictionary *)properties
{	
	loadProperties = [[NSMutableDictionary alloc] init];
	returnCache = [[NSMutableDictionary alloc] init];
	playerLock = [[NSRecursiveLock alloc] init];
	[super _initWithProperties:properties];
}

-(void)_destroy
{
	if (playing) {
		[movie stop];
	}
	
	WARN_IF_BACKGROUND_THREAD;	//NSNotificationCenter is not threadsafe!
	NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
	[nc removeObserver:self];

	RELEASE_TO_NIL(thumbnailCallback);
	RELEASE_TO_NIL(tempFile);
	RELEASE_TO_NIL(movie);
	RELEASE_TO_NIL(url);
	RELEASE_TO_NIL(loadProperties);
	RELEASE_TO_NIL(returnCache);
	RELEASE_TO_NIL(playerLock);
	[super _destroy];
}

-(void)configureNotifications
{
	WARN_IF_BACKGROUND_THREAD;	//NSNotificationCenter is not threadsafe!
	NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
	
	[nc addObserver:self selector:@selector(handlePlayerNotification:) 
			   name:MPMoviePlayerPlaybackDidFinishNotification
			 object:movie];
	
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		[nc addObserver:self selector:@selector(handleThumbnailImageRequestFinishNotification:) 
				   name:MPMoviePlayerThumbnailImageRequestDidFinishNotification
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleFullscreenEnterNotification:) 
				   name:MPMoviePlayerWillEnterFullscreenNotification
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleFullscreenExitNotification:)
				   name:MPMoviePlayerWillExitFullscreenNotification
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleSourceTypeNotification:) 
				   name:MPMovieSourceTypeAvailableNotification
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleDurationAvailableNotification:) 
				   name:MPMovieDurationAvailableNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleMediaTypesNotification:) 
				   name:MPMovieMediaTypesAvailableNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleNaturalSizeAvailableNotification:)
				   name:MPMovieNaturalSizeAvailableNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleLoadStateChangeNotification:)
				   name:MPMoviePlayerLoadStateDidChangeNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleNowPlayingNotification:)
				   name:MPMoviePlayerNowPlayingMovieDidChangeNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handlePlaybackStateChangeNotification:)
				   name:MPMoviePlayerPlaybackStateDidChangeNotification 
				 object:movie];
		
		[nc addObserver:self selector:@selector(handleRotationNotification:)
				   name:UIApplicationDidChangeStatusBarOrientationNotification
				 object:nil];
		
		//FIXME: add to replace preload for 3.2
		//MPMediaPlaybackIsPreparedToPlayDidChangeNotification
	}
	else {
#endif
		[nc addObserver:self selector:@selector(handleKeyWindowChanged:) 
				   name:UIWindowDidBecomeKeyNotification
				 object:nil];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	}
#endif	
}

// Used to avoid duplicate code in Brightcove module; makes things easier to maintain.
-(void)configurePlayer
{
	[self configureNotifications];
	[self setValuesForKeysWithDictionary:loadProperties];
	// we need this code below since the player can be realized before loading
	// properties in certain cases and when we go to create it again after setting
	// url we will need to set the new controller to the already created view
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		TiMediaVideoPlayer *vp = (TiMediaVideoPlayer*)[self view];
		[vp setMovie:movie];
	}
#endif
}

-(MPMoviePlayerController*)player
{
	[playerLock lock];
	if (movie == nil)
	{
		if (url==nil)
		{
			[playerLock unlock];
			// this is OK - we just need to delay creation of the 
			// player until after the url is set 
			return nil;
		}
		movie = [[MPMoviePlayerController alloc] initWithContentURL:url];
		[self configurePlayer];
	}
	[playerLock unlock];
	return movie;
}

-(TiUIView*)newView
{
	if (reallyAttached) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
		if ([TiUtils isiPhoneOS3_2OrGreater]) {
			// override since we're constructing ourselfs
			TiUIView *v = [[TiMediaVideoPlayer alloc] initWithPlayer:[self player] proxy:self];
			return v;
		}
		else {
#endif
			return [super newView];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
		}
#endif
	}
	return nil;
}

// TODO: Placing this in TiViewProxy would be better, but right now it screws up tableview.
// So... move it there and fix tableview, when we have the time.
-(void)relayout
{
	if (!CGRectEqualToRect(sandboxBounds, CGRectZero)) {
		[super relayout];
	}
}

-(void)viewWillAttach
{
	reallyAttached = YES;
}

-(void)viewDidDetach
{
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		if (playing) {
			[movie stop];
		}
		RELEASE_TO_NIL(movie);
	}
	reallyAttached = NO;
}

#pragma mark Public APIs

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
-(void)setBackgroundView:(id)proxy
{
	ONLY_IN_3_2_OR_GREATER(setBackgroundView)
	
	if (movie != nil) {
		UIView *background = [[self player] backgroundView];
		for (UIView *view_ in [background subviews])
		{
			[view_ removeFromSuperview];
		}
		[background addSubview:[proxy view]];
	}
	else {
		[loadProperties setValue:proxy forKey:@"backgroundView"];
	}
}
#endif

-(void)setInitialPlaybackTime:(id)time
{
	if (movie != nil) {
		CGFloat ourTime = [TiUtils doubleValue:time];
		if (ourTime > 0) {
			ENSURE_UI_THREAD_1_ARG(time);
			[[self player] setInitialPlaybackTime:ourTime];
		}
	}
	else {
		[loadProperties setValue:time forKey:@"initialPlaybackTime"];
	}
}

-(NSNumber*)initialPlaybackTime
{
	if (movie != nil) {
		return NUMDOUBLE([[self player] initialPlaybackTime]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"initialPlaybackTime", NUMINT(0));
	}
}

-(NSNumber*)playing
{
	return NUMBOOL(playing);
}

-(void)updateScalingMode:(id)value
{
	[[self player] setScalingMode:[TiUtils intValue:value def:MPMovieScalingModeNone]];
}

-(void)setScalingMode:(NSNumber *)value
{
	if (movie != nil) {
		[self performSelectorOnMainThread:@selector(updateScalingMode:) withObject:value waitUntilDone:NO];
	}
	else {
		[loadProperties setValue:value forKey:@"scalingMode"];
	}
}

-(NSNumber*)scalingMode
{
	if (movie != nil) {
		return NUMINT([[self player] scalingMode]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"scalingMode", NUMINT(MPMovieScalingModeNone));
	}
}

// < 3.2 functions for controls
-(void)updateControlMode:(id)value
{
#if __IPHONE_OS_VERSION_MAX_ALLOWED < __IPHONE_3_2
	[[self player] setMovieControlMode:[TiUtils intValue:value def:MPMovieControlModeDefault]];
#endif
}

-(void)setMovieControlMode:(NSNumber *)value
{
	if (![TiUtils isiPhoneOS3_2OrGreater]) {
		if (movie != nil) {
			[self performSelectorOnMainThread:@selector(updateControlMode:) withObject:value waitUntilDone:NO];
		}
		else {
			[loadProperties setValue:value forKey:@"movieControlMode"];
		}
	}
	else {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
		// Turn this into a synonym for setMovieControlStyle
		[self setMovieControlStyle:value];
#endif
	}
}

-(NSNumber*)movieControlMode
{
	if (![TiUtils isiPhoneOS3_2OrGreater]) {
		if (movie != nil) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED < __IPHONE_3_2
			return NUMINT([[self player] movieControlMode]);
#endif
		}
		else {
			RETURN_FROM_LOAD_PROPERTIES(@"movieControlMode",NUMINT(MPMovieControlModeDefault));
		}
	}
	else {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
		// Turn into a synonym for movieControlStyle
		return [self movieControlStyle];
#endif
	}
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
// >=3.2 functions for controls
-(void)updateControlStyle:(id)value
{
	[[self player] setControlStyle:[TiUtils intValue:value def:MPMovieControlStyleDefault]];
}

-(void)setMovieControlStyle:(NSNumber *)value
{
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		if (movie != nil) {
			[self performSelectorOnMainThread:@selector(updateControlStyle:) withObject:value waitUntilDone:NO];
		}
		else {
			[loadProperties setValue:value forKey:@"movieControlStyle"];
		}
	}
	else {
		// Turn this into a synonym for setMovieControlMode
		[self setMovieControlMode:value];
	}
}

-(NSNumber*)movieControlStyle
{
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		return NUMINT([[self player] controlStyle]);
	}
	else {
		// Turn this into a synonym for movieControlMode
		return [self movieControlMode];
	}
}
#endif 

-(void)setMedia:(id)media_
{
	if ([media_ isKindOfClass:[TiFile class]])
	{
		[self setUrl:[media_ path]];
	}
	else if ([media_ isKindOfClass:[TiBlob class]])
	{
		TiBlob *blob = (TiBlob*)media_;
		if ([blob type] == TiBlobTypeFile)
		{
			[self setUrl:[blob path]];
		}
		else if ([blob type] == TiBlobTypeData)
		{
			RELEASE_TO_NIL(tempFile);
			tempFile = [[TiUtils createTempFile:@"mov"] retain];
			[blob writeTo:[tempFile path] error:nil];
			[self setUrl:[NSURL fileURLWithPath:[tempFile path]]];
		}
		else
		{
			NSLog(@"[ERROR] unsupported blob for video player. %@",media_);
		}
	}
	else 
	{
		[self setUrl:media_];
	}
}

// Used to avoid duplicate code in Brightcove module; makes things easier to maintain.
-(void)restart
{
	BOOL restart = playing;
	if (playing)
	{
		[movie stop];
		playing = NO;
	}
	RELEASE_TO_NIL_AUTORELEASE(movie);
	
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		TiMediaVideoPlayer *video = (TiMediaVideoPlayer*)[self view];
		[video setMovie:[self player]];
		[video frameSizeChanged:[video frame] bounds:[video bounds]];
	}
#endif
	
	if (restart)
	{ 
		[self performSelectorOnMainThread:@selector(play:) withObject:nil waitUntilDone:NO];
	}
}

-(void)setUrl:(id)url_
{
	ENSURE_UI_THREAD(setUrl,url_);
	RELEASE_TO_NIL(url);
	url = [[TiUtils toURL:url_ proxy:self] retain];
	
	if (movie!=nil)
	{
		[self restart];
	}
	else {
		[self player];
	}

}

-(void)setContentURL:(id)newUrl
{
	[self setUrl:newUrl];
}

-(id)url
{
	return url;
}

-(id)contentURL
{
	return url;
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2

-(NSNumber*)autoplay
{
	ONLY_IN_3_2_OR_GREATER(autoplay)
	
	if (movie != nil) {
		return NUMBOOL([[self player] shouldAutoplay]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"autoplay",NUMBOOL(YES));
	}
}

-(void)setAutoplay:(id)value
{
	ONLY_IN_3_2_OR_GREATER(setAutoplay)
	
	if (movie != nil) {
		[[self player] setShouldAutoplay:[TiUtils boolValue:value]];
	}
	else {
		[loadProperties setValue:value forKey:@"autoplay"];
	}
}

-(NSNumber*)useApplicationAudioSession
{
	ONLY_IN_3_2_OR_GREATER(useApplicationAudioSession)
	
	if (movie != nil) {
		return NUMBOOL([[self player] useApplicationAudioSession]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"useApplicationAudioSession",NUMBOOL(YES));
	}
}

-(void)setUseApplicationAudioSession:(id)value
{
	ONLY_IN_3_2_OR_GREATER(setUseApplicationAudioSession)
	
	if (movie != nil) {
		[[self player] setUseApplicationAudioSession:[TiUtils boolValue:value]];
	}
	else {
		[loadProperties setValue:value forKey:@"useApplicationAudioSession"];
	}
}

-(void)cancelAllThumbnailImageRequests:(id)value
{
	ONLY_IN_3_2_OR_GREATER(cancelAllThumbnailImageRequests)
	
	[[self player] performSelectorOnMainThread:@selector(cancelAllThumbnailImageRequests) withObject:nil waitUntilDone:NO];
}

-(void)requestThumbnailImagesAtTimes:(id)args
{
	ENSURE_UI_THREAD(requestThumbnailImagesAtTimes,args);
	ONLY_IN_3_2_OR_GREATER(requestThumbnailImagesAtTimes)
	RELEASE_TO_NIL(thumbnailCallback);
	
	NSArray* array = [args objectAtIndex:0];
	NSNumber* option = [args objectAtIndex:1];
	thumbnailCallback = [[args objectAtIndex:2] retain];
	
	[[self player] requestThumbnailImagesAtTimes:array timeOption:[option intValue]];
}

-(void)generateThumbnail:(id)args
{
	ONLY_IN_3_2_OR_GREATER(generateThumbnail)
	
	NSNumber *time = [args objectAtIndex:0];
	NSNumber *options = [args objectAtIndex:1];
	TiBlob *blob = [args objectAtIndex:2];
	UIImage *image = [[self player] thumbnailImageAtTime:[time doubleValue] timeOption:[options intValue]];
	[blob setImage:image];
}

-(TiBlob*)thumbnailImageAtTime:(id)args
{
	ONLY_IN_3_2_OR_GREATER(thumbnailImageAtTime)
						   
	NSMutableArray *array = [NSMutableArray arrayWithArray:args];
	TiBlob *blob = [[[TiBlob alloc] init] autorelease];
	[array addObject:blob];
	[self performSelectorOnMainThread:@selector(generateThumbnail:) withObject:array waitUntilDone:YES];
	return blob;
}
#endif

-(void)setBackgroundColor:(id)color
{
	[self replaceValue:color forKey:color notification:NO];
	
	RELEASE_TO_NIL(backgroundColor);
	backgroundColor = [[TiUtils colorValue:color] retain];
	
	if (movie != nil) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
		if ([TiUtils isiPhoneOS3_2OrGreater]) {
			UIView *background = [[self player] backgroundView];
			if (background!=nil)
			{
				[background performSelectorOnMainThread:@selector(setBackgroundColor:) withObject:[backgroundColor _color] waitUntilDone:NO];
				return;
			}
		}
#endif
	}
	else {
		[loadProperties setValue:color forKey:@"backgroundColor"];
	}
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	// Have to make sure the view is created on the main thread, and that all properties are set there.
	[self makeViewPerformSelector:@selector(setBackgroundColor:) 
					   withObject:[backgroundColor _color] 
				   createIfNeeded:YES
					waitUntilDone:NO];
#endif
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
-(NSNumber*)playableDuration
{
	ONLY_IN_3_2_OR_GREATER(playableDuration)
	
	if (movie != nil) {
		return NUMDOUBLE([[self player] playableDuration]);
	}
	else {
		return NUMINT(0);
	}
}

-(NSNumber*)duration
{
	ONLY_IN_3_2_OR_GREATER(duration)
	
	if (movie != nil) {
		return NUMDOUBLE([[self player] duration]);
	}
	else {
		return NUMINT(0);
	}
}

-(NSNumber*)currentPlaybackTime
{
	ONLY_IN_3_2_OR_GREATER(currentPlaybackTime)
	
	if (movie != nil) {
		return NUMDOUBLE([[self player] currentPlaybackTime]);
	}
	else {
		return NUMINT(0);
	}
}

-(NSNumber*)endPlaybackTime
{
	ONLY_IN_3_2_OR_GREATER(endPlaybackTime)
	
	if (movie != nil) {
		return NUMDOUBLE([[self player] endPlaybackTime]);
	}
	else {
		return NUMINT(0);
	}
}

// Note that if we set the value on the UI thread, we have to return the value from the UI thread -
// otherwise the request for the value may come in before it's set.  The alternative is to r/w lock
// when reading properties - maybe we should do that instead.
-(NSNumber*)fullscreen
{
	ONLY_IN_3_2_OR_GREATER(fullscreen)
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(fullscreen) withObject:nil waitUntilDone:YES];
		return [returnCache valueForKey:@"fullscreen"];
	}
	
	if (movie != nil) {
		NSNumber* result = NUMBOOL([[self player] isFullscreen]);
		[returnCache setValue:result forKey:@"fullscreen"];
		return result;
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"fullscreen",NUMBOOL(NO));
	}
}

-(NSNumber*)loadState
{
	ONLY_IN_3_2_OR_GREATER(loadState)
	
	if (movie != nil) {
		return NUMINT([[self player] loadState]);
	}
	else {
		return NUMINT(MPMovieLoadStateUnknown);
	}
}

-(NSNumber*)mediaTypes
{
	ONLY_IN_3_2_OR_GREATER(mediaTypes)
	
	if (movie != nil) {
		return NUMINT([[self player] movieMediaTypes]);
	}
	else {
		return NUMINT(MPMovieMediaTypeMaskNone);
	}
}

-(NSNumber*)sourceType
{
	ONLY_IN_3_2_OR_GREATER(sourceType)
	
	if (movie != nil) {
		return NUMINT([[self player] movieSourceType]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"sourceType",NUMINT(MPMovieSourceTypeUnknown));
	}
}

-(void)setSourceType:(id)type
{
	ONLY_IN_3_2_OR_GREATER(setSourceType)
	
	ENSURE_SINGLE_ARG(type,NSObject);
	if (movie != nil) {
		[self player].movieSourceType = [TiUtils intValue:type];
	}
	else {
		[loadProperties setValue:type forKey:@"sourceType"];
	}
}

-(NSNumber*)playbackState
{
	ONLY_IN_3_2_OR_GREATER(playbackState)
	
	if (movie != nil) {
		return NUMINT([[self player] playbackState]);
	}
}

-(void)setRepeatMode:(id)value
{
	ONLY_IN_3_2_OR_GREATER(setRepeatMode)
	
	if (movie != nil) {
		[[self player] setRepeatMode:[TiUtils intValue:value]];
	}
	else {
		[loadProperties setValue:value forKey:@"repeatMode"];
	}
}

-(NSNumber*)repeatMode
{
	ONLY_IN_3_2_OR_GREATER(repeatMode)
	
	if (movie != nil) {
		return NUMINT([[self player] repeatMode]);
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"repeatMode",NUMINT(MPMovieRepeatModeNone));
	}
}

-(id)naturalSize
{
	ONLY_IN_3_2_OR_GREATER(naturalSize)
	
	if (movie != nil) {
		NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
		CGSize size = [[self player] naturalSize];
		[dictionary setObject:NUMDOUBLE(size.width) forKey:@"width"];
		[dictionary setObject:NUMDOUBLE(size.height) forKey:@"height"];
		return dictionary;
	}
	else {
		return [NSDictionary dictionaryWithObjectsAndKeys:NUMDOUBLE(0),@"width",NUMDOUBLE(0),@"height",nil];
	}
}

-(void)setFullscreen:(id)value
{
	ENSURE_UI_THREAD(setFullscreen,value);
	ONLY_IN_3_2_OR_GREATER(setFullscreen)
	
	if (movie != nil) {
		BOOL fs = [TiUtils boolValue:value];
		[[self player] setFullscreen:fs];
	}
	
	if ([value isEqual:[loadProperties valueForKey:@"fullscreen"]])
	{
		//This is to stop mutating loadProperties while configurePlayer.
		return;
	}
	
	// Movie players are picky.  You can't set the fullscreen value until
	// the movie's size has been determined, so we always have to cache the value - just in case
	// it's set before then.
	if (!sizeDetermined || movie == nil) {
		[loadProperties setValue:value forKey:@"fullscreen"];
	}
}
#endif

-(TiColor*)backgroundColor
{
	if (movie != nil) {
		return backgroundColor;
	}
	else {
		RETURN_FROM_LOAD_PROPERTIES(@"backgroundColor",nil);
	}
}

-(void)stop:(id)args
{
	ENSURE_UI_THREAD(stop, args);
	
	if (!playing) {
		return;
	}
	
	playing = NO;
	if (movie!=nil)
	{
		[movie stop];
	}
	RELEASE_TO_NIL_AUTORELEASE(movie);
}

-(void)play:(id)args
{
	ENSURE_UI_THREAD(play, args);
	
	if (playing) {
		return;
	}
	
	if (url == nil)
	{
		[self throwException:TiExceptionInvalidType
				subreason:@"Tried to play movie player without a valid url, media, or contentURL property"
				location:CODELOCATION];
	}
	
	if (playing)
	{
		[self stop:nil];
	}
	
	playing = YES;
	[[self player] play];
}

-(void)pause:(id)args
{
	ENSURE_UI_THREAD(pause,args)	
	if (!playing) {
		return;
	}
	
	// For the purposes of cleanup, we're still playing, so don't toggle that.
	if ([[self player] respondsToSelector:@selector(pause)])
	{
		[[self player] performSelector:@selector(pause)];
	}
}

-(void)release:(id)args
{
	ENSURE_UI_THREAD(release,args);
	[self stop:nil];
	[self detachView];
	[self _destroy];
}


-(void)add:(id)viewProxy
{
	ENSURE_UI_THREAD(add,viewProxy);
	ENSURE_SINGLE_ARG(viewProxy,TiViewProxy);
	if (views==nil)
	{
		views = TiCreateNonRetainingArray();
	}
	[views addObject:viewProxy];
	if ([TiUtils isiPhoneOS3_2OrGreater])
	{
		[super add:viewProxy];
	}
}

-(void)remove:(id)viewProxy
{
	ENSURE_SINGLE_ARG(viewProxy,TiViewProxy);
	if (![TiUtils isiPhoneOS3_2OrGreater]) {
		if (views!=nil)
		{
			[views removeObject:viewProxy];
			[[viewProxy view] removeFromSuperview];
			[viewProxy detachView];
			
			if ([views count]==0)
			{
				RELEASE_TO_NIL(views);
			}
		}
	}
	else {
		[views removeObject:viewProxy];
		if ([self viewAttached]) {
			[super remove:viewProxy];
		}
	}
}

#pragma mark Delegate Callbacks

- (void) handlePlayerNotification: (NSNotification *) notification
{
	if ([notification object] != movie) 
	{
		return;
	}
	
	NSString * name = [notification name];
	
	if ([name isEqualToString:MPMoviePlayerPlaybackDidFinishNotification])
	{
		if ([self _hasListeners:@"complete"])
		{
			NSMutableDictionary *event = [NSMutableDictionary dictionary];
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
			if ([TiUtils isiPhoneOS3_2OrGreater]) {
				NSNumber *reason = [[notification userInfo] objectForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey];
				if (reason!=nil)
				{
					[event setObject:reason forKey:@"reason"];
				}
			}
#endif			
			[self fireEvent:@"complete" withObject:event];
		}
		// Release memory
		playing = NO;
		RELEASE_TO_NIL_AUTORELEASE(movie);
	}
#if __IPHONE_OS_VERSION_MAX_ALLOWED < __IPHONE_3_2
	else if ([name isEqualToString:MPMoviePlayerContentPreloadDidFinishNotification])
	{
		NSError *error = [[notification userInfo] objectForKey:@"error"];
		if (error!=nil && [self _hasListeners:@"error"])
		{
			NSString *message = error!=nil ? [error description] : nil;
			NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:message,@"message",NUMBOOL(error==nil),@"success",nil];
			[self fireEvent:@"error" withObject:event];
		}
		else if ([self _hasListeners:@"preload"])
		{
			NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:[NSNull null],@"message",NUMBOOL(YES),@"success",nil];
			[self fireEvent:@"preload" withObject:event];
		}
	}
#endif	
	else if ([name isEqualToString:MPMoviePlayerScalingModeDidChangeNotification] && [self _hasListeners:@"resize"])
	{
		[self fireEvent:@"resize" withObject:nil];
	}
}

-(void)handleKeyWindowChanged:(NSNotification*)note
{
	if (playing)
	{
		if ([self _hasListeners:@"load"])
		{
			[self fireEvent:@"load" withObject:nil];
		}
		if (![TiUtils isiPhoneOS3_2OrGreater] && views!=nil && [views count]>0)
		{
			UIWindow *window = [note object];
			
			// get the window background views surface and place it on there
			// it's already rotated and will give us better positioning 
			// on the right surface area
			legacyWindowView = [[[[window subviews] objectAtIndex:0] subviews] objectAtIndex:0];
			CGRect bounds = [legacyWindowView bounds];
			for (TiViewProxy *proxy in views)
			{
				[proxy setSandboxBounds:bounds];
				[proxy insertIntoView:legacyWindowView bounds:bounds];
			}
		}
	}
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
-(void)handleThumbnailImageRequestFinishNotification:(NSNotification*)note
{
	if (thumbnailCallback!=nil)
	{
		NSDictionary *userinfo = [note userInfo];
		NSMutableDictionary *event = [NSMutableDictionary dictionary];
		NSError* value = [userinfo objectForKey:MPMoviePlayerThumbnailErrorKey];
		if (value!=nil)
		{
			[event setObject:NUMBOOL(NO) forKey:@"success"];
			[event setObject:[value description] forKey:@"error"];
		}
		else 
		{
			[event setObject:NUMBOOL(YES) forKey:@"success"];
			UIImage *image = [userinfo valueForKey:MPMoviePlayerThumbnailImageKey];
			TiBlob *blob = [[[TiBlob alloc] initWithImage:image] autorelease];
			[event setObject:blob forKey:@"image"];
		}
		[event setObject:[userinfo valueForKey:MPMoviePlayerThumbnailTimeKey] forKey:@"time"];
		
		[self _fireEventToListener:@"thumbnail" withObject:event listener:thumbnailCallback thisObject:nil];

		RELEASE_TO_NIL(thumbnailCallback);
	}
}

-(void)handleRotationNotification:(NSNotification*)note
{
	// Only track if we're fullscreen
	if (movie != nil) {
		hasRotated = [[self player] isFullscreen];
	}
}

-(void)handleFullscreenEnterNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"fullscreen"])
	{
		NSDictionary *userinfo = [note userInfo];
		NSMutableDictionary *event = [NSMutableDictionary dictionary];
		[event setObject:[userinfo valueForKey:MPMoviePlayerFullscreenAnimationDurationUserInfoKey] forKey:@"duration"];
		[event setObject:NUMBOOL(YES) forKey:@"entering"];
		[self fireEvent:@"fullscreen" withObject:event];
	}	
	hasRotated = NO;
}

-(void)handleFullscreenExitNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"fullscreen"])
	{
		NSDictionary *userinfo = [note userInfo];
		NSMutableDictionary *event = [NSMutableDictionary dictionary];
		[event setObject:[userinfo valueForKey:MPMoviePlayerFullscreenAnimationDurationUserInfoKey] forKey:@"duration"];
		[event setObject:NUMBOOL(NO) forKey:@"entering"];
		[self fireEvent:@"fullscreen" withObject:event];
	}	
	if (hasRotated) {
		[[[TiApp app] controller] resizeView];
		[[[TiApp app] controller] repositionSubviews];
	}
	hasRotated = NO;
}

-(void)handleSourceTypeNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"sourceChange"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self sourceType] forKey:@"sourceType"];
		[self fireEvent:@"sourceChange" withObject:event];
	}
}

-(void)handleDurationAvailableNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"durationAvailable"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self duration] forKey:@"duration"];
		[self fireEvent:@"durationAvailable" withObject:event];
	}
}

-(void)handleMediaTypesNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"mediaTypesAvailable"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self mediaTypes] forKey:@"mediaTypes"];
		[self fireEvent:@"mediaTypesAvailable" withObject:event];
	}
}

-(void)handleNaturalSizeAvailableNotification:(NSNotification*)note
{
	[self setFullscreen:[loadProperties valueForKey:@"fullscreen"]];
	if ([self _hasListeners:@"naturalSizeAvailable"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self naturalSize] forKey:@"naturalSize"];
		[self fireEvent:@"naturalSizeAvailable" withObject:event];
	}
	sizeDetermined = YES;
}

-(void)handleLoadStateChangeNotification:(NSNotification*)note
{
	MPMoviePlayerController *player = [self player];
	
	if (((player.loadState & MPMovieLoadStatePlayable)==MPMovieLoadStatePlayable) || 
		 ((player.loadState & MPMovieLoadStatePlaythroughOK)==MPMovieLoadStatePlaythroughOK)
		&&
		(player.playbackState == MPMoviePlaybackStateStopped||
		player.playbackState == MPMoviePlaybackStatePlaying)) 
	{
		if ([TiUtils isiPhoneOS3_2OrGreater]) {
			TiMediaVideoPlayer *vp = (TiMediaVideoPlayer*)[self view];
			[vp movieLoaded];
		}
	}
	if ([self _hasListeners:@"loadstate"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self loadState] forKey:@"loadState"];
		[self fireEvent:@"loadstate" withObject:event];
	}
}

-(void)handleNowPlayingNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"playing"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self url] forKey:@"url"];
		[self fireEvent:@"playing" withObject:event];
	}
}

-(void)handlePlaybackStateChangeNotification:(NSNotification*)note
{
	if ([self _hasListeners:@"playbackState"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObject:[self playbackState] forKey:@"playbackState"];
		[self fireEvent:@"playbackState" withObject:event];
	}
	switch ([movie playbackState]) {
		case MPMoviePlaybackStateStopped:
			playing = NO;
			break;
		case MPMoviePlaybackStatePlaying:
			playing = YES;
			break;
	}
}
#endif

@end

#endif