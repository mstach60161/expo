import { PermissionResponse, PermissionStatus } from 'expo-modules-core';
export declare enum BrightnessMode {
    /**
     * Means that the current brightness mode cannot be determined.
     */
    UNKNOWN = 0,
    /**
     * Mode in which the device OS will automatically adjust the screen brightness depending on the
     * ambient light.
     */
    AUTOMATIC = 1,
    /**
     * Mode in which the screen brightness will remain constant and will not be adjusted by the OS.
     */
    MANUAL = 2
}
export { PermissionResponse, PermissionStatus };
/**
 * Returns whether the Brightness API is enabled on the current device. This does not check the app
 * permissions.
 * @return Async `boolean`, indicating whether the Brightness API is available on the current device.
 * Currently this resolves `true` on iOS and Android only.
 */
export declare function isAvailableAsync(): Promise<boolean>;
/**
 * Gets the current brightness level of the device's main screen.
 * @return A `Promise` that fulfils with a number between `0` and `1`, inclusive, representing the
 * current screen brightness.
 */
export declare function getBrightnessAsync(): Promise<number>;
/**
 * Sets the current screen brightness. On iOS, this setting will persist until the device is locked,
 * after which the screen brightness will revert to the user's default setting. On Android, this
 * setting only applies to the current activity; it will override the system brightness value
 * whenever your app is in the foreground.
 * @param brightnessValue A number between `0` and `1`, inclusive, representing the desired screen
 * brightness.
 * @return A `Promise` that fulfils when the brightness has been successfully set.
 */
export declare function setBrightnessAsync(brightnessValue: number): Promise<void>;
/**
 * __Android only.__ Gets the global system screen brightness.
 * @return A `Promise` that is resolved with a number between `0` and `1`, inclusive, representing
 * the current system screen brightness.
 */
export declare function getSystemBrightnessAsync(): Promise<number>;
/**
 * > __WARNING:__ This method is experimental.
 *
 * __Android only.__ Sets the global system screen brightness and changes the brightness mode to
 * `MANUAL`. Requires `SYSTEM_BRIGHTNESS` permissions.
 * @param brightnessValue A number between `0` and `1`, inclusive, representing the desired screen
 * brightness.
 * @return A `Promise` that fulfils when the brightness has been successfully set.
 */
export declare function setSystemBrightnessAsync(brightnessValue: number): Promise<void>;
/**
 * __Android only.__ Resets the brightness setting of the current activity to use the system-wide
 * brightness value rather than overriding it.
 * @return A `Promise` that fulfils when the setting has been successfully changed.
 */
export declare function useSystemBrightnessAsync(): Promise<void>;
/**
 * __Android only.__ Returns a boolean specifying whether or not the current activity is using the
 * system-wide brightness value.
 * @return A `Promise` that fulfils with `true` when the current activity is using the system-wide
 * brightness value, and `false` otherwise.
 */
export declare function isUsingSystemBrightnessAsync(): Promise<boolean>;
/**
 * __Android only.__ Gets the system brightness mode (e.g. whether or not the OS will automatically
 * adjust the screen brightness depending on ambient light).
 * @return A `Promise` that fulfils with a [`BrightnessMode`](#brightnessmode). Requires
 * `SYSTEM_BRIGHTNESS` permissions.
 */
export declare function getSystemBrightnessModeAsync(): Promise<BrightnessMode>;
/**
 * __Android only.__ Sets the system brightness mode.
 * @param brightnessMode One of `BrightnessMode.MANUAL` or `BrightnessMode.AUTOMATIC`. The system
 * brightness mode cannot be set to `BrightnessMode.UNKNOWN`.
 */
export declare function setSystemBrightnessModeAsync(brightnessMode: BrightnessMode): Promise<void>;
/**
 * Checks user's permissions for accessing system brightness.
 * @return A promise that fulfils with an object of type [PermissionResponse](#permissionrespons).
 */
export declare function getPermissionsAsync(): Promise<PermissionResponse>;
/**
 * Asks the user to grant permissions for accessing system brightness.
 * @return A promise that fulfils with an object of type [PermissionResponse](#permissionrespons).
 */
export declare function requestPermissionsAsync(): Promise<PermissionResponse>;
