/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_UISCROLLABLEVIEW

#import "TiUIView.h"

@interface TiUIScrollableView : TiUIView<UIScrollViewDelegate> {
@private
	UIScrollView *scrollview;
	UIPageControl *pageControl;
	NSMutableArray *views;
	int currentPage;
	BOOL showPageControl;
	CGFloat pageControlHeight;
	BOOL handlingPageControlEvent;
	CGFloat maxScale;
	CGFloat minScale;
}

@end

#endif