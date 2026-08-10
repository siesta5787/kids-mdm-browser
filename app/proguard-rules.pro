# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room-generated database implementation classes are referenced only via reflection.
-keep class com.kidsmdm.browser.bookmarks.BookmarkDatabase_Impl { *; }
