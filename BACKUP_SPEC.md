# Settings Backup Specification

This document describes the JSON format used for importing and exporting settings in the Command Launcher app.

## File Format

The backup file is a standard JSON file.

## Schema

### Root Object

| Field | Type | Description |
|---|---|---|
| `version` | Integer | The version of the backup schema. Current version: `1`. |
| `timestamp` | Long | The timestamp (milliseconds since epoch) when the backup was created. |
| `hiddenApps` | Array<String> | A list of package names that are hidden from the launcher. |
| `aliases` | Object | A key-value map where the key is the alias name and the value is the target package name. |
| `maxRecentItems` | Integer | The maximum number of recent items to display. |
| `history` | Array<Object> | The usage history. |
| `statusBarTapEnabled` | Boolean | Whether the status bar tap trigger is enabled. |
| `statusBarTriggerWidthPercent` | Integer | The width percentage (0-100) of the status bar trigger area, aligned to the right. |
| `bottomTriggerEnabled` | Boolean | Whether the bottom corners trigger is enabled. |
| `bottomTriggerCenterGapPercent` | Integer | The percentage of the bottom screen width to leave as a gap in the center. |
| `bottomTriggerHeightDp` | Integer | The height of the bottom trigger area in dp. 0 means auto-detect. |
| `overlayCloseOnBackgroundTap` | Boolean | Whether to close the overlay when tapping the background. |

### History Item Object

| Field | Type | Description |
|---|---|---|
| `id` | String | The identifier of the item (e.g., package name). |
| `type` | String | The type of the item (`APP`, `WEB`, `CALC`). |
| `title` | String | The display title of the item. |
| `timestamp` | Long | The timestamp when the item was last accessed. |

## Example

```json
{
  "version": 1,
  "timestamp": 1715600000000,
  "hiddenApps": [
    "com.android.chrome",
    "com.google.android.youtube"
  ],
  "aliases": {
    "maps": "com.google.android.apps.maps",
    "mail": "com.google.android.gm"
  },
  "maxRecentItems": 5,
  "history": [
    {
      "id": "com.whatsapp",
      "type": "APP",
      "title": "WhatsApp",
      "timestamp": 1715599900000
    }
  ],
  "statusBarTapEnabled": false,
  "statusBarTriggerWidthPercent": 30,
  "bottomTriggerEnabled": false,
  "bottomTriggerCenterGapPercent": 40,
  "bottomTriggerHeightDp": 0,
  "overlayCloseOnBackgroundTap": true
}
```
