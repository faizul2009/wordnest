# WordNest Native Android

A Kotlin Android app using **native Android views** (no WebView, no HTML/JavaScript runtime). Includes dictionary lookup from dictionaryapi.dev, manually added words, on-device text-to-speech, searchable saved vocabulary, spaced repetition reviews, and JSON import/export via Android Storage Access Framework. Vocabulary is stored locally in SharedPreferences.

## Build
Open this folder in Android Studio with Android SDK 35 and JDK 17, sync Gradle, and choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Debug APK normally appears at `app/build/outputs/apk/debug/app-debug.apk`.

This source has not been compiled or device-tested in the creation environment. An internet connection is needed for new dictionary lookups. No account, cloud sync, push reminders, or Bangla translation service is included. Backup regularly.

## Build using only your Android phone (GitHub Actions)
1. Create a GitHub account and a new private repository.
2. Upload all project files **including the hidden `.github/workflows/build-apk.yml` path** into the repository, preserving folders. GitHub's web interface may require adding files individually; using a Git client that uploads a directory is easier.
3. Go to the repository's Actions tab, choose **Build WordNest APK**, and select **Run workflow** (or push to main).
4. After a successful run, open the run and download the **WordNest-debug-APK** artifact. Extract its ZIP to find `app-debug.apk`.
5. Install the APK on your phone after checking the app's requested permissions.

This build workflow has not been executed or verified here. The debug APK is for personal testing, not Play Store distribution. Do not upload private vocabulary backups to a public repository.
