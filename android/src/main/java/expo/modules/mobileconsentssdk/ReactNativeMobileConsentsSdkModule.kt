package expo.modules.mobileconsentssdk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.graphics.toColorInt
import com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK
import com.cookieinformation.mobileconsents.sdk.ui.CustomColorScheme
import com.cookieinformation.mobileconsents.core.ConsentSDK
import com.cookieinformation.mobileconsents.sdk.ui.ui.data.ConsentsUiState

import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties

class ReactNativeMobileConsentsSdkModule : Module() {
    private val tag: String = "MobileConsentModule"
    private var reactMainActivity: ComponentActivity? = null
    private var sdkError: String? = null
    private var isSDKInitialized: Boolean = false

    // Add ConsentSDK instance and wrapper
    private var consentSDK: ConsentSDK? = null
    private var consentSDKWrapper: ConsentSDKWrapperDirect? = null

    // Data class for SDK configuration
    private data class SDKConfig(
        val clientID: String,
        val clientSecret: String,
        val solutionID: String
    )

    // Define error codes as CodedException classes
    private class ActivityUnavailableException : CodedException("Activity is not available")
    private class SDKInitException(message: String) : CodedException(message)
    private class ConsentException(message: String) : CodedException(message)
    private class ConsentStatusException(message: String) : CodedException(message)

