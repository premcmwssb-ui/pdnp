# Keep Anthropic SDK + Jackson models if minification is enabled later
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.anthropic.**
-dontwarn com.fasterxml.jackson.**
