package by.nepravskysm.mailclientforeveonline.presentation.main.fragments.newmail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import by.nepravskysm.domain.entity.OutPutMail
import by.nepravskysm.domain.usecase.mails.SendMailUseCase
import by.nepravskysm.domain.utils.MAIL_IS_SENT
import by.nepravskysm.domain.utils.SEND_MAIL_ERROR


class NewMailViewModel(private val sendMail: SendMailUseCase) : ViewModel() {



    var mailBody = ""
    var mailEnd = ""
    var subject = ""
    val nameList = mutableSetOf<String>()
    val eventId: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }


    fun sendMail(){
        val mail = createOutPutMail()
        sendMail.setData(mail, nameList)
            .execute {
                onComplite { eventId.value = MAIL_IS_SENT }
                onError { eventId.value = SEND_MAIL_ERROR }
        }
    }

    private fun createOutPutMail(): OutPutMail {
        mailBody += mailEnd
        return OutPutMail(
            0,
            mailBody,
            mutableListOf(),
            subject)

    }

    fun initForwardMail(subject: String, from: String, mailBody: String){

        this.subject = "Fw: $subject"
        this.mailEnd = "<br /><br />" +
                "-------------------<br />" +
                "from: $from <br />" +
                "subject: $subject <br /><br /> " +
                mailBody

    }

    fun initReplayMail(subject: String, from: String, mailBody: String){
        this.subject = "Re: $subject"
        this.mailEnd = "<br /><br />" +
                "-------------------<br /><br />" +
                "from: $from <br />" +
                "subject: $subject<br /> <br />" +
                mailBody

    }


}