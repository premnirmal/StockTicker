package com.github.premnirmal.ticker.events

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

interface AsyncEvent : Parcelable

@Parcelize
class ErrorEvent constructor(val message: String) : AsyncEvent
