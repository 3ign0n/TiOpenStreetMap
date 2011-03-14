/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 * Special thanks to Steve Tramer for implementing this.
 */
#ifdef USE_TI_NETWORK

#import <Foundation/Foundation.h>
#import "TiProxy.h"
#import "TiNetworkTCPSocketProxy.h"
#import <Foundation/NSNetServices.h>

#if !defined(__IPHONE_4_0) || (__IPHONE_OS_VERSION_MAX_ALLOWED < __IPHONE_4_0)
//Prior to 4.0, All the delegate protocol didn't exist. Instead, the methods
//were a category on NSObject. So to make this compile for 3.x, we make an empty protocol.
@protocol NSNetServiceDelegate <NSObject>
@end

#endif

// NSNetService Delegate
@interface TiNetworkBonjourServiceProxy : TiProxy<NSNetServiceDelegate> {
    TiNetworkTCPSocketProxy* socket;
    NSNetService* service;
    
    BOOL local;
    BOOL published;
    NSString* error;
    NSCondition* connectCondition;
	
	NSNetServiceBrowser* domainBrowser;
    NSMutableArray* domains;
    
    NSString* searchError;
    BOOL searching;
    NSCondition* searchCondition;
}

-(NSNetService*)service;

-(id)initWithContext:(id<TiEvaluator>)context_ service:(NSNetService*)service_ local:(bool)local_;

-(void)publish:(id)arg;
-(void)resolve:(id)args;
-(void)stop:(id)arg;

@property(readonly) TiNetworkTCPSocketProxy* socket;
@property(readonly, nonatomic) NSString* name;
@property(readonly, nonatomic) NSString* type;
@property(readonly, nonatomic) NSString* domain;
@property(readonly, nonatomic, getter=isLocal) NSNumber* local;

#pragma mark internal

-(void)searchDomains:(id)unused;
-(void)stopDomainSearch:(id)unused;
-(NSNumber*)isSearching:(id)unused;
+(NSString*)stringForErrorCode:(NSNetServicesError)code;

@end

#endif