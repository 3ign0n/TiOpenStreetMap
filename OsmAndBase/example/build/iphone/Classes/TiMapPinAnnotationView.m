/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_MAP

#import "TiMapPinAnnotationView.h"
#import "TiMapView.h"

@implementation TiMapPinAnnotationView

-(void)removeFromSuperview
{
	if (observing)
	{
		[self removeObserver:map forKeyPath:@"selected"];
		observing = NO;
		map = nil;
	}
	[super removeFromSuperview];
}

-(id)initWithAnnotation:(id<MKAnnotation>)annotation reuseIdentifier:(NSString *)reuseIdentifier map:(TiMapView*)map_
{
	if (self = [self initWithAnnotation:annotation reuseIdentifier:reuseIdentifier])
	{
		map = map_;
		[self addObserver:map
				  forKeyPath:@"selected"
					 options:NSKeyValueObservingOptionNew
					 context:@"ANSELECTED"];
		observing = YES;
	}
	return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(lastHitName);
	[super dealloc];
}

-(NSString *)lastHitName
{
	NSString * result = lastHitName;
	[lastHitName autorelease];
	lastHitName = nil;
	return result;
}


- (UIView *)hitTest:(CGPoint) point withEvent:(UIEvent *)event 
{
    UIView * result = [super hitTest:point withEvent:event];
	
	if (result==nil)
	{
		for (UIView * ourSubView in [self subviews])
		{
			CGPoint subPoint = [self convertPoint:point toView:ourSubView];
			for (UIView * ourSubSubView in [ourSubView subviews])
			{
				if (CGRectContainsPoint([ourSubSubView frame], subPoint) &&
					[ourSubSubView isKindOfClass:[UILabel class]])
				{
					NSString * labelText = [(UILabel *)ourSubSubView text];
					TiMapAnnotationProxy * ourProxy = (TiMapAnnotationProxy *)[self annotation];
					RELEASE_TO_NIL(lastHitName);
					if ([labelText isEqualToString:[ourProxy title]])
					{
						lastHitName = [@"title" retain];
					}
					else if ([labelText isEqualToString:[ourProxy subtitle]])
					{
						lastHitName = [@"subtitle" retain];
					}

					return nil;
				}
			}
			if (CGRectContainsPoint([ourSubView bounds], subPoint))
			{
				RELEASE_TO_NIL(lastHitName);
				lastHitName = [@"annotation" retain];
				return nil;
			}
		}
	}
	RELEASE_TO_NIL(lastHitName);
	return result;
}

@end

#endif