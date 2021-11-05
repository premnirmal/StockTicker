package com.sec.android.app.shealth.events

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

interface AsyncEvent : Parcelable

@Parcelize
class ErrorEvent(val message: String) : AsyncEvent

@Parcelize
class FetchedEvent : AsyncEvent

@Parcelize
class RefreshEvent : AsyncEvent
