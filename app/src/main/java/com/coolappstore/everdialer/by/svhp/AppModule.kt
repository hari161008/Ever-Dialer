package com.coolappstore.everdialer.by.svhp

import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.CallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.ContactsRepository
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<IContactsRepository> {
        ContactsRepository(androidContext().contentResolver)
    }
    single<ICallLogRepository> {
        CallLogRepository(androidContext().contentResolver)
    }
    single {
        PreferenceManager(androidContext())
    }
    viewModel { ContactsViewModel(androidApplication(), get()) }
    viewModel { CallLogViewModel(androidApplication(), get()) }
}