// Reexport the native module. On web, it will be resolved to ReactNativeMobileConsentsSdkModule.web.ts
// and on native platforms to ReactNativeMobileConsentsSdkModule.ts
export { default } from './ReactNativeMobileConsentsSdkModule';
// eslint-disable-next-line import/export
export * from './ReactNativeMobileConsentsSdkModule';
