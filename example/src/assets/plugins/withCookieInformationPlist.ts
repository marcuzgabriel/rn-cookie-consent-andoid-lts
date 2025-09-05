import { withDangerousMod, withXcodeProject, ConfigPlugin, IOSConfig } from '@expo/config-plugins';
import * as fs from 'fs';
import * as path from 'path';

const isDev = process.env.EXPO_PUBLIC_ENVIRONMENT === 'development';

const withCookieInformationPlist: ConfigPlugin = config => {
  // Copy files
  config = withDangerousMod(config, [
    'ios',
    async config => {
      const projectRoot = config.modRequest.projectRoot;
      const iosProjectPath = config.modRequest.platformProjectRoot;
      const projectName = config.modRequest.projectName;

      if (!projectName) {
        throw new Error('Could not determine iOS project name');
      }

      const sourceDir = path.join(
        projectRoot,
        'src',
        'assets',
        'plugins',
        'plists',
        'CookieInformation',
      );
      const targetDir = path.join(iosProjectPath, projectName);

      if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
      }

      const sourceFile = path.join(sourceDir, isDev ? 'debug.plist' : 'release.plist');
      const targetFile = path.join(targetDir, 'CookieInformation.plist');

      if (fs.existsSync(sourceFile)) {
        fs.copyFileSync(sourceFile, targetFile);
        console.log(
          `✅ Copied ${isDev ? 'debug' : 'release'}.plist to CookieInformation.plist (${isDev ? 'development' : 'production'} mode)`,
        );
      } else {
        console.warn(`⚠️ ${isDev ? 'debug' : 'release'}.plist not found at ${sourceFile}`);
      }

      return config;
    },
  ]);

  // Register in Xcode project using the proper Expo utility
  return withXcodeProject(config, config => {
    const project = config.modResults;
    const projectName = config.modRequest?.projectName;

    try {
      // Register the single plist file
      IOSConfig.XcodeUtils.addResourceFileToGroup({
        filepath: `${projectName}/CookieInformation.plist`,
        groupName: 'Resources',
        project,
        isBuildFile: true,
        verbose: true,
      });

      console.log(`✅ CookieInformation.plist registered in Xcode project`);
    } catch (error) {
      console.log(`Error details:`, error.message);
      console.log('File copied but could not register in Xcode project');
    }

    return config;
  });
};

export default withCookieInformationPlist;
