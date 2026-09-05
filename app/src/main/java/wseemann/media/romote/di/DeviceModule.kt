package wseemann.media.romote.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.database.device.DeviceDatabase
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.device.DeviceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeviceModule {

    @Singleton
    @Provides
    fun provideDeviceManager(deviceRepository: DeviceRepository): DeviceManager {
        DeviceManagerSingleton.deviceManager = DeviceManager(deviceRepository)
        return DeviceManagerSingleton.deviceManager
    }

    @Singleton
    @Provides
    fun provideDeviceRepository(
        deviceDatabase: DeviceDatabase,
        sharedPreferences: SharedPreferences
    ): DeviceRepository = DeviceRepository(
        deviceDatabase = deviceDatabase,
        sharedPreferences = sharedPreferences
    )

    @Provides
    fun provideDeviceDatabase(@ApplicationContext context: Context): DeviceDatabase =
        DeviceDatabase(context)

    object DeviceManagerSingleton {
        lateinit var deviceManager: DeviceManager
    }
}
