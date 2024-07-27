package de.reeii.checkthesite.di

import de.reeii.checkthesite.service.*
import org.koin.dsl.module

val appModule = module {
    single { RequestService(get(), get()) }
    single { ConfigurationService(get(), get()) }
    single { FileService() }
    single { ConsoleService() }
    single { MailService(get(), get()) }
}