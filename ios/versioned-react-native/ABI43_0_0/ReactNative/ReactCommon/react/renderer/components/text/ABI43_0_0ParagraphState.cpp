/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "ABI43_0_0ParagraphState.h"

#include <ABI43_0_0React/ABI43_0_0renderer/components/text/conversions.h>
#include <ABI43_0_0React/ABI43_0_0renderer/debug/debugStringConvertibleUtils.h>

namespace ABI43_0_0facebook {
namespace ABI43_0_0React {

#ifdef ANDROID
folly::dynamic ParagraphState::getDynamic() const {
  return toDynamic(*this);
}
#endif

} // namespace ABI43_0_0React
} // namespace ABI43_0_0facebook
