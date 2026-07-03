package com.kelly.usbconnection.di

import com.kelly.usbconnection.UsbConnectionManager
import com.kelly.usbconnection.UsbConnectionVM
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { UsbConnectionManager(androidApplication()) }
    viewModel { UsbConnectionVM(get()) }
}