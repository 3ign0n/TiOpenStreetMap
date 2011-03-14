/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#import <Foundation/Foundation.h>
#import "TiCore.h"
#import "KrollContext.h"

//
// KrollCallback is a wrapper around a JS function object which is passed
// from JS land to native.  This object can be passed around on the native
// side as a normal opaque object and then passed back through to Kroll
// for function invocation (or just to pass the function object back as-is)
//
@interface KrollCallback : NSObject {
@private
	TiContextRef jsContext;
	TiObjectRef thisObj;
	TiObjectRef function;
	KrollContext *context;
	NSLock* contextLock;
	NSString *type;
}

@property(nonatomic,readwrite,retain) NSString *type;

-(id)initWithCallback:(TiValueRef)function_ thisObject:(TiObjectRef)thisObject_ context:(KrollContext*)context_;
-(id)call:(NSArray*)args thisObject:(id)thisObject_;
-(TiObjectRef)function;
-(KrollContext*)context;
+(void)shutdownContext:(KrollContext*)context;

@end
