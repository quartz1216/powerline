package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.data.MemoRepository
import com.quartz.powerline.domain.models.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemoRepository(application)

    private val _memos = MutableStateFlow<List<Memo>>(emptyList())
    val memos: StateFlow<List<Memo>> = _memos.asStateFlow()

    init {
        loadMemos()
    }

    private fun loadMemos() {
        viewModelScope.launch {
            _memos.value = repository.getAllMemos()
        }
    }

    fun deleteMemos(ids: Set<String>) {
        viewModelScope.launch {
            repository.deleteMemos(ids)
            loadMemos()
        }
    }
}
