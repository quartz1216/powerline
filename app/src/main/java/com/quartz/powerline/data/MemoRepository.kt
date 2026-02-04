package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quartz.powerline.domain.models.Memo
import java.util.UUID

class MemoRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("memos", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllMemos(): List<Memo> {
        val json = prefs.getString("memo_list", "[]")
        val type = object : TypeToken<List<Memo>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addMemo(content: String) {
        val currentList = getAllMemos().toMutableList()
        val newMemo = Memo(
            id = UUID.randomUUID().toString(),
            content = content,
            timestamp = System.currentTimeMillis()
        )
        // Add to top
        currentList.add(0, newMemo)
        saveMemos(currentList)
    }

    fun deleteMemos(ids: Set<String>) {
        val currentList = getAllMemos().filter { !ids.contains(it.id) }
        saveMemos(currentList)
    }

    private fun saveMemos(memos: List<Memo>) {
        val json = gson.toJson(memos)
        prefs.edit().putString("memo_list", json).apply()
    }
}
