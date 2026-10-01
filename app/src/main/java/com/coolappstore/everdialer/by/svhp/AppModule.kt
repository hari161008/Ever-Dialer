package com.coolappstore.everdialer.by.svhp

import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.CallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.ContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.SmsRepository
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
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
    viewModel { ContactsViewModel(androidApplication(), get(), get()) }
    viewModel { CallLogViewModel(androidApplication(), get(), get()) }
    viewModel { SmsViewModel(androidApplication(), get()) }
}