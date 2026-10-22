# Gson reads and writes these by field name (SharedPreferences "history"); renaming the
# fields would make stored history unreadable after an update.
-keep class dev.quxomiya.powerline.data.HistoryItem { <fields>; }
-keep class dev.quxomiya.powerline.data.UserDataRepository$StoredHistoryItem { <fields>; <init>(...); }

# The Functions settings ("search_engines", "share_targets") are stored the same way
-keep class dev.quxomiya.powerline.search.SearchEngine { <fields>; <init>(...); }
-keep class dev.quxomiya.powerline.search.ShareTarget { <fields>; <init>(...); }
