package expo.modules.mobileconsentssdk

import android.content.Context
import android.util.Log
import com.cookieinformation.mobileconsents.core.ConsentSDK

class ConsentSDKWrapperDirect(private val context: Context) {

    private val tag = "ConsentSDKWrapper"
    private var coreSDKInstance: ConsentSDK? = null

    fun initCoreSDK(clientID: String, clientSecret: String, solutionId: String, languageCode: String): String {
        Log.d(tag, "Creating Core SDK instance")

        return try {
            coreSDKInstance = ConsentSDK(
                context = context,
                clientID = clientID,
                clientSecret = clientSecret,
                solutionId = solutionId,
                language = languageCode
            )

            Log.d(tag, "Core SDK instance created and stored")
            "Core SDK ready"

        } catch (e: Exception) {
            Log.e(tag, "Error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    data class ConsentIdMapping(
        val uuid: String,
        val longId: Long
    )

    data class ConsentInformation(
        val consentItemId: String,
        val required: Boolean,
        val type: String,
        val translations: List<Map<String, Any?>>
    )

    suspend fun getAllConsentInformation(): Result<List<ConsentInformation>> {
        Log.d(tag, "Fetching all consent information")

        return runCatching {
            // Create a ConsentSDK instance the same way the UI SDK does
            val sdk = com.cookieinformation.mobileconsents.core.ConsentSDK(
                context,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientID,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientSecret,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.solutionId,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.languageCode
            )

            // Initialize SDK
            val initResult = sdk.init()
            if (initResult.isFailure) {
                throw Exception("SDK init failed: ${initResult.exceptionOrNull()?.message}")
            }

            // Access internal API and call fetchConsents to get solution data
            val consentsSDKClass = Class.forName("com.cookieinformation.mobileconsents.core.ConsentsSDK")
            val apiField = consentsSDKClass.getDeclaredField("api")
            apiField.isAccessible = true
            val apiInstance = apiField.get(sdk) ?: throw Exception("API instance is null")

            // Fetch consent definitions from solution
            val fetchMethod = apiInstance.javaClass.declaredMethods.find { it.name.contains("fetchConsents") }
                ?: throw Exception("fetchConsents method not found")
            fetchMethod.isAccessible = true

            val fetchResult = kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
                try {
                    fetchMethod.invoke(apiInstance, sdk.solutionId, continuation)
                } catch (e: Exception) {
                    continuation.resumeWith(kotlin.Result.failure(e))
                }
            }

            if (fetchResult !is kotlin.Result<*> || fetchResult.isFailure) {
                throw Exception("Failed to fetch solution data")
            }

            val solutionResponse = fetchResult.getOrNull() ?: throw Exception("Solution response is null")

            // Get the consent items from the response
            val consentItemsField = solutionResponse.javaClass.getDeclaredField("consentItems")
            consentItemsField.isAccessible = true
            val consentItems = consentItemsField.get(solutionResponse) as? List<*>
                ?: throw Exception("No consent items found")

            // Extract only what exists in ItemResponse
            val consentInfoList = mutableListOf<ConsentInformation>()
            consentItems.forEach { item ->
                try {
                    if (item != null) {
                        // Extract consentItemId
                        val consentItemIdField = item.javaClass.getDeclaredField("consentItemId")
                        consentItemIdField.isAccessible = true
                        val consentItemId = consentItemIdField.get(item) as String

                        // Extract required
                        val requiredField = item.javaClass.getDeclaredField("required")
                        requiredField.isAccessible = true
                        val required = requiredField.get(item) as Boolean

                        // Extract type
                        val typeField = item.javaClass.getDeclaredField("type")
                        typeField.isAccessible = true
                        val type = typeField.get(item) as String

                        // Extract translations
                        val translationsField = item.javaClass.getDeclaredField("translations")
                        translationsField.isAccessible = true
                        val translations = translationsField.get(item) as? List<*> ?: emptyList<Any>()

                        // Convert translations to simple map format
                        val translationMaps = translations.mapNotNull { translation ->
                            if (translation != null) {
                                val translationMap = mutableMapOf<String, Any?>()
                                translation.javaClass.declaredFields.forEach { field ->
                                    try {
                                        field.isAccessible = true
                                        translationMap[field.name] = field.get(translation)
                                    } catch (e: Exception) {
                                        Log.d(tag, "Could not extract field ${field.name}: ${e.message}")
                                    }
                                }
                                translationMap
                            } else null
                        }

                        consentInfoList.add(
                            ConsentInformation(
                                consentItemId = consentItemId,
                                required = required,
                                type = type,
                                translations = translationMaps
                            )
                        )

                        Log.d(tag, "Extracted consent: ID=$consentItemId, Type=$type, Required=$required, Translations=${translationMaps.size}")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error processing consent item: ${e.message}")
                }
            }

            Log.d(tag, "Found ${consentInfoList.size} consent items")
            consentInfoList
        }
    }

    suspend fun getConsentIds(): Result<List<ConsentIdMapping>> {
        Log.d(tag, "Fetching consent IDs from solution using UI SDK values")

        return runCatching {
            // Create a ConsentSDK instance the same way the UI SDK does
            val sdk = com.cookieinformation.mobileconsents.core.ConsentSDK(
                context,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientID,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientSecret,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.solutionId,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.languageCode
            )

            // Initialize SDK
            val initResult = sdk.init()
            if (initResult.isFailure) {
                throw Exception("SDK init failed: ${initResult.exceptionOrNull()?.message}")
            }

            // Access internal API and call fetchConsents
            val consentsSDKClass = Class.forName("com.cookieinformation.mobileconsents.core.ConsentsSDK")
            val apiField = consentsSDKClass.getDeclaredField("api")
            apiField.isAccessible = true
            val apiInstance = apiField.get(sdk) ?: throw Exception("API instance is null")

            // Get the UUIDs from fetchConsents
            val fetchMethod = apiInstance.javaClass.declaredMethods.find { it.name.contains("fetchConsents") }
                ?: throw Exception("fetchConsents method not found")
            fetchMethod.isAccessible = true

            val fetchResult = kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
                try {
                    fetchMethod.invoke(apiInstance, sdk.solutionId, continuation)
                } catch (e: Exception) {
                    continuation.resumeWith(kotlin.Result.failure(e))
                }
            }

            if (fetchResult !is kotlin.Result<*> || fetchResult.isFailure) {
                throw Exception("Failed to fetch solution data")
            }

            val solutionResponse = fetchResult.getOrNull() ?: throw Exception("Solution response is null")

            // Get the UUID strings from the response
            val consentItemsField = solutionResponse.javaClass.getDeclaredField("consentItems")
            consentItemsField.isAccessible = true
            val consentItems = consentItemsField.get(solutionResponse) as? List<*>
                ?: throw Exception("No consent items found")

            val consentMappings = mutableListOf<ConsentIdMapping>()
            consentItems.forEachIndexed { index, item ->
                val idField = item?.javaClass?.getDeclaredField("consentItemId")
                idField?.isAccessible = true
                val uuid = idField?.get(item) as? String
                if (uuid != null) {
                    consentMappings.add(ConsentIdMapping(
                        uuid = uuid,
                        longId = (index + 1).toLong()
                    ))
                }
            }

            Log.d(tag, "Found ${consentMappings.size} consent ID mappings:")
            consentMappings.forEach { mapping ->
                Log.d(tag, "  Long ID: ${mapping.longId} -> UUID: ${mapping.uuid}")
            }

            consentMappings
        }
    }

    suspend fun acceptAllConsents(userId: String?, session: Any?): Result<Unit> {
        Log.d(tag, "Accepting all consents using UI SDK's session")

        return runCatching {
            // Create a ConsentSDK instance the same way the UI SDK does
            val sdk = com.cookieinformation.mobileconsents.core.ConsentSDK(
                context,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientID,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.clientSecret,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.solutionId,
                com.cookieinformation.mobileconsents.sdk.ui.ConsentsUISDK.languageCode
            )

            // Initialize it
            val initResult = sdk.init()
            if (initResult.isFailure) {
                throw Exception("SDK init failed: ${initResult.exceptionOrNull()?.message}")
            }

            // Get consent IDs first
            val consentIdsResult = getConsentIds()
            if (consentIdsResult.isFailure) {
                throw Exception("Failed to get consent IDs: ${consentIdsResult.exceptionOrNull()?.message}")
            }

            val consentMappings = consentIdsResult.getOrThrow()
            val uuidStrings = consentMappings.map { it.uuid }
            Log.d(tag, "Got ${uuidStrings.size} consent UUIDs, creating ProcessingPurposeRequest objects...")

            // Access SDK internals for API call
            val consentsSDKClass = Class.forName("com.cookieinformation.mobileconsents.core.ConsentsSDK")
            val apiField = consentsSDKClass.getDeclaredField("api")
            apiField.isAccessible = true
            val apiInstance = apiField.get(sdk) ?: throw Exception("API instance is null")

            // Get solution version (we need to fetch this again)
            val fetchMethod = apiInstance.javaClass.declaredMethods.find { it.name.contains("fetchConsents") }
                ?: throw Exception("fetchConsents method not found")
            fetchMethod.isAccessible = true

            val fetchResult = kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
                try {
                    fetchMethod.invoke(apiInstance, sdk.solutionId, continuation)
                } catch (e: Exception) {
                    continuation.resumeWith(kotlin.Result.failure(e))
                }
            }

            val solutionResponse = fetchResult as kotlin.Result<*>
            val solution = solutionResponse.getOrNull()
            val versionField = solution?.javaClass?.getDeclaredField("solutionVersion")
            versionField?.isAccessible = true
            val solutionVersion = versionField?.get(solution) as? String
                ?: throw Exception("Failed to get solutionVersion")

            // Create ProcessingPurposeRequest objects for each consent
            val processingPurposeRequestClass = Class.forName("com.cookieinformation.mobileconsents.core.data.network.dto.requests.ProcessingPurposeRequest")
            val constructor = processingPurposeRequestClass.getConstructor(
                Boolean::class.java,
                String::class.java,
                String::class.java
            )

            val handledConsents = uuidStrings.map { consentId ->
                constructor.newInstance(true, "DA", consentId)
            }

            Log.d(tag, "Created ${handledConsents.size} ProcessingPurposeRequest objects")

            // Get applicationName and packageName
            val packageName = context.packageName
            val applicationName = context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(context.packageName, 0)
            ).toString()

            // Debug session to find the userId
            Log.d(tag, "Session object: $session")
            Log.d(tag, "Session class: ${session?.javaClass}")
            Log.d(tag, "Passed userId parameter: $userId")

            // Extract the session ID which is used as userId in API calls
            val sessionIdField = session?.javaClass?.getDeclaredField("id")
            sessionIdField?.isAccessible = true
            val sessionUserId = sessionIdField?.get(session) as? String
                ?: throw Exception("Session ID is null")

            Log.d(tag, "Using session ID as userId: $sessionUserId")

            // Try to find userId field in session
            val sessionFields = session.javaClass.declaredFields
            sessionFields.forEach { field ->
                field.isAccessible = true
                try {
                    val value = field.get(session)
                    Log.d(tag, "Session field: ${field.name} = $value")
                } catch (e: Exception) {
                    Log.d(tag, "Session field: ${field.name} = <error: ${e.message}>")
                }
            }

            // Find acceptConsents method
            val acceptConsentsMethod = apiInstance.javaClass.declaredMethods.find { method ->
                method.name.contains("acceptConsents") || method.name.contains("accept")
            } ?: throw Exception("No accept method found")

            acceptConsentsMethod.isAccessible = true

            // DEBUG: Log method details
            Log.d(tag, "=== METHOD DEBUG ===")
            Log.d(tag, "Calling method: ${acceptConsentsMethod.name}")
            Log.d(tag, "Method class: ${acceptConsentsMethod.declaringClass}")
            Log.d(tag, "Method parameters: ${acceptConsentsMethod.parameterTypes.joinToString()}")

            // DEBUG: Log each ProcessingPurposeRequest object
            Log.d(tag, "=== REQUEST OBJECTS DEBUG ===")
            handledConsents.forEachIndexed { index, request ->
                Log.d(tag, "ProcessingPurposeRequest[$index]: $request")
            }

            // DEBUG: Log all available methods to see alternatives
            Log.d(tag, "=== AVAILABLE METHODS ===")
            apiInstance.javaClass.declaredMethods.forEach { method ->
                if (method.name.contains("consent", ignoreCase = true) ||
                    method.name.contains("accept", ignoreCase = true) ||
                    method.name.contains("reject", ignoreCase = true) ||
                    method.name.contains("save", ignoreCase = true)) {
                    Log.d(tag, "Available method: ${method.name} - ${method.parameterTypes.joinToString()}")
                }
            }

            Log.d(tag, "=== FULL API CALL PARAMETERS ===")
            Log.d(tag, "Session: $session")
            Log.d(tag, "SolutionId: ${sdk.solutionId}")
            Log.d(tag, "SolutionVersion: $solutionVersion")
            Log.d(tag, "HandledConsents count: ${handledConsents.size}")
            Log.d(tag, "PackageName: $packageName")
            Log.d(tag, "ApplicationName: $applicationName")

            // Call acceptConsents API using the session's ID as userId
            val result = kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
                try {
                    acceptConsentsMethod.invoke(
                        apiInstance,
                        session,
                        sdk.solutionId,
                        solutionVersion,
                        handledConsents,
                        packageName,
                        applicationName,
                        continuation
                    )
                } catch (e: Exception) {
                    continuation.resumeWith(kotlin.Result.failure(e))
                }
            }

            if (result is kotlin.Result<*> && result.isFailure) {
                throw Exception("API call failed: ${result.exceptionOrNull()?.message}")
            }

            Log.d(tag, "Successfully accepted all consents via API")

            // Skip local saveConsents since it's causing "invalid state" errors
            // Instead, we'll let the calling code handle UI state updates
        }
    }

    fun getSDK(): ConsentSDK? {
        return coreSDKInstance
    }
}