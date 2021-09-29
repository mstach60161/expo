package expo.modules.facebook

import expo.modules.core.ExportedModule
import com.facebook.CallbackManager
import expo.modules.core.ModuleRegistry
import com.facebook.appevents.AppEventsLogger
import com.facebook.internal.AttributionIdentifiers
import expo.modules.core.interfaces.ExpoMethod
import com.facebook.FacebookSdk
import com.facebook.AccessToken
import android.os.Bundle
import expo.modules.core.arguments.ReadableArguments
import com.facebook.login.LoginManager
import com.facebook.login.LoginBehavior
import com.facebook.FacebookCallback
import com.facebook.login.LoginResult
import com.facebook.FacebookException
import android.app.Activity
import android.content.Context
import android.content.Intent
import expo.modules.core.ModuleRegistryDelegate
import expo.modules.core.Promise
import expo.modules.core.interfaces.ActivityEventListener
import expo.modules.core.interfaces.ActivityProvider
import expo.modules.core.interfaces.services.UIManager
import java.lang.Exception
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.util.*

class FacebookModule(
  context: Context,
  private val moduleRegistryDelegate: ModuleRegistryDelegate = ModuleRegistryDelegate()
) : ExportedModule(context), ActivityEventListener {
  private val callbackManager: CallbackManager = CallbackManager.Factory.create()
  private var appEventLogger: AppEventsLogger? = null
  private var attributionIdentifiers: AttributionIdentifiers? = null
  private var appId: String? = null
  private var appName: String? = null
  private val uIManager: UIManager by moduleRegistry()

  companion object {
    private const val ERR_FACEBOOK_MISCONFIGURED = "ERR_FACEBOOK_MISCONFIGURED"
    private const val ERR_FACEBOOK_LOGIN = "ERR_FACEBOOK_LOGIN"
    private const val PUSH_PAYLOAD_KEY = "fb_push_payload"
    private const val PUSH_PAYLOAD_CAMPAIGN_KEY = "campaign"
  }

  private inline fun <reified T> moduleRegistry() = moduleRegistryDelegate.getFromModuleRegistry<T>()

  override fun getName(): String {
    return "ExponentFacebook"
  }

  override fun onCreate(moduleRegistry: ModuleRegistry) {
    moduleRegistryDelegate.onCreate(moduleRegistry)
    uIManager.registerActivityEventListener(this)
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent) {
    callbackManager.onActivityResult(requestCode, resultCode, data)
  }

  override fun onNewIntent(intent: Intent) {
    // do nothing
  }

  private fun bundleWithNullValuesAsStrings(parameters: ReadableArguments?): Bundle {
    return Bundle().apply {
      if (parameters != null) {
        for (key in parameters.keys()) {
          val value = parameters[key]
          if (value == null) {
            putString(key, "null")
          } else if (value is String) {
            putString(key, value)
          } else if (value is Int) {
            putInt(key, value)
          } else if (value is Double) {
            putDouble(key, value)
          } else if (value is Long) {
            putLong(key, value)
          }
        }
      }
    }
  }

  @ExpoMethod
  fun setAutoLogAppEventsEnabledAsync(enabled: Boolean, promise: Promise) {
    FacebookSdk.setAutoLogAppEventsEnabled(enabled)
    promise.resolve(null)
  }

  @ExpoMethod
  fun setAdvertiserIDCollectionEnabledAsync(enabled: Boolean, promise: Promise) {
    FacebookSdk.setAdvertiserIDCollectionEnabled(enabled)
    promise.resolve(null)
  }

  @ExpoMethod
  fun getAuthenticationCredentialAsync(promise: Promise) {
    val accessToken = AccessToken.getCurrentAccessToken()
    promise.resolve(accessTokenToBundle(accessToken))
  }

  private fun accessTokenToBundle(accessToken: AccessToken?): Bundle? {
    return if (accessToken != null) {
      Bundle().apply {
        putString("token", accessToken.token)
        putString("userId", accessToken.userId)
        putString("appId", accessToken.applicationId)
        putStringArrayList("permissions", ArrayList(accessToken.permissions))
        putStringArrayList("declinedPermissions", ArrayList(accessToken.declinedPermissions))
        putStringArrayList("expiredPermissions", ArrayList(accessToken.expiredPermissions))
        putDouble("expirationDate", accessToken.expires.time.toDouble())
        putDouble("dataAccessExpirationDate", accessToken.dataAccessExpirationTime.time.toDouble())
        putDouble("refreshDate", accessToken.lastRefresh.time.toDouble())
        putString("tokenSource", accessToken.source.name)
      }
    } else {
      null
    }
  }

  @ExpoMethod
  fun initializeAsync(options: ReadableArguments, promise: Promise) {
    try {
      options.getString("appId")?.let {
        appId = it
        FacebookSdk.setApplicationId(it)
      }
      if (options.containsKey("appName")) {
        appName = options.getString("appName")
        FacebookSdk.setApplicationName(appName)
      }
      if (options.containsKey("version")) {
        FacebookSdk.setGraphApiVersion(options.getString("version"))
      }
      if (options.containsKey("autoLogAppEvents")) {
        val autoLogAppEvents = options.getBoolean("autoLogAppEvents")
        FacebookSdk.setAutoLogAppEventsEnabled(autoLogAppEvents)
      }
      if (options.containsKey("domain")) {
        FacebookSdk.setFacebookDomain(options.getString("domain"))
      }
      if (options.containsKey("isDebugEnabled")) {
        FacebookSdk.setIsDebugEnabled(options.getBoolean("isDebugEnabled"))
      }
      FacebookSdk.sdkInitialize(context) {
        FacebookSdk.fullyInitialize()
        appId = FacebookSdk.getApplicationId()
        appName = FacebookSdk.getApplicationName()
        appEventLogger = AppEventsLogger.newLogger(context)
        attributionIdentifiers = AttributionIdentifiers.getAttributionIdentifiers(context)
        promise.resolve(null)
      }
    } catch (e: Exception) {
      promise.reject(ERR_FACEBOOK_MISCONFIGURED, "An error occurred while trying to initialize a FBSDK app", e)
    }
  }

  @ExpoMethod
  fun logOutAsync(promise: Promise) {
    AccessToken.setCurrentAccessToken(null)
    LoginManager.getInstance().logOut()
    promise.resolve(null)
  }

  @ExpoMethod
  fun logInWithReadPermissionsAsync(config: ReadableArguments, promise: Promise) {
    if (FacebookSdk.getApplicationId() == null) {
      promise.reject(
        ERR_FACEBOOK_MISCONFIGURED,
        "No appId configured, required for initialization. " +
          "Please ensure that you're either providing `appId` to `initializeAsync` as an argument or inside AndroidManifest.xml."
      )
    }

    // Log out
    AccessToken.setCurrentAccessToken(null)
    LoginManager.getInstance().logOut()

    // Convert permissions
    val permissions = config.getList("permissions", listOf("public_profile", "email")) as List<String?>
    if (config.containsKey("behavior")) {
      var behavior = LoginBehavior.NATIVE_WITH_FALLBACK
      when (config.getString("behavior")) {
        "browser" -> behavior = LoginBehavior.WEB_ONLY
        "web" -> behavior = LoginBehavior.WEB_VIEW_ONLY
      }
      LoginManager.getInstance().loginBehavior = behavior
    }
    LoginManager.getInstance().registerCallback(
      callbackManager,
      object : FacebookCallback<LoginResult> {
        override fun onSuccess(loginResult: LoginResult) {
          LoginManager.getInstance().registerCallback(callbackManager, null)

          // This is the only route through which we send an access token back. Make sure the
          // application ID is correct.
          if (appId != loginResult.accessToken.applicationId) {
            promise.reject(IllegalStateException("Logged into wrong app, try again?"))
            return
          }
          val response = accessTokenToBundle(loginResult.accessToken)
          response?.putString("type", "success")
          promise.resolve(response)
        }

        override fun onCancel() {
          LoginManager.getInstance().registerCallback(callbackManager, null)
          promise.resolve(
            Bundle().apply {
              putString("type", "cancel")
            }
          )
        }

        override fun onError(error: FacebookException) {
          LoginManager.getInstance().registerCallback(callbackManager, null)
          promise.reject(ERR_FACEBOOK_LOGIN, "An error occurred while trying to log in to Facebook", error)
        }
      }
    )
    try {
      val activityProvider: ActivityProvider by moduleRegistry()
      LoginManager.getInstance().logInWithReadPermissions(activityProvider.currentActivity, permissions)
    } catch (e: FacebookException) {
      promise.reject(ERR_FACEBOOK_LOGIN, "An error occurred while trying to log in to Facebook", e)
    }
  }

  @ExpoMethod
  fun setFlushBehaviorAsync(flushBehavior: String, promise: Promise) {
    AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.valueOf(flushBehavior.toUpperCase(Locale.ROOT)))
    promise.resolve(null)
  }

  @ExpoMethod
  fun logEventAsync(eventName: String?, valueToSum: Double, parameters: ReadableArguments?, promise: Promise) {
    appEventLogger!!.logEvent(eventName, valueToSum, bundleWithNullValuesAsStrings(parameters))
    promise.resolve(null)
  }

  @ExpoMethod
  fun logPurchaseAsync(
    purchaseAmount: Double,
    currencyCode: String?,
    parameters: ReadableArguments?,
    promise: Promise
  ) {
    try {
      appEventLogger!!.logPurchase(
        BigDecimal.valueOf(purchaseAmount),
        Currency.getInstance(currencyCode),
        bundleWithNullValuesAsStrings(parameters)
      )
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_APP_EVENT_LOGGER", "appEventLogger is not initialized", e)
    }
  }

  @ExpoMethod
  fun logPushNotificationOpenAsync(campaign: String?, promise: Promise) {
    // the Android FBSDK expects the fb_push_payload to be a JSON string
    val payload = Bundle()
    payload.putString(PUSH_PAYLOAD_KEY, String.format("{\"%s\" : \"%s\"}", PUSH_PAYLOAD_CAMPAIGN_KEY, campaign))
    try {
      appEventLogger!!.logPushNotificationOpen(payload)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_APP_EVENT_LOGGER", "appEventLogger is not initialized", e)
    }
  }

  @ExpoMethod
  fun setUserIDAsync(userID: String?, promise: Promise) {
    AppEventsLogger.setUserID(userID)
    promise.resolve(null)
  }

  @ExpoMethod
  fun getUserIDAsync(promise: Promise) {
    promise.resolve(AppEventsLogger.getUserID())
  }

  @ExpoMethod
  fun getAnonymousIDAsync(promise: Promise) {
    try {
      promise.resolve(AppEventsLogger.getAnonymousAppDeviceGUID(context))
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_ANONYMOUS_ID", "Can not get anonymousID", e)
    }
  }

  @ExpoMethod
  fun getAdvertiserIDAsync(promise: Promise) {
    try {
      promise.resolve(attributionIdentifiers!!.androidAdvertiserId)
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_ADVERTISER_ID", "Can not get advertiserID", e)
    }
  }

  @ExpoMethod
  fun getAttributionIDAsync(promise: Promise) {
    try {
      promise.resolve(attributionIdentifiers!!.attributionId)
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_ADVERTISER_ID", "Can not get attributionID", e)
    }
  }

  @ExpoMethod
  fun setUserDataAsync(userData: ReadableArguments, promise: Promise) {
    AppEventsLogger.setUserData(
      userData.getString("email"),
      userData.getString("firstName"),
      userData.getString("lastName"),
      userData.getString("phone"),
      userData.getString("dateOfBirth"),
      userData.getString("gender"),
      userData.getString("city"),
      userData.getString("state"),
      userData.getString("zip"),
      userData.getString("country")
    )
    promise.resolve(null)
  }

  @ExpoMethod
  fun flushAsync(promise: Promise) {
    try {
      appEventLogger!!.flush()
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("ERR_FACEBOOK_APP_EVENT_LOGGER", "appEventLogger is not initialized", e)
    }
  }
}
