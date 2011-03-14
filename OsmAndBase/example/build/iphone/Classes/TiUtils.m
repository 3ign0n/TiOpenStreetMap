/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#import <QuartzCore/QuartzCore.h>

#import "TiBase.h"
#import "TiUtils.h"
#import "TiHost.h"
#import "TiPoint.h"
#import "TiProxy.h"
#import "ImageLoader.h"
#import "WebFont.h"
#import "TiDimension.h"
#import "TiColor.h"
#import "TiFile.h"
#import "TiBlob.h"


#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
// for checking version
#import <sys/utsname.h>
#endif

#import "UIImage+Resize.h"

#if TARGET_IPHONE_SIMULATOR
extern NSString * const TI_APPLICATION_RESOURCE_DIR;
#endif

@implementation TiUtils

+(BOOL)isRetinaDisplay
{
	// since we call this alot, cache it
	static CGFloat scale = 0.0;
	if (scale == 0.0)
	{
#if __IPHONE_3_2 <= __IPHONE_OS_VERSION_MAX_ALLOWED
// NOTE: iPad in iPhone compatibility mode will return a scale factor of 2.0
// when in 2x zoom, which leads to false positives and bugs. This tries to
// future proof against possible different model names, but in the event of
// an iPad with a retina display, this will need to be fixed.
// Credit to Brion on github for the origional fix.
		if(UI_USER_INTERFACE_IDIOM()==UIUserInterfaceIdiomPhone)
		{
			NSRange iPadStringPosition = [[[UIDevice currentDevice] model] rangeOfString:@"iPad"];
			if(iPadStringPosition.location != NSNotFound)
			{
				scale = 1.0;
				return NO;
			}
		}
#endif
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
		if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)])
		{
			scale = [[UIScreen mainScreen] scale];
		}
#endif
	}
	return scale > 1.0;
}

+(BOOL)isIOS4_2OrGreater
{
	return [UIView instancesRespondToSelector:@selector(drawRect:forViewPrintFormatter:)];
}

+(BOOL)isIOS4OrGreater
{
	return [UIView instancesRespondToSelector:@selector(contentScaleFactor)];
}

+(BOOL)isiPhoneOS3_2OrGreater
{
	// Here's a cheap way to test for 3.2; does it respond to a selector that was introduced with that version?
	return [[UIApplication sharedApplication] respondsToSelector:@selector(setStatusBarHidden:withAnimation:)];
}

+(BOOL)isIPad
{
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
	if ([TiUtils isiPhoneOS3_2OrGreater]) {
		return UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad;
	}
#endif
	return NO;
}

+(BOOL)isIPhone4
{
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	static bool iphone_checked = NO;
	static bool iphone4 = NO;
	if (iphone_checked==NO)
	{
		iphone_checked = YES;
		// for now, this is all we know. we assume this
		// will continue to increase with new models but
		// for now we can't really assume
		if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone)
		{
			struct utsname u;
			uname(&u);
			if (!strcmp(u.machine, "iPhone3,1"))
			{
				iphone4 = YES;
			}
		}
	}
	return iphone4;
#endif
	return NO;
}

+(void)queueAnalytics:(NSString*)type name:(NSString*)name data:(NSDictionary*)data
{
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
						   VAL_OR_NSNULL(type),@"type",
						   VAL_OR_NSNULL(name),@"name",
						   VAL_OR_NSNULL(data),@"data",
						   nil];
	WARN_IF_BACKGROUND_THREAD;	//NSNotificationCenter is not threadsafe!
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiAnalyticsNotification object:nil userInfo:event];
}

