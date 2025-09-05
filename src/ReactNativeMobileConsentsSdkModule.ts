import { NativeModule, requireNativeModule } from 'expo';

interface TrackingConsents {
  marketing: boolean;
  statistical: boolean;
  necessary: boolean;
}

interface ConsentIdMapping {
  uuid: string;
  longId: number;
}

interface ConsentInformation {
  consentItemId: string;
  required: boolean;
  type: string;
  translations: Array<{ [key: string]: any }>;
}

interface SavedUserConsent {
  [key: string]: any;
}

interface AcceptAllConsentsResponse {
  success: boolean;
  message: string;
}

interface SDKStatus {
  initialized: boolean;
  error?: string;
}

declare class ReactNativeMobileConsentsSdkModule extends NativeModule {
  showPrivacyPopUp(): Promise<TrackingConsents>;
  acceptAllConsents(userId?: string): Promise<AcceptAllConsentsResponse>;
  getConsentIds(): Promise<ConsentIdMapping[]>;
  getAllConsentInformation(): Promise<ConsentInformation[]>;
  cacheAndGetLatestSavedConsents(): Promise<SavedUserConsent[]>;
  getSDKStatus(): Promise<SDKStatus>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ReactNativeMobileConsentsSdkModule>(
  'ReactNativeMobileConsentsSdk',
);