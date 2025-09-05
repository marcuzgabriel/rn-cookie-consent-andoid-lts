## IDE
Visual Studio Code

## Installation
0. cd example/properties change the properties information otherwise the SDK cant be initialized.
1. I use bun -> bun install in root
2. Bun install in example folder
3. cd Example folder -> run in terminal npx expo prebuild --clean
4. from IDE; open -a /Applications/Android\ Studio.app
5. Select the android folder as project and android studio
6. Build and start the project
7. from example folder; bun start to have metro bundler open

Now you can observe the logs in JavaScript (metro bundler) and android studio (Logcat). 

## Implementation
I use cookie consent UISDK and the CoreSDK

## Problem
Expect the same behaviour between iOS and Android when it comes to getting and accepting consents. 

When I try to get consents on Android I just get empty array. When I use the core SDK I can retrieve highlevel info and afterwards accept all consents. I see both the get and post request in logcat but the UI never updates. Why?
