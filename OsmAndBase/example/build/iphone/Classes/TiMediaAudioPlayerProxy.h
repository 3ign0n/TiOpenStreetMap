/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_MEDIA


#import "AudioStreamer/AudioStreamer.h"
#import "TiProxy.h"

@interface TiMediaAudioPlayerProxy : TiProxy<AudioStreamerDelegate> {
@private
	NSURL *url;
	AudioStreamer *player;
	BOOL progress;
	NSTimer *timer;
}

@property (nonatomic,readonly) NSURL *url;
@property (nonatomic,readwrite,assign,getter=isPaused)  NSNumber *paused;
@property (nonatomic,readonly,getter=isPlaying) NSNumber *playing;
@property (nonatomic,readonly,getter=isWaiting) NSNumber *waiting;
@property (nonatomic,readonly,getter=isIdle) NSNumber *idle;
@property (nonatomic,readonly) NSNumber *bitRate;
@property (nonatomic,readonly) NSNumber *progress;
@property (nonatomic,readonly) NSNumber *state;
@property (nonatomic,readwrite,assign) NSNumber* audioSessionMode;

@property (nonatomic,readonly) NSNumber *STATE_INITIALIZED;
@property (nonatomic,readonly) NSNumber *STATE_STARTING;
@property (nonatomic,readonly) NSNumber *STATE_WAITING_FOR_DATA;
@property (nonatomic,readonly) NSNumber *STATE_WAITING_FOR_QUEUE;
@property (nonatomic,readonly) NSNumber *STATE_PLAYING;
@property (nonatomic,readonly) NSNumber *STATE_BUFFERING;
@property (nonatomic,readonly) NSNumber *STATE_STOPPING;
@property (nonatomic,readonly) NSNumber *STATE_STOPPED;
@property (nonatomic,readonly) NSNumber *STATE_PAUSED;

@end

#endif