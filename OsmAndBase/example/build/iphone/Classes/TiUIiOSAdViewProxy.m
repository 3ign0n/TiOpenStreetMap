/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010 by OsmmExample, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#import "TiBase.h"
#import "TiUIiOSAdViewProxy.h"
#import "TiUtils.h"

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_0

#ifdef USE_TI_UIIOSADVIEW

#import <iAd/iAd.h>

@implementation TiUIiOSAdViewProxy

MAKE_SYSTEM_STR(SIZE_320x50,ADBannerContentSizeIdentifier320x50);
MAKE_SYSTEM_STR(SIZE_480x32,ADBannerContentSizeIdentifier480x32);

USE_VIEW_FOR_AUTO_HEIGHT
USE_VIEW_FOR_AUTO_WIDTH

-(void)cancelAction:(id)args
{
	[[self view] performSelectorOnMainThread:@selector(cancelAction:) withObject:args waitUntilDone:NO];
}

@end

#endif

#endif