+(NSString *)UTCDateForDate:(NSDate*)data
{
	NSDateFormatter *dateFormatter = [[[NSDateFormatter alloc] init] autorelease];
	NSTimeZone *timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
	[dateFormatter setTimeZone:timeZone];

	NSLocale * USLocale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US"];
	[dateFormatter setLocale:USLocale];
	[USLocale release];


	//Example UTC full format: 2009-06-15T21:46:28.685+0000
	[dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss'.'SSS+0000"];
	return [dateFormatter stringFromDate:data];
}

+(NSDate *)dateForUTCDate:(NSString*)date
{
	NSDateFormatter *dateFormatter = [[[NSDateFormatter alloc] init] autorelease];
	NSTimeZone* timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
	[dateFormatter setTimeZone:timeZone];
	
	NSLocale* USLocale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US"];
	[dateFormatter setLocale:USLocale];
	[USLocale release];
	
	[dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss'.'SSS+0000"];
	return [dateFormatter dateFromString:date];
}

+(NSString *)UTCDate
{
	return [TiUtils UTCDateForDate:[NSDate date]];
}

+(NSString*)createUUID
{
	CFUUIDRef resultID = CFUUIDCreate(NULL);
	NSString * resultString = (NSString *) CFUUIDCreateString(NULL, resultID);
	CFRelease(resultID);
	return [resultString autorelease];
}

+(TiFile*)createTempFile:(NSString*)extension
{
	return [TiFile createTempFile:extension];
}

+(NSString *)encodeQueryPart:(NSString *)unencodedString
{
	NSString * result = (NSString *)CFURLCreateStringByAddingPercentEscapes(
															   NULL,
															   (CFStringRef)unencodedString,
															   NULL,
															   (CFStringRef)@"!*'();:@+$,/?%#[]=", 
															   kCFStringEncodingUTF8 );
	[result autorelease];
	return result;
}

+(NSString *)encodeURIParameters:(NSString *)unencodedString
{
	// NOTE: we must encode each individual part for the to successfully work
	
	NSMutableString *result = [[[NSMutableString alloc]init] autorelease];
	
	NSArray *parts = [unencodedString componentsSeparatedByString:@"&"];
	for (int c=0;c<[parts count];c++)
	{
		NSString *part = [parts objectAtIndex:c];
		NSRange range = [part rangeOfString:@"="];
		
		if (range.location != NSNotFound)
		{
			[result appendString:[TiUtils encodeQueryPart:[part substringToIndex:range.location]]];
			[result appendString:@"="];
			[result appendString:[TiUtils encodeQueryPart:[part substringFromIndex:range.location+1]]];
		}
		else 
		{
			[result appendString:[TiUtils encodeQueryPart:part]];
		}
		
		
		if (c + 1 < [parts count])
		{
			[result appendString:@"&"];
		}
	}
	
	return result;
}

+(NSString*)stringValue:(id)value
{
	if ([value isKindOfClass:[NSString class]])
	{
		return (NSString*)value;
	}
	else if ([value isKindOfClass:[NSNull class]])
	{
		return nil;
	}
	else if ([value respondsToSelector:@selector(stringValue)])
	{
		return [value stringValue];
	}
	return [value description];
}

+(BOOL)boolValue:(id)value def:(BOOL)def;
{
	if ([value respondsToSelector:@selector(boolValue)])
	{
		return [value boolValue];
	}
	return def;
}

+(BOOL)boolValue:(id)value
{
	return [self boolValue:value def:NO];
}

+(double)doubleValue:(id)value
{
	if ([value respondsToSelector:@selector(doubleValue)])
	{
		return [value doubleValue];
	}
	return 0;
}

+(UIEdgeInsets)contentInsets:(id)value
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		NSDictionary *dict = (NSDictionary*)value;
		CGFloat t = [TiUtils floatValue:@"top" properties:dict def:0];
		CGFloat l = [TiUtils floatValue:@"left" properties:dict def:0];
		CGFloat b = [TiUtils floatValue:@"bottom" properties:dict def:0];
		CGFloat r = [TiUtils floatValue:@"right" properties:dict def:0];
		return UIEdgeInsetsMake(t, l, b, r);
	}
	return UIEdgeInsetsMake(0,0,0,0);
}

+(CGRect)rectValue:(id)value
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		NSDictionary *dict = (NSDictionary*)value;
		CGFloat x = [TiUtils floatValue:@"x" properties:dict def:0];
		CGFloat y = [TiUtils floatValue:@"y" properties:dict def:0];
		CGFloat w = [TiUtils floatValue:@"width" properties:dict def:0];
		CGFloat h = [TiUtils floatValue:@"height" properties:dict def:0];
		return CGRectMake(x, y, w, h);
	}
	return CGRectMake(0, 0, 0, 0);
}

