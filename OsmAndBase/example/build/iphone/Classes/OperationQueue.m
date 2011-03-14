/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */

#import "TiBase.h"
#import "OperationQueue.h"


OperationQueue *sharedQueue = nil;

@interface OperationQueueOp : NSOperation
{
	SEL selector;
	id target;
	id arg;
	SEL after;
	id afterTarget;
	BOOL ui;
}
-(id)initWithSelector:(SEL)selector target:(id)target arg:(id)arg after:(SEL)after afterTarget:(id)afterTarget ui:(BOOL)ui;
@end

@implementation OperationQueueOp

-(id)initWithSelector:(SEL)selector_ target:(id)target_ arg:(id)arg_ after:(SEL)after_ afterTarget:(id)afterTarget_ ui:(BOOL)ui_
{
	if (self = [super init])
	{
		selector = selector_;
		target = [target_ retain];
		arg = [arg_ retain];
		after = after_;
		afterTarget = [afterTarget_ retain];
		ui = ui_;
	}
	return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(target);
	RELEASE_TO_NIL(arg);
	RELEASE_TO_NIL(afterTarget);
	[super dealloc];
}

-(void)main
{
	NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
	
	@try {
		NSMethodSignature * methodSignature = [target methodSignatureForSelector:selector];
		NSInvocation * invoker = [NSInvocation invocationWithMethodSignature:methodSignature];
		
		[invoker setSelector:selector]; 
		[invoker setTarget:target];
		if (arg!=nil)
		{
			[invoker setArgument:&arg atIndex:2];
		}
		[invoker invoke];
		id result = nil;
		

		if ([methodSignature methodReturnLength] == sizeof(id)) 
		{
			[invoker getReturnValue:&result];
		}

		if (afterTarget!=nil && after!=nil)
		{
			NSMethodSignature * methodSignature2 = [afterTarget methodSignatureForSelector:after];
			if (ui)
			{
				// if UI thread, just use perform
				if ([methodSignature2 numberOfArguments]==3)
				{
					[afterTarget performSelectorOnMainThread:after withObject:result waitUntilDone:YES modes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
				}
				else 
				{
					[afterTarget performSelectorOnMainThread:after withObject:nil waitUntilDone:NO modes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
				}
			}
			else 
			{
				// not on UI, just dynamically invoke
				NSInvocation * invoker2 = [NSInvocation invocationWithMethodSignature:methodSignature2];
				[invoker2 setSelector:after];
				[invoker2 setTarget:afterTarget];
				if ([methodSignature2 numberOfArguments]==3)
				{
					[invoker2 setArgument:&result atIndex:2];
				}
				[invoker2 invoke];
			}
			
		}
	}
	@catch (NSException * e) 
	{
		NSLog(@"[ERROR] unhandled exception raised in OperationQueue. Exception was %@",[e description]);
	}
	[pool release];
}

@end


@implementation OperationQueue

-(id)init
{
	if (self = [super init])
	{
		queue = [[NSOperationQueue alloc] init];
	}
	return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(queue);
	[super dealloc];
}

+(OperationQueue*)sharedQueue
{
	if (sharedQueue==nil)
	{
		sharedQueue = [[OperationQueue alloc] init];
	}
	return sharedQueue;
}

-(void)queue:(SEL)selector target:(id)target arg:(id)arg after:(SEL)after on:(id)on ui:(BOOL)ui
{
	OperationQueueOp *op = [[OperationQueueOp alloc] initWithSelector:selector target:target arg:arg after:after afterTarget:on ui:ui];
	[queue addOperation:op];
	[op release];
}

@end
