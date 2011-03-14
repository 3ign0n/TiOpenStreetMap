/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010 by OsmmExample, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */

#import "TiUIiOSProxy.h"
#import "TiUtils.h"

#ifdef USE_TI_UIIOS

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0

#ifdef USE_TI_UIIOSADVIEW
#import "TiUIiOSAdViewProxy.h"
#endif

#endif


@implementation TiUIiOSProxy

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0
#ifdef USE_TI_UIIOSADVIEW

-(id)createAdView:(id)args
{
	return [[[TiUIiOSAdViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

#endif
#endif

@end

#endif