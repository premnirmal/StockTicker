package com.sec.android.app.shealth.mock

import com.sec.android.app.shealth.TestActivity
import com.sec.android.app.shealth.components.AppComponent
import dagger.Component
import javax.inject.Singleton

/**
 * Created by android on 3/22/17.
 */
@Singleton
@Component(modules = [MockAppModule::class])
interface MockAppComponent : AppComponent {

  fun inject(activity: TestActivity)
}