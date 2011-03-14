/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_NETWORK

#import "TiProxy.h"
#import "TiBlob.h"
#import "TiBase.h"
#import "ASIFormDataRequest.h"
#import "ASIProgressDelegate.h"

typedef enum {
	NetworkClientStateUnsent = 0,
	NetworkClientStateOpened = 1,
	NetworkClientStateHeaders = 2,
	NetworkClientStateLoading = 3,
	NetworkClientStateDone = 4,	
} NetworkClientState;


@interface TiNetworkHTTPClientProxy : TiProxy<ASIHTTPRequestDelegate,ASIProgressDelegate> 
{
@private
	ASIFormDataRequest *request;
	NetworkClientState readyState;
	BOOL connected;
	BOOL async;
	NSURL *url;
	long long uploadProgress;
	long long downloadProgress;
	long long downloadLength;
	long long uploadLength;
	BOOL validatesSecureCertificate;
    NSNumber* timeout;
	
	// callbacks
	KrollCallback *onload;
	KrollCallback *onerror;
	KrollCallback *onreadystatechange;
	KrollCallback *ondatastream;
	KrollCallback *onsendstream;
}
// Internal
-(NSDictionary*)responseHeaders;

// event callbacks
@property(nonatomic,retain) KrollCallback* onload;
@property(nonatomic,retain) KrollCallback* onerror;
@property(nonatomic,retain) KrollCallback* onreadystatechange;
@property(nonatomic,retain) KrollCallback* ondatastream;
@property(nonatomic,retain) KrollCallback* onsendstream;

// state information
@property(nonatomic,readonly) NSInteger status;
@property(nonatomic,readonly) BOOL connected;
@property(nonatomic,readonly) NSInteger readyState;
@property(nonatomic,readonly) NSString* responseText;
@property(nonatomic,readonly) TiProxy* responseXML;	
@property(nonatomic,readonly) TiBlob* responseData;	
@property(nonatomic,readonly) NSString* connectionType;
@property(nonatomic,readonly) NSString* location;
@property(nonatomic,readwrite) BOOL validatesSecureCertificate;
@property(nonatomic,retain,readwrite) NSNumber* timeout;

// constants
@property(nonatomic,readonly) NSInteger UNSENT;
@property(nonatomic,readonly) NSInteger OPENED;
@property(nonatomic,readonly) NSInteger HEADERS_RECEIVED;
@property(nonatomic,readonly) NSInteger LOADING;
@property(nonatomic,readonly) NSInteger DONE;

// public methods
-(void)abort:(id)args;
-(void)open:(id)args;
-(void)setRequestHeader:(id)args;
-(void)send:(id)args;
-(id)getResponseHeader:(id)args;

@end

#endif