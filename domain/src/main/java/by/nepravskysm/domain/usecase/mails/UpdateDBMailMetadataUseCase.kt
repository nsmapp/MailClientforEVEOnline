package by.nepravskysm.domain.usecase.mails

import by.nepravskysm.domain.repository.database.DBMailHeadersRepository
import by.nepravskysm.domain.usecase.base.AsyncUseCase

class UpdateDBMailMetadataUseCase(private val dbMailHeadersRepository: DBMailHeadersRepository)
    :AsyncUseCase<Boolean>(){


    private var mailId: Long = 0
    fun setData(mailId: Long): UpdateDBMailMetadataUseCase{
        this.mailId = mailId
        return this
    }
    override suspend fun onBackground():Boolean {
        dbMailHeadersRepository.setMailIsRead(mailId)
        return true
    }
}