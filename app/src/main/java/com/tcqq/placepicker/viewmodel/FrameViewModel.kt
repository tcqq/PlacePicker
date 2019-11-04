package com.tcqq.placepicker.viewmodel

import androidx.annotation.MenuRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author Alan Perry
 * @since 19/08/2018 Created
 */
class FrameViewModel : ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()
    @MenuRes
    val menuResId: MutableLiveData<Int> = MutableLiveData()
}