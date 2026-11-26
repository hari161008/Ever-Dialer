package com.modiprincekr.thedialer

import com.modiprincekr.thedialer.controller.CallLogViewModel
import com.modiprincekr.thedialer.controller.ContactsViewModel
import com.modiprincekr.thedialer.controller.SmsViewModel
import com.modiprincekr.thedialer.modal.`interface`.ICallLogRepository
import com.modiprincekr.thedialer.modal.`interface`.IContactsRepository
import com.modiprincekr.thedialer.modal.`interface`.ISmsRepository
import com.modiprincekr.thedialer.modal.repository.CallLogRepository
import com.modiprincekr.thedialer.modal.repository.ContactsRepository
import com.modiprincekr.thedialer.modal.repository.SmsRepository
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<IContactsRepository> {
        ContactsRepository(androidContext().contentResolver, androidContext())
    }
    single {
        PreferenceManager(androidContext())
    }
    single<ICallLogRepository> {
        CallLogRepository(androidContext(), androidContext().contentResolver, get())
    }
    single<ISmsRepository> {
        SmsRepository(androidContext(), androidContext().contentResolver)
    }
    single {
        com.modiprincekr.thedialer.controller.util.PcBluetoothSyncManager(
            androidContext(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { ContactsViewModel(androidApplication(), get(), get()) }
    viewModel { CallLogViewModel(androidApplication(), get(), get()) }
    viewModel { SmsViewModel(androidApplication(), get(), get()) }
}