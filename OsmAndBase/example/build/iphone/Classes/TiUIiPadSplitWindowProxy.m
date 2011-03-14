/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#import "TiBase.h"

#ifdef USE_TI_UIIPADSPLITWINDOW
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_3_2
#import "TiUIiPadSplitWindowProxy.h"
#import "TiUIiPadSplitWindow.h"
#import "TiUtils.h"
#import "TiApp.h"

@implementation TiUIiPadSplitWindowProxy

-(TiUIView*)newView
{
	return [[TiUIiPadSplitWindow alloc] init];
}

- (UIViewController *)childViewController
{
	return [(TiUIiPadSplitWindow*)[self view] controller];
}

-(void)windowDidOpen 
{
	[super windowDidOpen];
	[self reposition];
}

-(void)windowDidClose
{
	//TODO: reattach the root controller?
	[super windowDidClose];
}

-(void)setToolbar:(id)items withObject:(id)properties
{
	ENSURE_UI_THREAD_WITH_OBJ(setToolbar,items,properties);
	[(TiUIiPadSplitWindow*)[self view] setToolbar:items withObject:properties];
}


-(void)setDetailView:(id<NSObject,TiOrientationController>)newDetailView
{
	ENSURE_UI_THREAD_1_ARG(newDetailView);
	if (newDetailView == detailView)
	{
		return;
	}
	[detailView setParentOrientationController:nil];
	[newDetailView setParentOrientationController:self];
	RELEASE_AND_REPLACE(detailView,newDetailView);
	[self replaceValue:newDetailView forKey:@"detailView" notification:YES];
}

-(TiOrientationFlags)orientationFlags
{
	return [detailView orientationFlags];
}

@end

#endif

#endif