+(CGPoint)pointValue:(id)value
{
	if ([value isKindOfClass:[TiPoint class]])
	{
		return [value point];
	}
	if ([value isKindOfClass:[NSDictionary class]])
	{
		return CGPointMake([[value objectForKey:@"x"] floatValue],[[value objectForKey:@"y"] floatValue]);
	}
	return CGPointMake(0,0);
}

+(CGPoint)pointValue:(id)value bounds:(CGRect)bounds defaultOffset:(CGPoint)defaultOffset;
{
	TiDimension xDimension;
	TiDimension yDimension;
	CGPoint result;

	if ([value isKindOfClass:[TiPoint class]])
	{
		xDimension = [value xDimension];
		yDimension = [value yDimension];
	}
	else if ([value isKindOfClass:[NSDictionary class]])
	{
		xDimension = [self dimensionValue:@"x" properties:value];
		yDimension = [self dimensionValue:@"x" properties:value];
	}
	else
	{
		xDimension = TiDimensionUndefined;
		yDimension = TiDimensionUndefined;
	}

	if (!TiDimensionDidCalculateValue(xDimension, bounds.size.width, &result.x))
	{
		result.x = defaultOffset.x * bounds.size.width;
	}
	if (!TiDimensionDidCalculateValue(yDimension, bounds.size.height, &result.y))
	{
		result.y = defaultOffset.y * bounds.size.height;
	}

	return CGPointMake(result.x + bounds.origin.x,result.y + bounds.origin.y);
}



+(CGFloat)floatValue:(id)value def:(CGFloat) def
{
	if ([value respondsToSelector:@selector(floatValue)])
	{
		return [value floatValue];
	}
	return def;
}

+(CGFloat)floatValue:(id)value
{
	return [self floatValue:value def:0];
}

+(int)intValue:(id)value def:(int)def
{
	if ([value respondsToSelector:@selector(intValue)])
	{
		return [value intValue];
	}
	return def;
}

+(int)intValue:(id)value
{
	return [self intValue:value def:0];
}

+(TiColor*)colorValue:(id)value
{
	if ([value isKindOfClass:[TiColor class]])
	{
		return (TiColor*)value;
	}
	if ([value respondsToSelector:@selector(stringValue)])
	{
		value = [value stringValue];
	}
	if ([value isKindOfClass:[NSString class]])
	{
		return [TiColor colorNamed:value]; 
	}
	return nil;
}

+(TiDimension)dimensionValue:(id)value
{
	return TiDimensionFromObject(value);
}

+(id)valueFromDimension:(TiDimension)dimension
{
	switch (dimension.type)
	{
		case TiDimensionTypeUndefined:
			return [NSNull null];
		case TiDimensionTypeAuto:
			return @"auto";
		case TiDimensionTypePixels:
			return [NSNumber numberWithFloat:dimension.value];
	}
	return nil;
}

+(UIImage*)scaleImage:(UIImage *)image toSize:(CGSize)newSize
{
	if (!CGSizeEqualToSize(newSize, CGSizeZero))
	{
		CGSize imageSize = [image size];
		if (newSize.width==0)
		{
			newSize.width = imageSize.width;
		}
		if (newSize.height==0)
		{
			newSize.height = imageSize.height;
		}
		if (!CGSizeEqualToSize(newSize, imageSize))
		{
			image = [UIImageResize resizedImage:newSize interpolationQuality:kCGInterpolationDefault image:image hires:NO];
		}
	}
	return image;
}

+(UIImage*)toImage:(id)object proxy:(TiProxy*)proxy size:(CGSize)imageSize
{
	if ([object isKindOfClass:[TiBlob class]])
	{
		return [self scaleImage:[(TiBlob *)object image] toSize:imageSize];
	}

	if ([object isKindOfClass:[TiFile class]])
	{
		TiFile *file = (TiFile*)object;
		UIImage *image = [UIImage imageWithContentsOfFile:[file path]];
		return [self scaleImage:image toSize:imageSize];
	}

	NSURL * urlAttempt = [self toURL:object proxy:proxy];
	UIImage * image = [[ImageLoader sharedLoader] loadImmediateImage:urlAttempt withSize:imageSize];
	return image;
	//Note: If url is a nonimmediate image, this returns nil.
}

