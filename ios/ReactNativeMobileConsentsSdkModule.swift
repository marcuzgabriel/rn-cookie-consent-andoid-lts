import ExpoModulesCore
import Foundation
import MobileConsentsSDK

public class ReactNativeMobileConsentsSdkModule: Module {
  private var mobileConsentsSDK: MobileConsents?
  
  public func definition() -> ModuleDefinition {
    Name("ReactNativeMobileConsentsSdk")

    OnCreate {
      initializeSDK()
    }

    AsyncFunction("showPrivacyPopUp") { (promise: Promise) in
      self.withSDK(promise: promise) { sdk in
        sdk.showPrivacyPopUp { userConsents in
          promise.resolve(userConsents.reduce(into: [:]) { dict, consent in
            dict[consent.consentItem.type.rawValue] = consent.isSelected
          })
        } errorHandler: { error in
          promise.reject("SDK_ERROR", error.localizedDescription)
        }
      }
    }
    
    AsyncFunction("acceptAllConsents") { (promise: Promise) in
      self.withSDK(promise: promise) { sdk in
        sdk.fetchConsentSolution { result in
          guard case .success(let solution) = result else {
            promise.reject("FETCH_ERROR", "Failed to fetch consent solution")
            return
          }
          
          let userConsents = solution.consentItems
            .filter { $0.type != .privacyPolicy }
            .map { UserConsent(consentItem: $0, isSelected: true) }
          
          let consent = Consent(
            consentSolutionId: solution.id,
            consentSolutionVersionId: solution.versionId,
            userConsents: userConsents
          )
          
          sdk.postConsent(consent) { error in
            if let error = error {
              promise.reject("POST_ERROR", error.localizedDescription)
            } else {
              promise.resolve(userConsents.reduce(into: [:]) { dict, consent in
                dict[consent.consentItem.type.rawValue] = consent.isSelected
              })
            }
          }
        }
      }
    }
  }
  
  private func withSDK(promise: Promise, action: @escaping (MobileConsents) -> Void) {
    guard let sdk = mobileConsentsSDK else {
      promise.reject("INIT_ERROR", "SDK not initialized")
      return
    }
    action(sdk)
  }
  
  private func initializeSDK() {
    guard let path = Bundle.main.path(forResource: "CookieInformation", ofType: "plist") else {
      print("❌ CookieInformation.plist not found")
      return
    }
    
    guard let plist = NSDictionary(contentsOfFile: path) else {
      print("❌ Could not read CookieInformation.plist")
      return
    }
    
    mobileConsentsSDK = MobileConsents(
      clientID: plist["clientID"] as? String ?? "",
      clientSecret: plist["clientSecret"] as? String ?? "",
      solutionId: plist["solutionID"] as? String ?? "",
      enableNetworkLogger: plist["enableNetworkLogger"] as? Bool ?? false
    )
    
    print("✅ MobileConsents SDK initialized")
  }
}