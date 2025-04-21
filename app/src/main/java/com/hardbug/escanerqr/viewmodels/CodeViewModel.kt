package com.hardbug.escanerqr.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CodeViewModel : ViewModel() {
    private val _generatedCode = MutableLiveData<Bitmap?>()
    private val _name = MutableLiveData<String?>()
    val generatedCode: LiveData<Bitmap?> get() = _generatedCode
    val name : LiveData<String?> get() = _name
            
    fun setGeneratedCode(bitmap: Bitmap?) {
        _generatedCode.value = bitmap
    }

    fun setName(name : String?){
        _name.value = name
    }
}