+(UIImage*)toImage:(id)object proxy:(TiProxy*)proxy
{
	if ([object isKindOfClass:[TiBlob class]])
	{
		return [(TiBlob *)object image];
	}

	if ([object isKindOfClass:[TiFile class]])
	{
		TiFile *file = (TiFile*)object;
		UIImage *image = [UIImage imageWithContentsOfFile:[file path]];
		return image;
	}

	NSURL * urlAttempt = [self toURL:object proxy:proxy];
	UIImage * image = [[ImageLoader sharedLoader] loadImmediateImage:urlAttempt];
	return image;
	//Note: If url is a nonimmediate image, this returns nil.
}

+(NSURL*)checkFor2XImage:(NSURL*)url
{
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
	if ([url isFileURL] && [TiUtils isRetinaDisplay])
	{
		NSString *path = [url path];
		if ([path hasSuffix:@".png"] || [path hasSuffix:@".jpg"])
		{
			//NOTE; I'm not sure the order here.. the docs don't necessarily 
			//specify the exact order 
			NSFileManager *fm = [NSFileManager defaultManager];
			NSString *partial = [path substringToIndex:[path length]-4];
			NSString *ext = [path substringFromIndex:[path length]-4];
			NSString *os = [TiUtils isIPad] ? @"~ipad" : @"~iphone";
			// first try 2x device specific
			NSString *testpath = [NSString stringWithFormat:@"%@@2x%@%@",partial,os,ext];
			if ([fm fileExistsAtPath:testpath])
			{
				return [NSURL fileURLWithPath:testpath];
			}
			// second try plain 2x
			testpath = [NSString stringWithFormat:@"%@@2x%@",partial,ext];
			if ([fm fileExistsAtPath:testpath])
			{
				return [NSURL fileURLWithPath:testpath];
			}
			// third try just device specific normal res
			testpath = [NSString stringWithFormat:@"%@%@%@",partial,os,ext];
			if ([fm fileExistsAtPath:testpath])
			{
				return [NSURL fileURLWithPath:testpath];
			}
		}
	}
#endif
	return url;
}

const CFStringRef charactersThatNeedEscaping = NULL;
const CFStringRef charactersToNotEscape = CFSTR(":[]@!$ '()*+,;\"<>%{}|\\^~`#");

+(NSURL*)toURL:(id)object proxy:(TiProxy*)proxy
{
	NSURL *url = nil;
	
	if ([object isKindOfClass:[NSString class]])
	{
		if ([object hasPrefix:@"/"])
		{
			return [TiUtils checkFor2XImage:[NSURL fileURLWithPath:object]];
		}
		if ([object hasPrefix:@"sms:"] || 
			[object hasPrefix:@"tel:"] ||
			[object hasPrefix:@"mailto:"])
		{
			return [NSURL URLWithString:object];
		}
		
		// don't bother if we don't at least have a path and it's not remote
		///TODO: This looks ugly and klugy.
		NSString *urlString = [TiUtils stringValue:object];
		if ([urlString hasPrefix:@"http"])
		{
			NSRange range = [urlString rangeOfString:@"/" options:0 range:NSMakeRange(7, [urlString length]-7)];
			if (range.location!=NSNotFound)
			{
				NSString *firstPortion = [urlString substringToIndex:range.location];
				NSString *pathPortion = [urlString substringFromIndex:range.location];
				CFStringRef escapedPath = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault,
						(CFStringRef)pathPortion, charactersToNotEscape,charactersThatNeedEscaping,
						kCFStringEncodingUTF8);
				urlString = [firstPortion stringByAppendingString:(NSString *)escapedPath];
				if(escapedPath != NULL)
				{
					CFRelease(escapedPath);
				}
			}
		}
		
		url = [NSURL URLWithString:urlString relativeToURL:[proxy _baseURL]];
		
		if (url==nil)
		{
			//encoding problem - fail fast and make sure we re-escape
			NSRange range = [object rangeOfString:@"?"];
			if (range.location != NSNotFound)
			{
				NSString *qs = [TiUtils encodeURIParameters:[object substringFromIndex:range.location+1]];
				NSString *newurl = [NSString stringWithFormat:@"%@?%@",[object substringToIndex:range.location],qs];
				return [TiUtils checkFor2XImage:[NSURL URLWithString:newurl]];
			}
		}
	}
	else if ([object isKindOfClass:[NSURL class]])
	{
		return [TiUtils checkFor2XImage:[NSURL URLWithString:[object absoluteString] relativeToURL:[proxy _baseURL]]];
	}
	return [TiUtils checkFor2XImage:url];			  
}

