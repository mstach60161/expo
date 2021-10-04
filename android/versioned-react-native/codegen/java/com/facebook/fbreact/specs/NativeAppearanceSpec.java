
/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 *
 * @generated by codegen project: GenerateModuleJavaSpec.js
 *
 * @nolint
 */

package com.facebook.fbreact.specs;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReactModuleWithSpec;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;
import javax.annotation.Nullable;

public abstract class NativeAppearanceSpec extends ReactContextBaseJavaModule implements ReactModuleWithSpec, TurboModule {
  public NativeAppearanceSpec(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public abstract @Nullable String getColorScheme();

  @ReactMethod
  public abstract void addListener(String eventName);

  @ReactMethod
  public abstract void removeListeners(double count);
}
