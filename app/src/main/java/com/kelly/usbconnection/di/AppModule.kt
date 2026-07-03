package com.kelly.usbconnection.di

import com.kelly.usbconnection.UsbConnectionManager
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
    single { UsbConnectionManager(androidApplication()) }
}