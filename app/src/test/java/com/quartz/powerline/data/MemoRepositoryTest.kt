package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.quartz.powerline.domain.models.Memo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class MemoRepositoryTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var editor: SharedPreferences.Editor

    private lateinit var memoRepository: MemoRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSharedPreferences("memos", Context.MODE_PRIVATE)).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        // Mock default empty list behavior
        `when`(sharedPreferences.getString("memo_list", "[]")).thenReturn("[]")

        memoRepository = MemoRepository(context)
    }

    @Test
    fun testAddMemo() {
        memoRepository.addMemo("Test Memo")

        verify(editor).putString(anyString(), anyString())
        verify(editor).apply()
    }

    // Hard to test Gson serialization without Robolectric or real JSON parsing in the mock response,
    // but we can verify interactions.
}
