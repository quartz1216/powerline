# Gson reads and writes these by field name (SharedPreferences "history"); renaming the
# fields would make stored history unreadable after an update.
-keep class dev.quxomiya.powerline.data.HistoryItem { <fields>; }
-keep class dev.quxomiya.powerline.data.UserDataRepository$StoredHistoryItem { <fields>; <init>(...); }
