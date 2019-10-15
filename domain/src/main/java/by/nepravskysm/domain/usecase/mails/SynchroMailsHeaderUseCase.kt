package by.nepravskysm.domain.usecase.mails

import by.nepravskysm.domain.entity.MailHeader
import by.nepravskysm.domain.entity.MailingList
import by.nepravskysm.domain.repository.database.ActiveCharacterRepository
import by.nepravskysm.domain.repository.database.AuthInfoRepository
import by.nepravskysm.domain.repository.rest.auth.AuthRepository
import by.nepravskysm.domain.repository.database.DBMailHeadersRepository
import by.nepravskysm.domain.repository.rest.mail.MailingListRepository
import by.nepravskysm.domain.repository.rest.mail.MailsHeadersRepository
import by.nepravskysm.domain.repository.utils.NamesRepository
import by.nepravskysm.domain.usecase.base.AsyncUseCase
import by.nepravskysm.domain.utils.formaMailHeaderList
import by.nepravskysm.domain.utils.listHeadersToUniqueList
import by.nepravskysm.domain.utils.removeUnsubscrubeMailingList
import java.lang.Exception
import java.util.logging.Level

class SynchroMailsHeaderUseCase(private val authRepository: AuthRepository,
                                private val authInfoRepository: AuthInfoRepository,
                                private val mailsHeadersRepository: MailsHeadersRepository,
                                private val namesRepository: NamesRepository,
                                private val dbMailHeadersRepository: DBMailHeadersRepository,
                                private val mailingListRepository: MailingListRepository,
                                private val activeCharacterRepository: ActiveCharacterRepository
) : AsyncUseCase<Boolean>() {


    override suspend fun onBackground(): Boolean{

        val characterName = activeCharacterRepository.getActiveCharacterName()
        val characterInfo = authInfoRepository.getAuthInfo(characterName)

        return try {
            getMailHeadersContainer(characterInfo.accessToken, characterInfo.characterId, characterInfo.characterName)

        }catch (e: Exception){
            val token = authRepository.refreshAuthToken(characterInfo.refreshToken)
            getMailHeadersContainer(token.accessToken, characterInfo.characterId, characterInfo.characterName)
        }
    }

    private suspend fun getMailHeadersContainer(accessToken: String, characterId: Long, characterName: String)
            :Boolean{

        val lastMailId: Long = dbMailHeadersRepository.getLastMailId(characterName)
        var headerList = mutableListOf<MailHeader>()

        val headers = mailsHeadersRepository
            .getLast50(accessToken, characterId)
            .filter {header -> header.mailId > lastMailId }

        if (headers.isNotEmpty()){
            headerList.addAll(headers)
            if (headers.size == 50){

                var minHeaderId: Long = headers.minBy { it.mailId }!!.mailId
                do {

                    val beforeHeaders = mailsHeadersRepository
                        .get50BeforeId(accessToken, characterId, minHeaderId)
                        .filter {header -> header.mailId > lastMailId }

                    if (beforeHeaders.isEmpty()){break}
                        else{
                        headerList.addAll(beforeHeaders)
                        minHeaderId = beforeHeaders.minBy { it.mailId }!!.mailId
                    }
                }while(beforeHeaders.size == 50)
            }
        }

        if (headerList.isNotEmpty()){

            val nameMap = HashMap<Long, String>()
            val mailingList: List<MailingList> = mailingListRepository
                .getMailingList(accessToken, characterId)
            val mailingListIds = mailingList.map { it.id }
            for (list in mailingList){
                nameMap[list.id] = list.name
            }

            val mailingListHeaders = mutableListOf<MailHeader>()
            val noMailingListHeaders = mutableListOf<MailHeader>()

            for(header in headerList){
                if(header.labels.isNotEmpty()){
                    noMailingListHeaders.add(header)
                }else{
                    for (recipient in header.recipients.map { it.id }){
                        if (mailingListIds.contains(recipient)){
                            mailingListHeaders.add(header)
                        }
                    }
                }
            }

            headerList.clear()
            headerList.addAll(noMailingListHeaders)
            headerList.addAll(mailingListHeaders)

            val nameIdList = noMailingListHeaders.map { it.fromId }.distinct()
            nameMap.putAll(namesRepository.getNameMap(nameIdList))

            for (header in headerList){
                try {
                    header.fromName = nameMap[header.fromId]!!
                }catch (e: Exception){

                }
            }
            dbMailHeadersRepository
                .saveMailsHeaders(headerList, characterName)
        }

        return true
    }

}