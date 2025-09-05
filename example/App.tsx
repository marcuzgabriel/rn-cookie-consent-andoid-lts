import { View, StyleSheet, Button, ScrollView, Alert, Text } from 'react-native';
import MobileConsent from '@topdanmark/mobile-insurance-consents';
import React, { useState } from 'react';

const { 
  showPrivacyPopUp, 
  acceptAllConsents, 
  getConsentIds, 
  getAllConsentInformation,
  cacheAndGetLatestSavedConsents,
  getSDKStatus 
} = MobileConsent;

export default function App() {
  const [consentInfo, setConsentInfo] = useState<string>('');

  const handleShowPrivacyPopup = async () => {
    try {
      const result = await showPrivacyPopUp();
      console.log('Privacy popup result:', result);
      Alert.alert('Success', 'Privacy popup completed');
    } catch (e) {
      console.error('Error showing privacy popup:', e);
      Alert.alert('Error', `Failed to show privacy popup: ${e}`);
    }
  };

  const handleAcceptAll = async () => {
    try {
      const result = await acceptAllConsents();
      console.log('Accept all result:', result);
      Alert.alert('Success', result.message || 'All consents accepted');
    } catch (e) {
      console.error('Error accepting consents:', e);
      Alert.alert('Error', `Failed to accept consents: ${e}`);
    }
  };

  const handleGetConsentIds = async () => {
    try {
      const consentIds = await getConsentIds();
      console.log('Consent IDs:', consentIds);
      
      const idsText = consentIds.map(id => 
        `ID: ${id.longId} - UUID: ${id.uuid.substring(0, 8)}...`
      ).join('\n');
      
      setConsentInfo(`Consent IDs:\n${idsText}`);
      Alert.alert('Consent IDs', `Found ${consentIds.length} consent IDs`);
    } catch (e) {
      console.error('Error getting consent IDs:', e);
      Alert.alert('Error', `Failed to get consent IDs: ${e}`);
    }
  };

  const handleGetAllConsentInfo = async () => {
    try {
      const allInfo = await getAllConsentInformation();
      console.log('All consent information:', allInfo);
      
      // Updated to use the actual fields from ConsentInformation
      const infoText = allInfo.map((info, index) => {
        // Try to get a name from translations if available
        let displayName = info.consentItemId;
        if (info.translations && info.translations.length > 0) {
          const firstTranslation = info.translations[0];
          const name = firstTranslation.name || firstTranslation.title;
          if (name) {
            displayName = name;
          }
        }
        
        return `${index + 1}. ${displayName}\n   Type: ${info.type}\n   Required: ${info.required ? 'Yes' : 'No'}\n   ID: ${info.consentItemId.substring(0, 8)}...`;
      }).join('\n\n');
      
      setConsentInfo(`Consent Information:\n${infoText}`);
      Alert.alert('Consent Information', `Found ${allInfo.length} consents`);
    } catch (e) {
      console.error('Error getting consent information:', e);
      Alert.alert('Error', `Failed to get consent information: ${e}`);
    }
  };

  const handleCacheAndGetSaved = async () => {
    try {
      const savedConsents = await cacheAndGetLatestSavedConsents();
      console.log('Saved consents:', savedConsents);
      
      const savedText = savedConsents.map((consent, index) => {
        // Extract key fields from the saved consent object
        const fields = Object.entries(consent)
          .filter(([key, value]) => value !== null && value !== undefined)
          .slice(0, 3) // Show first 3 non-null fields
          .map(([key, value]) => `${key}: ${String(value).substring(0, 20)}${String(value).length > 20 ? '...' : ''}`)
          .join('\n   ');
        
        return `${index + 1}. Saved Consent:\n   ${fields}`;
      }).join('\n\n');
      
      setConsentInfo(`Saved Consents:\n${savedText}`);
      Alert.alert('Saved Consents', `Found ${savedConsents.length} saved consents`);
    } catch (e) {
      console.error('Error getting saved consents:', e);
      Alert.alert('Error', `Failed to get saved consents: ${e}`);
    }
  };

  const handleCheckSDKStatus = async () => {
    try {
      const status = await getSDKStatus();
      console.log('SDK Status:', status);
      
      const statusMessage = status.initialized 
        ? 'SDK is initialized and ready' 
        : `SDK not initialized${status.error ? `: ${status.error}` : ''}`;
      
      Alert.alert('SDK Status', statusMessage);
    } catch (e) {
      console.error('Error checking SDK status:', e);
      Alert.alert('Error', `Failed to check SDK status: ${e}`);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      <Button
        title='Show Cookie Consent'
        onPress={handleShowPrivacyPopup}
      />
      
      <View style={styles.buttonSpacing} />
      
      <Button
        title='Accept All Consents'
        onPress={handleAcceptAll}
      />
      
      <View style={styles.buttonSpacing} />
      
      <Button
        title='Get Consent IDs'
        onPress={handleGetConsentIds}
      />
      
      <View style={styles.buttonSpacing} />
      
      <Button
        title='Get All Consent Information'
        onPress={handleGetAllConsentInfo}
      />
      
      <View style={styles.buttonSpacing} />
      
      <Button
        title='Cache & Get Saved Consents'
        onPress={handleCacheAndGetSaved}
      />
      
      <View style={styles.buttonSpacing} />
      
      <Button
        title='Check SDK Status'
        onPress={handleCheckSDKStatus}
      />
      
      {consentInfo ? (
        <>
          <View style={styles.buttonSpacing} />
          <Text style={styles.infoText}>{consentInfo}</Text>
        </>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  contentContainer: {
    justifyContent: 'center',
    padding: 20,
    paddingTop: 60,
  },
  buttonSpacing: {
    height: 15,
  },
  infoText: {
    marginTop: 20,
    padding: 15,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    fontSize: 12,
    fontFamily: 'monospace',
    lineHeight: 16,
  },
});