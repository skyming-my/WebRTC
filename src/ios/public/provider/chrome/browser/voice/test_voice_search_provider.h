// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_PUBLIC_PROVIDER_CHROME_BROWSER_VOICE_TEST_VOICE_SEARCH_PROVIDER_H_
#define IOS_PUBLIC_PROVIDER_CHROME_BROWSER_VOICE_TEST_VOICE_SEARCH_PROVIDER_H_

#include <memory>

#include "base/macros.h"
#include "ios/public/provider/chrome/browser/voice/voice_search_provider.h"

// Test provider that returns dummy values or test objects.
class TestVoiceSearchProvider : public VoiceSearchProvider {
 public:
  TestVoiceSearchProvider();
  ~TestVoiceSearchProvider() override;

  // VoiceSearchProvider.
  NSArray* GetAvailableLanguages() const override;
  AudioSessionController* GetAudioSessionController() const override;

 private:
  DISALLOW_COPY_AND_ASSIGN(TestVoiceSearchProvider);
};

#endif  // IOS_PUBLIC_PROVIDER_CHROME_BROWSER_VOICE_TEST_VOICE_SEARCH_PROVIDER_H_