+(UIImage *)stretchableImage:(id)object proxy:(TiProxy*)proxy
{
	return [[ImageLoader sharedLoader] loadImmediateStretchableImage:[self toURL:object proxy:proxy]];
}

+(UIImage *)image:(id)object proxy:(TiProxy*)proxy
{
	return [[ImageLoader sharedLoader] loadImmediateImage:[self toURL:object proxy:proxy]];
}


+(int)intValue:(NSString*)name properties:(NSDictionary*)properties def:(int)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(intValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value intValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)properties def:(double)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(doubleValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value doubleValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)properties def:(float)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(floatValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value floatValue];
		}		
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)properties def:(BOOL)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(boolValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value boolValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties def:(NSString*)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value isKindOfClass:[NSString class]])
		{
			if (exists != NULL) *exists = YES;
			return value;
		}
		else if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		else if ([value respondsToSelector:@selector(stringValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value stringValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties def:(CGPoint)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value isKindOfClass:[NSDictionary class]])
		{
			NSDictionary *dict = (NSDictionary*)value;
			CGPoint point;
			point.x = [self doubleValue:@"x" properties:dict def:def.x];
			point.y = [self doubleValue:@"y" properties:dict def:def.y];
			if (exists != NULL) *exists = YES;
			return point;
		}
	}

	if (exists != NULL) *exists = NO;
	return def;
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties def:(TiColor*)def exists:(BOOL*) exists
{
	TiColor * result = nil;
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		if ([value respondsToSelector:@selector(stringValue)])
		{
			value = [value stringValue];
		}
		if ([value isKindOfClass:[NSString class]])
		{
			// need to retain here since we autorelease below and since colorName also autoreleases
			result = [[TiColor colorNamed:value] retain]; 
		}
	}
	if (result != nil)
	{
		if (exists != NULL) *exists = YES;
		return [result autorelease];
	}
	
	if (exists != NULL) *exists = NO;
	return def;
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if (value != nil)
		{
			if (exists != NULL)
			{
				*exists = YES;
			}
			return [self dimensionValue:value];
		}
	}
	if (exists != NULL)
	{
		*exists = NO;
	}
	return def;
	
}


+(int)intValue:(NSString*)name properties:(NSDictionary*)props def:(int)def;
{
	return [self intValue:name properties:props def:def exists:NULL];
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)props def:(double)def;
{
	return [self doubleValue:name properties:props def:def exists:NULL];
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)props def:(float)def;
{
	return [self floatValue:name properties:props def:def exists:NULL];
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)props def:(BOOL)def;
{
	return [self boolValue:name properties:props def:def exists:NULL];
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties def:(NSString*)def;
{
	return [self stringValue:name properties:properties def:def exists:NULL];
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties def:(CGPoint)def;
{
	return [self pointValue:name properties:properties def:def exists:NULL];
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties def:(TiColor*)def;
{
	return [self colorValue:name properties:properties def:def exists:NULL];
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def
{
	return [self dimensionValue:name properties:properties def:def exists:NULL];
}



+(int)intValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self intValue:name properties:props def:0 exists:NULL];
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self doubleValue:name properties:props def:0.0 exists:NULL];
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self floatValue:name properties:props def:0.0 exists:NULL];
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self boolValue:name properties:props def:NO exists:NULL];
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self stringValue:name properties:properties def:nil exists:NULL];
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self pointValue:name properties:properties def:CGPointZero exists:NULL];
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self colorValue:name properties:properties def:nil exists:NULL];
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties
{
	return [self dimensionValue:name properties:properties def:TiDimensionUndefined exists:NULL];
}

