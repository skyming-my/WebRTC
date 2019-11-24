// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/ui/fullscreen/animated_scoped_fullscreen_disabler.h"

#include "base/logging.h"
#import "ios/chrome/browser/ui/fullscreen/fullscreen_controller.h"
#import "ios/chrome/common/material_timing.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

#pragma mark - AnimatedScopedFullscreenDisablerObserverListContainer

// An Objective-C container used to store observers.  This is used to allow
// correct memory management for use in UIView animation blocks.
@interface AnimatedScopedFullscreenDisablerObserverListContainer : NSObject {
  // The AnimatedScopedFullscreenDisablerObservers.
  base::ObserverList<AnimatedScopedFullscreenDisablerObserver>::Unchecked
      observers_;
}

// The disabler passed on initialization.
@property(nonatomic, readonly) AnimatedScopedFullscreenDisabler* disabler;

// Designated initializer for a container containing |disabler|'s observer list.
- (instancetype)initWithDisabler:(AnimatedScopedFullscreenDisabler*)disabler
    NS_DESIGNATED_INITIALIZER;
- (instancetype)init NS_UNAVAILABLE;

// Adds and removes observers.
- (void)addObserver:(AnimatedScopedFullscreenDisablerObserver*)observer;
- (void)removeObserver:(AnimatedScopedFullscreenDisablerObserver*)observer;

// Notifies observers when the animation starts and finishes.
- (void)onAnimationStarted;
- (void)onAnimationFinished;
- (void)onDisablerDestroyed;

@end

@implementation AnimatedScopedFullscreenDisablerObserverListContainer
@synthesize disabler = _disabler;

- (instancetype)initWithDisabler:(AnimatedScopedFullscreenDisabler*)disabler {
  if (self = [super init]) {
    _disabler = disabler;
    DCHECK(_disabler);
  }
  return self;
}

- (const base::ObserverList<
    AnimatedScopedFullscreenDisablerObserver>::Unchecked&)observers {
  return observers_;
}

- (void)addObserver:(AnimatedScopedFullscreenDisablerObserver*)observer {
  observers_.AddObserver(observer);
}

- (void)removeObserver:(AnimatedScopedFullscreenDisablerObserver*)observer {
  observers_.RemoveObserver(observer);
}

- (void)onAnimationStarted {
  for (auto& observer : observers_) {
    observer.FullscreenDisablingAnimationDidStart(_disabler);
  }
}

- (void)onAnimationFinished {
  for (auto& observer : observers_) {
    observer.FullscreenDisablingAnimationDidFinish(_disabler);
  }
}

- (void)onDisablerDestroyed {
  for (auto& observer : observers_) {
    observer.AnimatedFullscreenDisablerDestroyed(_disabler);
  }
}

@end

#pragma mark - AnimatedScopedFullscreenDisabler

AnimatedScopedFullscreenDisabler::AnimatedScopedFullscreenDisabler(
    FullscreenController* controller)
    : controller_(controller) {
  DCHECK(controller_);
  observer_list_container_ =
      [[AnimatedScopedFullscreenDisablerObserverListContainer alloc]
          initWithDisabler:this];
}

AnimatedScopedFullscreenDisabler::~AnimatedScopedFullscreenDisabler() {
  if (disabling_)
    controller_->DecrementDisabledCounter();
  [observer_list_container_ onDisablerDestroyed];
}

void AnimatedScopedFullscreenDisabler::AddObserver(
    AnimatedScopedFullscreenDisablerObserver* observer) {
  [observer_list_container_ addObserver:observer];
}

void AnimatedScopedFullscreenDisabler::RemoveObserver(
    AnimatedScopedFullscreenDisablerObserver* observer) {
  [observer_list_container_ removeObserver:observer];
}

void AnimatedScopedFullscreenDisabler::StartAnimation() {
  // StartAnimation() should be idempotent, so early return if this disabler has
  // already incremented the disabled counter.
  if (disabling_)
    return;
  disabling_ = true;

  if (controller_->IsEnabled()) {
    // Increment the disabled counter in an animation block if the controller is
    // not already disabled.
    [observer_list_container_ onAnimationStarted];
    __weak AnimatedScopedFullscreenDisablerObserverListContainer*
        weak_observer_list_container = observer_list_container_;
    [UIView animateWithDuration:ios::material::kDuration1
        animations:^{
          controller_->IncrementDisabledCounter();
        }
        completion:^(BOOL finished) {
          [weak_observer_list_container onAnimationFinished];
        }];
  } else {
    // If |controller_| is already disabled, no animation is necessary.
    controller_->IncrementDisabledCounter();
  }
}