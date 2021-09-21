package com.example.drawingapp

import android.os.Build

inline fun <T> sdkVersion29andUp(onSdk29:() -> T): T?{
    return if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.Q){
        onSdk29()
    }else null
}