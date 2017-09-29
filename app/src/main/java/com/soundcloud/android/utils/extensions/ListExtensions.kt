package com.soundcloud.android.utils.extensions

import com.soundcloud.java.collections.Lists

fun <T>List<T>.partition(size:Int):List<List<T>> = Lists.partition(this, size )