+(NSDictionary*)pointToDictionary:(CGPoint)point
{
	return [NSDictionary dictionaryWithObjectsAndKeys:
			[NSNumber numberWithDouble:point.x],@"x",
			[NSNumber numberWithDouble:point.y],@"y",
			nil];
}

+(NSDictionary*)rectToDictionary:(CGRect)rect
{
	return [NSDictionary dictionaryWithObjectsAndKeys:
			[NSNumber numberWithDouble:rect.origin.x],@"x",
			[NSNumber numberWithDouble:rect.origin.y],@"y",
			[NSNumber numberWithDouble:rect.size.width],@"width",
			[NSNumber numberWithDouble:rect.size.height],@"height",
			nil];
}

+(NSDictionary*)sizeToDictionary:(CGSize)size
{
	return [NSDictionary dictionaryWithObjectsAndKeys:
			[NSNumber numberWithDouble:size.width],@"width",
			[NSNumber numberWithDouble:size.height],@"height",
			nil];
}

+(CGRect)contentFrame:(BOOL)window
{
	double height = 0;
	if (window && ![[UIApplication sharedApplication] isStatusBarHidden])
	{
		CGRect statusFrame = [[UIApplication sharedApplication] statusBarFrame];
		height = statusFrame.size.height;
	}
	
	CGRect f = [[UIScreen mainScreen] applicationFrame];
	return CGRectMake(f.origin.x, height, f.size.width, f.size.height);
}

+(CGFloat)sizeValue:(id)value
{
	if ([value isKindOfClass:[NSString class]])
	{
		NSString *s = [(NSString*) value stringByReplacingOccurrencesOfString:@"px" withString:@""];
		return [[s stringByReplacingOccurrencesOfString:@" " withString:@""] floatValue];
	}
	return [value floatValue];
}

+(WebFont*)fontValue:(id)value def:(WebFont *)def
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		WebFont *font = [[WebFont alloc] init];
		[font updateWithDict:value inherits:nil];
		return [font autorelease];
	}
	if ([value isKindOfClass:[NSString class]])
	{
		WebFont *font = [[WebFont alloc] init];
		font.family = value;
		font.size = 14;
		return [font autorelease];
	}
	return def;
}


+(WebFont*)fontValue:(id)value
{
	WebFont * result = [self fontValue:value def:nil];
	if (result == nil) {
		result = [WebFont defaultFont];
	}
	return result;
}

+(UITextAlignment)textAlignmentValue:(id)alignment
{
	UITextAlignment align = UITextAlignmentLeft;

	if ([alignment isKindOfClass:[NSString class]])
	{
		if ([alignment isEqualToString:@"left"])
		{
			align = UITextAlignmentLeft;
		}
		else if ([alignment isEqualToString:@"center"])
		{
			align = UITextAlignmentCenter;
		}
		else if ([alignment isEqualToString:@"right"])
		{
			align = UITextAlignmentRight;
		}
	}
	else if ([alignment isKindOfClass:[NSNumber class]])
	{
		align = [alignment intValue];
	}
	return align;
}

+(NSString*)exceptionMessage:(id)arg
{
	if ([arg isKindOfClass:[NSDictionary class]])
	{
		// check to see if the object past is a JS Error object and if so attempt
		// to construct a string that is more readable to the developer
		id message = [arg objectForKey:@"message"];
		if (message!=nil)
		{
			id source = [arg objectForKey:@"sourceURL"];
			if (source!=nil)
			{
				id lineNumber = [arg objectForKey:@"line"];
				return [NSString stringWithFormat:@"%@ at %@ (line %@)",message,[source lastPathComponent],lineNumber];
			}
		}
	}
	return arg;
}

