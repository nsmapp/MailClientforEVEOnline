package by.nepravskysm.mailclientforeveonline.presentation.main


import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import by.nepravskysm.rest.api.createAuthUrl
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.viewmodel.ext.android.viewModel
import by.nepravskysm.mailclientforeveonline.R
import by.nepravskysm.mailclientforeveonline.presentation.main.dialog.CharacterChangeDialog
import by.nepravskysm.mailclientforeveonline.utils.pastImage
import by.nepravskysm.mailclientforeveonline.utils.showErrorToast
import by.nepravskysm.mailclientforeveonline.workers.CheckNewMailWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), CharacterChangeDialog.ChangeCharacterListener {


    private val vModel: MainViewModel by viewModel()
    private lateinit var navHeader: View
    private lateinit var navigationController: NavController
    private var loginListener: LoginListener? = null
    private val characterListDialog = CharacterChangeDialog()

    private val progresBarObserver = Observer<Boolean>{
        if(it){showProgresBar()
        }else{hideProgresBar()
            loginListener?.refreshDataAfterLogin()
        }
    }
    val nameObserver = Observer<String>{name ->  characterName.text = name}
    val characterIdObserver = Observer<Long>{id ->
            pastImage(activeCharacter, id)
        }

    val errorObserver = Observer<Long>{errorId ->

        Log.d("logd", "ERROR ERROR ERROER $errorId")
        showErrorToast(this ,errorId)}


    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navHeader = navigationView.getHeaderView(0)
        navigationController = findNavController(R.id.hostFragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.newMailFragment,
                R.id.inboxFragment,
                R.id.sendFragment,
                R.id.corpFragment,
                R.id.allianceFragment,
                R.id.mailingListFragment,
                R.id.aboutFragment,
                R.id.settingsFragment),
            drawerLayout)

        navigationView.setupWithNavController(navigationController)

        initOnClickListner()
        checkAuthCode()

        characterListDialog.setChangeCharacterListener(this)

        vModel.getActiveCharacterInfo()

        vModel.characterName.observe(this, nameObserver)
        vModel.characterId.observe(this, characterIdObserver)
        vModel.isVisibilityProgressBar.observe(this, progresBarObserver)
        vModel.errorId.observe(this, errorObserver)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()


        val syncPeriodically = PeriodicWorkRequestBuilder<CheckNewMailWorker>(15, TimeUnit.MINUTES,
            10, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork("myw4",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncPeriodically)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.hostFragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun characterChanged() {
        if(loginListener != null){
            loginListener?.refreshDataAfterLogin()
        }
        vModel.getActiveCharacterInfo()
    }

    private fun startBrowser(){
        val url: String = createAuthUrl()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun checkAuthCode(){
        if(intent.data != null){
            if(intent.data?.getQueryParameter("code") != null){
                val code: String = intent.data!!.getQueryParameter("code")!!
                vModel.startAuth(code)
            }else{
                if(loginListener != null){
                    loginListener?.refreshDataAfterLogin()
                }
            }
        }

    }

    private fun initOnClickListner(){
//        navHeader.addCharacter.setOnClickListener {
//            startBrowser()
//        }

        navMenu.setOnClickListener {
            drawerLayout.openDrawer(Gravity.LEFT)
        }

        activeCharacter.setOnClickListener {
            characterListDialog.show(supportFragmentManager, "characterList")
        }

    }

    private fun showProgresBar(){
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgresBar(){
        progressBar.visibility = View.GONE
    }

    fun setLoginListnerInterface(loginListener: LoginListener) {
        this.loginListener = loginListener

    }

    interface LoginListener{
        fun refreshDataAfterLogin()
    }

}


