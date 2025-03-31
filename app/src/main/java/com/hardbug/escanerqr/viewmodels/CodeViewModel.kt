package com.hardbug.escanerqr.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CodeViewModel : ViewModel() {
    private val _generatedCode = MutableLiveData<Bitmap?>()
    val generatedCode: LiveData<Bitmap?> get() = _generatedCode

    fun setGeneratedCode(bitmap: Bitmap?) {
        _generatedCode.value = bitmap
    }
}