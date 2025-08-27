# TrackAsia Demo Android Project - Cursor Rules

This document explains the Cursor rules set up for this project to enhance the development experience.

## Cursor Rules Configuration

The `.cursorrules` file is configured to optimize your development workflow with the following features:

### Code Quality & Style

- **Auto Import References**: Automatically adds required imports when adding new classes or functions
- **Type Checking Mode**: Set to "strict" to catch potential type-related errors early
- **Format On Save**: Automatically formats code when saving files
- **Remove Unused Imports**: Keeps code clean by removing imports that aren't used
- **Max Line Length**: Set to 120 characters for good readability
- **Indent Size**: Set to 4 spaces (standard for Kotlin/Android)
- **File Header**: Adds a standardized header to new files

### Build & Compatibility

- **Kotlin Version**: Set to 1.8 to align with project requirements
- **Android Studio Integration**: Enabled for better tooling compatibility
- **Java Compile Options**: Set source and target compatibility to Java 1.8

### Performance & Focus

- **Exclude Patterns**: Configuration excludes build artifacts, IDE files, and system files to keep search results relevant
- **Kotlin Experimental APIs**: Allowed for newer Kotlin features
- **Kotlin Style Guide**: Enforces Kotlin style conventions
- **Check Nullability**: Helps prevent null pointer exceptions

### Productivity Features

- **Quick Actions**: Configured for common tasks:
  - Format Document
  - Organize Imports
  - Run Current File

### Code Snippets

Custom snippets are available for frequently used patterns:

- **log**: Insert a Log.d() statement
- **activity**: Create a new Activity class
- **fragment**: Create a new Fragment class
- **trackmap**: Set up a basic TrackAsiaSample implementation

## Using Snippets

Type the snippet name and press Tab to expand. For example:
- Type `log` and press Tab to create `Log.d("TAG", "message")`
- Type `fragment` and press Tab to create a new Fragment template

## Excluded Directories

The following directories are excluded from searches to improve performance:
- build directories
- .gradle directories
- .idea directories
- .history
- keystore directories
- local.properties files
- .DS_Store files