    override fun definition() = ModuleDefinition {
        Name("ReactNativeMobileConsentsSdk")

        OnCreate {
            reactMainActivity = appContext.activityProvider?.currentActivity as? ComponentActivity
            reactMainActivity?.let { activity ->
                CoroutineScope(Dispatchers.Main).launch {
                    initializeConsentsUISDK(activity)
                    initializeConsentsCoreSDK(activity)
                }
            } ?: run {
                sdkError = "Activity not available"
                Log.w(tag, "Activity not available")
            }
        }

        AsyncFunction("showPrivacyPopUp") { promise: Promise ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    withSDK(promise) { activity ->
                        ConsentsUISDK.showPrivacyPopup(activity).collect { result ->
                            result.fold(
                                onSuccess = { consentItems ->
                                    val consentMap = mutableMapOf<String, Boolean>()
                                    consentItems.forEach { userConsent ->
                                        // Use the actual property names from the SDK
                                        consentMap[userConsent.title] = userConsent.accepted
                                    }
                                    promise.resolve(consentMap)
                                },
                                onFailure = { throwable ->
                                    val errorMsg = throwable.message ?: "Error showing privacy popup"
                                    Log.e(tag, errorMsg, throwable)
                                    promise.reject(ConsentException(errorMsg))
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "Unknown error showing privacy popup"
                    Log.e(tag, errorMsg, e)
                    promise.reject(ConsentException(errorMsg))
                }
            }
        }

        AsyncFunction("acceptAllConsents") { userId: String?, promise: Promise ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activity = reactMainActivity
                    if (activity == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(ActivityUnavailableException())
                        }
                        return@launch
                    }

                    val wrapper = consentSDKWrapper
                    if (wrapper == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(SDKInitException("ConsentSDK wrapper not initialized"))
                        }
                        return@launch
                    }

                    // Get current session
                    val session = getCurrentSession()
                    if (session == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(ConsentException("Unable to get current session"))
                        }
                        return@launch
                    }

                    // Clear UI SDK cache before making changes
                    withContext(Dispatchers.Main) {
                        ConsentsUISDK.deleteLocalConsentsData(activity, userId)
                    }

                    val acceptResult = wrapper.acceptAllConsents(userId, session)

                    acceptResult.fold(
                        onSuccess = {
                            withContext(Dispatchers.Main) {
                                promise.resolve(mapOf(
                                    "success" to true,
                                    "message" to "All consents accepted successfully"
                                ))
                            }
                        },
                        onFailure = { error ->
                            withContext(Dispatchers.Main) {
                                promise.reject(ConsentException(error.message ?: "Failed to accept consents"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        promise.reject(ConsentException(e.message ?: "Unknown error"))
                    }
                }
            }
        }

        AsyncFunction("getConsentIds") { promise: Promise ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val wrapper = consentSDKWrapper
                    if (wrapper == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(SDKInitException("ConsentSDK wrapper not initialized"))
                        }
                        return@launch
                    }

                    val result = wrapper.getConsentIds()
                    result.fold(
                        onSuccess = { consentMappings ->
                            val mappingsList = consentMappings.map { mapping ->
                                mapOf(
                                    "uuid" to mapping.uuid,
                                    "longId" to mapping.longId
                                )
                            }
                            withContext(Dispatchers.Main) {
                                promise.resolve(mappingsList)
                            }
                        },
                        onFailure = { error ->
                            withContext(Dispatchers.Main) {
                                promise.reject(ConsentException(error.message ?: "Failed to get consent IDs"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        promise.reject(ConsentException(e.message ?: "Unknown error getting consent IDs"))
                    }
                }
            }
        }

        AsyncFunction("getAllConsentInformation") { promise: Promise ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val wrapper = consentSDKWrapper
                    if (wrapper == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(SDKInitException("ConsentSDK wrapper not initialized"))
                        }
                        return@launch
                    }

                    val result = wrapper.getAllConsentInformation()
                    result.fold(
                        onSuccess = { consentInfoList ->
                            val consentList = consentInfoList.map { consent ->
                                mapOf(
                                    "consentItemId" to consent.consentItemId,
                                    "required" to consent.required,
                                    "type" to consent.type,
                                    "translations" to consent.translations
                                )
                            }
                            withContext(Dispatchers.Main) {
                                promise.resolve(consentList)
                            }
                        },
                        onFailure = { error ->
                            withContext(Dispatchers.Main) {
                                promise.reject(ConsentException(error.message ?: "Failed to get consent information"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        promise.reject(ConsentException(e.message ?: "Unknown error getting consent information"))
                    }
                }
            }
        }

        AsyncFunction("cacheAndGetLatestSavedConsents") { promise: Promise ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sdk = consentSDK
                    if (sdk == null) {
                        withContext(Dispatchers.Main) {
                            promise.reject(SDKInitException("ConsentSDK not initialized"))
                        }
                        return@launch
                    }

                    // First cache the latest consent solution
                    Log.d(tag, "Caching latest consent solution...")
                    sdk.cacheLatestConsentSolution()

                    // Then get the latest saved user consents
                    Log.d(tag, "Getting latest saved user consents...")
                    val savedConsentsResult = sdk.getLatestSavedUserConsents()

                    savedConsentsResult.fold(
                        onSuccess = { userConsents ->
                            // Convert the user consents to a simple map structure
                            val consentsList = userConsents.map { consent ->
                                val consentMap = mutableMapOf<String, Any?>()

                                // Extract fields using reflection
                                consent.javaClass.declaredFields.forEach { field ->
                                    try {
                                        field.isAccessible = true
                                        val value = field.get(consent)
                                        consentMap[field.name] = value
                                    } catch (e: Exception) {
                                        Log.d(tag, "Could not extract field ${field.name}: ${e.message}")
                                    }
                                }

                                consentMap
                            }

                            Log.d(tag, "Successfully retrieved ${consentsList.size} saved user consents")
                            withContext(Dispatchers.Main) {
                                promise.resolve(consentsList)
                            }
                        },
                        onFailure = { error ->
                            Log.e(tag, "Failed to get saved user consents: ${error.message}")
                            withContext(Dispatchers.Main) {
                                promise.reject(ConsentException(error.message ?: "Failed to get saved user consents"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error in cacheAndGetLatestSavedConsents: ${e.message}")
                    withContext(Dispatchers.Main) {
                        promise.reject(ConsentException(e.message ?: "Unknown error caching and getting saved consents"))
                    }
                }
            }
        }

        // Add a method to check SDK status
        AsyncFunction("getSDKStatus") { promise: Promise ->
            promise.resolve(mapOf(
                "initialized" to isSDKInitialized,
                "error" to sdkError
            ))
        }
    }

    // Helper method to get current session
    private suspend fun getCurrentSession(): Any? {
        return try {
            val sdk = consentSDK ?: return null

            val consentsSDKClass = Class.forName("com.cookieinformation.mobileconsents.core.ConsentsSDK")
            val sessionRepoField = consentsSDKClass.getDeclaredField("sessionRepo")
            sessionRepoField.isAccessible = true
            val sessionRepo = sessionRepoField.get(sdk) ?: return null

            val sessionMethods = sessionRepo.javaClass.declaredMethods
            val getSessionMethod = sessionMethods.find { it.name.startsWith("getSession") } ?: return null
            getSessionMethod.isAccessible = true

            if (getSessionMethod.parameterCount == 0) {
                getSessionMethod.invoke(sessionRepo)
            } else {
                getSessionMethod.invoke(sessionRepo, null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting current session", e)
            null
        }
    }

    // Helper method to ensure SDK is available (matching iOS pattern)
    private fun withSDK(promise: Promise, action: suspend (ComponentActivity) -> Unit) {
        if (reactMainActivity == null) {
            Log.e(tag, "Activity is not available")
            promise.reject(ActivityUnavailableException())
            return
        }

        if (!isSDKInitialized || sdkError != null) {
            Log.e(tag, "SDK not initialized: $sdkError")
            promise.reject(SDKInitException("SDK not initialized: $sdkError"))
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            action(reactMainActivity!!)
        }
    }

    // Load SDK configuration from properties file
    private fun loadSDKConfig(activity: ComponentActivity): SDKConfig? {
        try {
            val properties = Properties()

            try {
                activity.assets.open("CookieInformation.properties").use {
                    properties.load(it)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load properties file", e)
                return null
            }

            val clientID = properties.getProperty("clientID")
            val clientSecret = properties.getProperty("clientSecret")
            val solutionID = properties.getProperty("solutionID")

            if (clientID.isNullOrEmpty() || clientSecret.isNullOrEmpty() || solutionID.isNullOrEmpty()) {
                Log.e(tag, "Missing required configuration properties")
                return null
            }

            return SDKConfig(
                clientID = clientID,
                clientSecret = clientSecret,
                solutionID = solutionID
            )
        } catch (e: Exception) {
            Log.e(tag, "Error loading SDK config", e)
            return null
        }
    }

    private suspend fun initializeConsentsCoreSDK(activity: ComponentActivity) {
        val config = loadSDKConfig(activity)

        if (config == null) {
            sdkError = "Failed to load required configuration"
            isSDKInitialized = false
            return
        }

        try {
            // Initialize Core SDK as per documentation
            consentSDK = ConsentSDK(
                context = activity,
                clientID = config.clientID,
                clientSecret = config.clientSecret,
                solutionId = config.solutionID,
                language = "da"
            )

            // Initialize the wrapper
            consentSDKWrapper = ConsentSDKWrapperDirect(activity)

            consentSDK?.init()?.onSuccess {
                consentSDK?.cacheLatestConsentSolution()
                consentSDK?.getLatestSavedUserConsents()?.onSuccess { consent ->
                    Log.d(tag, "Latest saved user consents: $consent")
                }
                Log.d(tag, "ConsentSDK Core initialized successfully")
            }?.onFailure { error ->
                Log.e(tag, "Failed to initialize ConsentSDK Core", error)
                sdkError = "Core SDK init failed: ${error.message}"
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating ConsentSDK Core", e)
            sdkError = "Core SDK creation failed: ${e.message}"
        }
    }

    // Initialize both SDKs
    private fun initializeConsentsUISDK(activity: ComponentActivity) {
        try {
            val config = loadSDKConfig(activity)

            if (config == null) {
                sdkError = "Failed to load required configuration"
                isSDKInitialized = false
                return
            }

            // Initialize UI SDK (existing code)
            ConsentsUISDK.init(
                clientID = config.clientID,
                clientSecret = config.clientSecret,
                solutionId = config.solutionID,
                context = activity,
                languageCode = "da",
                customLightColorScheme = createLightColorScheme(),
                customDarkColorScheme = createDarkColorScheme(),
            )

            isSDKInitialized = true
            sdkError = null
            Log.d(tag, "ConsentsUISDK initialized successfully")
        } catch (e: Exception) {
            sdkError = e.message ?: "Unknown error during SDK initialization"
            isSDKInitialized = false
            Log.e(tag, "Failed to initialize SDK", e)
        }
    }

    // Light mode color scheme
    private fun createLightColorScheme(): CustomColorScheme {
        return CustomColorScheme(
            primaryColorCode = "#007AFF".toColorInt(), // iOS system blue
            onPrimaryColorCode = "#FFFFFF".toColorInt(),
            primaryContainerColorCode = "#E1F5FE".toColorInt(), // Light blue container
            surfaceColorCode = "#FFFFFF".toColorInt(), // White background
            surfaceVariantColorCode = "#F6F6F6".toColorInt(), // Light gray variant
            onSurfaceColorCode = "#000000".toColorInt(), // Black text on white
            onSurfaceVariantColorCode = "#666666".toColorInt(), // Dark gray text
            outlineColorCode = "#E0E0E0".toColorInt(), // Light outline
            outlineVariantColorCode = "#F0F0F0".toColorInt() // Light outline variant
        )
    }

    // Dark mode color scheme
    private fun createDarkColorScheme(): CustomColorScheme {
        return CustomColorScheme(
            primaryColorCode = "#007AFF".toColorInt(), // iOS system blue
            onPrimaryColorCode = "#FFFFFF".toColorInt(),
            primaryContainerColorCode = "#1B3A57".toColorInt(), // Dark blue container
            surfaceColorCode = "#121212".toColorInt(), // Dark background
            surfaceVariantColorCode = "#1E1E1E".toColorInt(), // Dark variant
            onSurfaceColorCode = "#FFFFFF".toColorInt(), // White text on dark
            onSurfaceVariantColorCode = "#B0B0B0".toColorInt(), // Light gray text
            outlineColorCode = "#404040".toColorInt(), // Dark outline
            outlineVariantColorCode = "#2A2A2A".toColorInt() // Dark outline variant
        )
    }
}