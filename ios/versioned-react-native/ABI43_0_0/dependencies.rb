# @generated by expotools

require './versioned-react-native/ABI43_0_0/ReactNative/scripts/react_native_pods.rb'

use_react_native_ABI43_0_0! path: './versioned-react-native/ABI43_0_0/ReactNative'

pod 'ABI43_0_0ExpoKit',
  :path => './versioned-react-native/ABI43_0_0/Expo/ExpoKit',
  :project_name => 'ABI43_0_0',
  :subspecs => ['Expo', 'ExpoOptional']

use_pods! '{versioned,vendored}/sdk43/**/*.podspec.json', 'ABI43_0_0'
