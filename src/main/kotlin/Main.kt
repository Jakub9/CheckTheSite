package de.reeii.checkthesite

import de.reeii.checkthesite.di.appModule
import de.reeii.checkthesite.service.ConsoleService
import de.reeii.checkthesite.service.ConfigurationService
import de.reeii.checkthesite.service.MailService
import de.reeii.checkthesite.service.RequestService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class Bootstrap : KoinComponent {

    private val configurationService : ConfigurationService by inject()
    private val consoleService : ConsoleService by inject()
    private val requestService : RequestService by inject()
    private val mailService : MailService by inject()

    fun start() {
        configurationService.initialize()
        mailService.initialize()
        requestService.loadConfiguration()
        requestService.schedulePerConfiguration()
        consoleService.loop()
    }
}

fun main() {
    startKoin {
        modules(listOf(appModule))
    }
    Bootstrap().start()
}