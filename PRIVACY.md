# Powerline Privacy Policy

Powerline is a keyboard launcher for Android. Press Alt+Space on a physical keyboard and a search panel opens over whatever you are doing.

## Summary

- Powerline does not have the `INTERNET` permission. It cannot send anything anywhere.
- It does not read screen content and does not record keystrokes.
- The only data it keeps is your Powerline launch history, favorite apps and search settings, stored on the device.
- It shows no ads and contains no analytics or tracking SDKs.

## Accessibility service

Android only lets an app catch a key combination like Alt+Space from anywhere through an accessibility service. Powerline uses one for exactly two things:

1. **Detecting Alt+Space.** The service receives hardware key events so it can recognise Alt+Space. Every other key is passed straight through. Keys are not logged, stored or sent anywhere.
2. **Closing the panel when you switch apps.** The service receives window-change events and looks only at the package name of the app that came to the front. If it is not Powerline, the system UI or your keyboard, the panel closes.

The service is configured with `canRetrieveWindowContent="false"`, so Android does not let it read text or other content on screen.

The search panel is drawn by this service as an accessibility overlay window.

## Data stored on the device

Powerline keeps these in its private app storage:

| Data | Contents | Why |
|---|---|---|
| Launch history | Package name, launcher activity and time of the last 100 apps you opened from Powerline | "Recent apps" row and ranking of frequently used apps |
| Favorite apps | Package names you marked as favorite | Ranking |
| Search settings | Built-in switches, your search engines and share aliases, and whether app results show package names | Your choices on the status screen |

Searches, calculator input, web queries and shared text are not saved.

Android Auto Backup may include this data in your device backup (your Google account backup or a device-to-device transfer), as it does for most apps. Uninstalling Powerline deletes it.

## Installed apps

Powerline uses the `QUERY_ALL_PACKAGES` permission to list your installed apps so they can be searched and launched, and to show the icon of the app that opens each search engine. The list stays on the device.

## Web search, sharing and copying

- **Web search and URLs**: nothing is sent while you type. Only when you choose a web result does Powerline ask Android to open the URL. That app (your browser, YouTube and so on) then handles it under its own privacy policy.
- **Share**: choosing "Share text" opens the Android share sheet with your text. The app you pick receives it.
- **Calculator**: choosing a result copies it to the clipboard.

## Contact

Questions or problems: open an issue at https://github.com/quartz1216/powerline/issues
