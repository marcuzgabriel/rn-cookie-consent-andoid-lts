import 'tsx/cjs';
import { ConfigContext, ExpoConfig } from 'expo/config';

export default ({ config }: ConfigContext): ExpoConfig => ({
  name: 'react-native-mobile-consents-sdk-example',
  slug: 'react-native-mobile-consents-sdk-example',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'automatic',
  newArchEnabled: true,
  splash: {
    image: './assets/splash-icon.png',
    resizeMode: 'contain',
    backgroundColor: '#ffffff',
  },
  ios: {
    supportsTablet: false,
    bundleIdentifier: 'expo.modules.mobileconsentssdk.example',
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon.png',
      backgroundColor: '#ffffff',
    },
    package: 'expo.modules.mobileconsentssdk.example',
  },
  web: {
    favicon: './assets/favicon.png',
  },
  plugins: [
    './src/assets/plugins/withMobileConsentsSDK.ts',
    './src/assets/plugins/withCookieInformationPlist.ts',
    './src/assets/plugins/withCookieInformationProperties.ts',
  ],
});
