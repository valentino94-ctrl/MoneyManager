# Money Manager

An Android app by **V94 Studio**.

Money Manager is an Android app for tracking accounts, transactions, budgets,
recurring activity, and saving goals. Financial records are stored locally on
the device.

## Features

- Account and transaction tracking
- Category budgets and spending reports
- Recurring income and expense schedules
- Saving goals
- CSV import and export
- Optional biometric app lock
- Reminders and a home-screen balance widget
- Light and dark themes

## Requirements

- Android 7.0 (API 24) or newer
- Android Studio with JDK 11 or newer

## Build and test

Open the project in Android Studio, allow Gradle to finish syncing, and run the
`app` configuration. From a terminal, the local checks are:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Privacy

Read the [Money Manager Privacy Policy](docs/privacy-policy.md).

Privacy questions can be sent to
[v94studio.apps@gmail.com](mailto:v94studio.apps@gmail.com).

## Project status

Money Manager is being prepared for its first stable release. The public API,
data model, and user experience may change before version 1.0.
