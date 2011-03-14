/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#import "TiBase.h"

#ifdef USE_TI_FILESYSTEM

#import "TiFile.h"

@interface TiFilesystemFileProxy : TiFile {
@private
	NSFileManager *fm;
}

-(id)initWithFile:(NSString*)path;

+(id)makeTemp:(BOOL)isDirectory;

@property(nonatomic,readonly) id name;
@property(nonatomic,readonly) id nativePath;
@property(nonatomic,readonly) id readonly;
@property(nonatomic,readonly) id writable;
@property(nonatomic,readonly) id symbolicLink;
@property(nonatomic,readonly) id executable;
@property(nonatomic,readonly) id hidden;



@end

#endif