#define RETURN_IF_ORIENTATION_STRING(str,orientation) \
if ([str isEqualToString:@#orientation]) return orientation;

+(UIDeviceOrientation)orientationValue:(id)value def:(UIDeviceOrientation)def
{
	if ([value isKindOfClass:[NSString class]])
	{
		if ([value isEqualToString:@"portrait"])
		{
			return UIDeviceOrientationPortrait;
		}
		if ([value isEqualToString:@"landscape"])
		{
			return UIInterfaceOrientationLandscapeRight;
		}
		
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationPortrait)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationPortraitUpsideDown)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationLandscapeLeft)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationLandscapeRight)
	}

	if ([value respondsToSelector:@selector(intValue)])
	{
		return [value intValue];
	}
	return def;
}

+(BOOL)isOrientationPortait
{
	return UIInterfaceOrientationIsPortrait([self orientation]);
}

+(BOOL)isOrientationLandscape
{
	return UIInterfaceOrientationIsLandscape([self orientation]);
}

+(UIInterfaceOrientation)orientation 
{
	UIDeviceOrientation orient = [UIDevice currentDevice].orientation;
//	TODO: A previous bug was DeviceOrientationUnknown == 0, which is always true. Uncomment this when pushing.
	if (UIDeviceOrientationUnknown == orient) 
	{
		return UIDeviceOrientationPortrait;
	} 
	else 
	{
		return orient;
	}
}

+(CGRect)screenRect
{
	return [UIScreen mainScreen].bounds;
}

//TODO: rework these to be more accurate and multi-device

+(CGRect)navBarRect
{
	CGRect rect = [self screenRect];
	rect.size.height = TI_NAVBAR_HEIGHT;
	return rect;
}

+(CGSize)navBarTitleViewSize
{
	CGRect rect = [self screenRect];
	return CGSizeMake(rect.size.width-TI_NAVBAR_BUTTON_WIDTH, TI_NAVBAR_HEIGHT);
}

+(CGRect)navBarTitleViewRect
{
	CGRect rect = [self screenRect];
	rect.size.height = TI_NAVBAR_HEIGHT;
	rect.size.width-=TI_NAVBAR_BUTTON_WIDTH; // offset for padding on both sides
	return rect;
}

+(CGPoint)centerSize:(CGSize)smallerSize inRect:(CGRect)largerRect
{
	return CGPointMake(
		largerRect.origin.x + (largerRect.size.width - smallerSize.width)/2,
		largerRect.origin.y + (largerRect.size.height - smallerSize.height)/2);
}

+(CGRect)centerRect:(CGRect)smallerRect inRect:(CGRect)largerRect
{
	smallerRect.origin = [self centerSize:smallerRect.size inRect:largerRect];

	return smallerRect;
}

#define USEFRAME	0

+(void)setView:(UIView *)view positionRect:(CGRect)frameRect
{
#if	USEFRAME
	[view setFrame:frameRect];
	return;
#endif
	
	CGPoint anchorPoint = [[view layer] anchorPoint];
	CGPoint newCenter;
	newCenter.x = frameRect.origin.x + (anchorPoint.x * frameRect.size.width);
	newCenter.y = frameRect.origin.y + (anchorPoint.y * frameRect.size.height);
	CGRect newBounds = CGRectMake(0, 0, frameRect.size.width, frameRect.size.height);

	[view setBounds:newBounds];
	[view setCenter:newCenter];
}

+(CGRect)viewPositionRect:(UIView *)view
{
#if	USEFRAME
	return [view frame];
#endif

	CGPoint anchorPoint = [[view layer] anchorPoint];
	CGRect bounds = [view bounds];
	CGPoint center = [view center];
	
	return CGRectMake(center.x - (anchorPoint.x * bounds.size.width),
			center.y - (anchorPoint.y * bounds.size.height),
			bounds.size.width, bounds.size.height);
}

