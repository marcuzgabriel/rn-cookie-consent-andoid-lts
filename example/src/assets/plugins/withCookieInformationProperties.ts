import { withDangerousMod, ConfigPlugin } from '@expo/config-plugins';
import * as fs from 'fs';
import * as path from 'path';

const isDev = process.env.EXPO_PUBLIC_ENVIRONMENT === 'development';

const withCookieInformationProperties: ConfigPlugin = config => {
  return withDangerousMod(config, [
    'android',
    async config => {
      const projectRoot = config.modRequest.projectRoot;
      const androidProjectPath = config.modRequest.platformProjectRoot;

      // Define source and target directories
      const sourceDir = path.join(
        projectRoot,
        'src',
        'assets',
        'plugins',
        'properties',
        'CookieInformation',
      );
      const targetDir = path.join(androidProjectPath, 'app', 'src', 'main', 'assets');

      // Ensure target directory exists
      if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
      }

      // Copy the appropriate properties file
      const sourceFile = path.join(sourceDir, isDev ? 'debug.properties' : 'release.properties');
      const targetFile = path.join(targetDir, 'CookieInformation.properties');

      if (fs.existsSync(sourceFile)) {
        fs.copyFileSync(sourceFile, targetFile);
        console.log(
          `✅ Copied ${isDev ? 'debug' : 'release'}.properties to CookieInformation.properties (${isDev ? 'development' : 'production'} mode)`,
        );
      } else {
        console.warn(`⚠️ ${isDev ? 'debug' : 'release'}.properties not found at ${sourceFile}`);
      }

      return config;
    },
  ]);
};

export default withCookieInformationProperties;
