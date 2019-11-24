// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_CHROME_BROWSER_UI_SCANNER_CAMERA_CONTROLLER_H_
#define IOS_CHROME_BROWSER_UI_SCANNER_CAMERA_CONTROLLER_H_

#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>

#include "base/ios/block_types.h"

namespace scanner {

// Values to distinguish between different camera states to display the correct
// view controller or system alert.
// Note: no state encodes the state where the usage of the camera is prohibited
// because the app is in the background. The reason is that iOS transparently
// stops/starts the camera when the app enter/leaves the background.
// See AVCaptureSessionInterruptionReasonVideoDeviceNotAvailableInBackground for
// more information.
enum CameraState {
  // Camera is loaded and available;
  CAMERA_AVAILABLE = 0,
  // The application cannot use the camera because it is in use exclusively by
  // another application.
  CAMERA_IN_USE_BY_ANOTHER_APPLICATION,
  // The application cannot use the camera because video input is not supported
  // if there are multiple foreground apps running.
  MULTIPLE_FOREGROUND_APPS,
  // The application does not have the permission to use the camera.
  CAMERA_PERMISSION_DENIED,
  // Camera unavailable due to "system pressure".
  CAMERA_UNAVAILABLE_DUE_TO_SYSTEM_PRESSURE,
  // The camera is unavailable for an unspecified reason.
  CAMERA_UNAVAILABLE,
  // The camera was not yet loaded.
  CAMERA_NOT_LOADED,
};

}  // namespace scanner

@protocol CameraControllerDelegate

// Called on the main queue when the capture session is connected to the
// preview.
- (void)captureSessionIsConnected;
// Called on the main queue when the camera state changes.
- (void)cameraStateChanged:(scanner::CameraState)state;
// Called on the main queue when the torch state changes.
- (void)torchStateChanged:(BOOL)torchIsOn;
// Called on the main queue when the torch availability changes.
- (void)torchAvailabilityChanged:(BOOL)torchIsAvailable;

@end

// The CameraController manages the AVCaptureSession, its inputs, outputs, and
// notifications for the ScannerViewController.
@interface CameraController : NSObject

// The current state of the torch.
@property(nonatomic, readonly, assign, getter=isTorchActive) BOOL torchActive;

// Initializes the controller with the |delegate|.
- (instancetype)initWithDelegate:(id<CameraControllerDelegate>)delegate
    NS_DESIGNATED_INITIALIZER;

- (instancetype)init NS_UNAVAILABLE;

// Returns the app's authorization in regards to the camera.
- (AVAuthorizationStatus)getAuthorizationStatus;

// Asks the user to grant the authorization to access the camera.
// Should only be called when the current authorization status is
// AVAuthorizationStatusNotDetermined.
- (void)requestAuthorizationAndLoadCaptureSession:
    (AVCaptureVideoPreviewLayer*)previewLayer;

// Loads the camera and sets the value of |cameraState|.
// Should only be called when the current authorization status is
// AVAuthorizationStatusAuthorized.
- (void)loadCaptureSession:(AVCaptureVideoPreviewLayer*)previewLayer;

// Sets the rectangle in which codes are recognized to |viewportRect|. If the
// metadata output object is not loaded, |viewportRect| will be set when the
// output loads.
- (void)setViewport:(CGRect)viewportRect;

// Resets the video orientation to the current interface orientation.
- (void)resetVideoOrientation:(AVCaptureVideoPreviewLayer*)previewLayer;

// Starts the camera capture session. Does nothing if the camera is not
// available.
- (void)startRecording;

// Stops the camera capture session. Does nothing if the camera is not
// available.
- (void)stopRecording;

// Sets the camera's torch mode to |mode|. Does nothing if the camera is not
// available or the torch mode is not supported.
- (void)setTorchMode:(AVCaptureTorchMode)mode;

@end

@interface CameraController (Subclassing)

// The queue for dispatching calls to |_captureSession|.
@property(nonatomic, readonly) dispatch_queue_t sessionQueue;

// The capture session for recording video and detecting QR codes or credit
// cards.
@property(nonatomic, readwrite) AVCaptureSession* captureSession;

// The metadata output attached to the capture session.
@property(nonatomic, readwrite) AVCaptureMetadataOutput* metadataOutput;

// Set camera state.
- (void)setCameraState:(scanner::CameraState)state;

// Configures the scanner specific capture session elements, i.e. either QR code
// or credit card scanner. Must be overridden in the subclass.
- (void)configureScannerWithSession:(AVCaptureSession*)session;

@end

#endif  // IOS_CHROME_BROWSER_UI_SCANNER_CAMERA_CONTROLLER_H_