+(NSData *)loadAppResource:(NSURL*)url
{
	BOOL app = [[url scheme] hasPrefix:@"app"];
	if ([url isFileURL] || app)
	{
		BOOL had_splash_removed = NO;
		NSString *urlstring = [[url standardizedURL] path];
		NSString *resourceurl = [[NSBundle mainBundle] resourcePath];
		NSRange range = [urlstring rangeOfString:resourceurl];
		NSString *appurlstr = urlstring;
		if (range.location!=NSNotFound)
		{
			appurlstr = [urlstring substringFromIndex:range.location + range.length + 1];
		}
		if ([appurlstr hasPrefix:@"/"])
		{
			had_splash_removed = YES;
			appurlstr = [appurlstr substringFromIndex:1];
		}
#if TARGET_IPHONE_SIMULATOR
		if (app==YES && had_splash_removed)
		{
			// on simulator we want to keep slash since it's coming from file
			appurlstr = [@"/" stringByAppendingString:appurlstr];
		}
		if (TI_APPLICATION_RESOURCE_DIR!=nil && [TI_APPLICATION_RESOURCE_DIR isEqualToString:@""]==NO)
		{
			if ([appurlstr hasPrefix:TI_APPLICATION_RESOURCE_DIR])
			{
				if ([[NSFileManager defaultManager] fileExistsAtPath:appurlstr])
				{
					return [NSData dataWithContentsOfFile:appurlstr];
				}
			}
			// this path is only taken during a simulator build
			// in this path, we will attempt to load resources directly from the
			// app's Resources directory to speed up round-trips
			NSString *filepath = [TI_APPLICATION_RESOURCE_DIR stringByAppendingPathComponent:appurlstr];
			if ([[NSFileManager defaultManager] fileExistsAtPath:filepath])
			{
				return [NSData dataWithContentsOfFile:filepath];
			}
		}
#endif
		static id AppRouter;
		if (AppRouter==nil)
		{
			AppRouter = NSClassFromString(@"ApplicationRouting");
		}
		if (AppRouter!=nil)
		{
			appurlstr = [appurlstr stringByReplacingOccurrencesOfString:@"." withString:@"_"];
			if ([appurlstr characterAtIndex:0]=='/')
			{
				appurlstr = [appurlstr substringFromIndex:1];
			}
#ifdef DEBUG			
			NSLog(@"[DEBUG] loading: %@, resource: %@",urlstring,appurlstr);
#endif			
			return [AppRouter performSelector:@selector(resolveAppAsset:) withObject:appurlstr];
		}
	}
	return nil;
}

+(BOOL)barTranslucencyForColor:(TiColor *)color
{
	return [color _color]==[UIColor clearColor];
}

+(UIColor *)barColorForColor:(TiColor *)color
{
	UIColor * result = [color _color];
	// TODO: Return nil for the appropriate colors once Apple fixes how the 'cancel' button
	// is displayed on nil-color bars.
	if ((result == [UIColor clearColor]))
	{
		return nil;
	}
	return result;
}

+(UIBarStyle)barStyleForColor:(TiColor *)color
{
	UIColor * result = [color _color];
	// TODO: Return UIBarStyleBlack for the appropriate colors once Apple fixes how the 'cancel' button
	// is displayed on nil-color bars.
	if ((result == [UIColor clearColor]))
	{
		return UIBarStyleBlack;
	}
	return UIBarStyleDefault;
}


+(void)applyColor:(TiColor *)color toNavigationController:(UINavigationController *)navController
{
	UIColor * barColor = [self barColorForColor:color];
	UIBarStyle barStyle = [self barStyleForColor:color];
	BOOL isTranslucent = [self barTranslucencyForColor:color];

	UINavigationBar * navBar = [navController navigationBar];
	[navBar setBarStyle:barStyle];
	[navBar setTranslucent:isTranslucent];
	[navBar setTintColor:barColor];

	UIToolbar * toolBar = [navController toolbar];
	[toolBar setBarStyle:barStyle];
	[toolBar setTranslucent:isTranslucent];
	[toolBar setTintColor:barColor];
}

+(NSString*)replaceString:(NSString *)string characters:(NSCharacterSet *)characterSet withString:(NSString *)replacementString
{
	if(string == nil)
	{
		return nil;
	}

	NSRange setRange = [string rangeOfCharacterFromSet:characterSet];

	if(setRange.location == NSNotFound)
	{
		return string;
	}

	return [[string componentsSeparatedByCharactersInSet:characterSet] componentsJoinedByString:replacementString];
}

@end
