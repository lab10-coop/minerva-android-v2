package minerva.android.services.login

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_painless_login.*
import kotlinx.android.synthetic.main.fragment_create_wallet.*
import minerva.android.R
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.QrCodeResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class PainlessLoginActivity : AppCompatActivity() {

    private val viewModel: PainlessLoginViewModel by viewModel()
    private val identitiesAdapter = IdentitiesAdapter()
    private lateinit var qrCodeResponse: QrCodeResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painless_login)
        setupActionBar()
        intent.getSerializableExtra(SCAN_RESULT)?.let { qrCodeResponse = it as QrCodeResponse }
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.loadingLiveData.observe(this, EventObserver { if (it) showLoader() else hideLoader() })
        viewModel.errorLiveData.observe(this, EventObserver {
            //            todo handle login error
            Toast.makeText(this, "Ups, sth wen wrong", Toast.LENGTH_LONG).show()
            finish()
        })
        viewModel.loginLiveData.observe(this, EventObserver {
            //            todo handle login success
            Toast.makeText(this, "Login successful!", Toast.LENGTH_LONG).show()
            finish()
        })
        viewModel.requestedFieldsLiveData.observe(this, EventObserver {
            Toast.makeText(this, getString(R.string.requested_data_message), Toast.LENGTH_LONG).show()
        })
    }

    override fun onResume() {
        super.onResume()
        setupServiceData()
        setupRecycleView()
        setupIdentitiesList()
        setLoginButtonOnClickListener()
    }

    private fun hideLoader() {
        loginButton.visible()
        loginProgressBar.invisible()
    }

    private fun showLoader() {
        loginProgressBar.visible()
        loginButton.invisible()
    }

    private fun setupServiceData() {
        serviceName.text = qrCodeResponse.serviceName
        requestedFields.text = qrCodeResponse.identityFields
    }

    private fun setupIdentitiesList() {
        viewModel.getIdentities()?.let {
            identitiesAdapter.updateList(it)
        }
    }

    private fun setLoginButtonOnClickListener() {
        loginButton.setOnClickListener {
            handleLoginButton()
        }
    }

    private fun handleLoginButton() {
        identitiesAdapter.getSelectedIdentity()?.let {
            viewModel.handleLoginButton(it, qrCodeResponse)
        }.orElse {
            Toast.makeText(this, getString(R.string.select_identity_message), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = String.Empty
            setBackgroundDrawable(ColorDrawable(getColor(R.color.lightGray)))
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        window.statusBarColor = getColor(R.color.lightGray)
    }

    private fun setupRecycleView() {
        identities.apply {
            layoutManager = LinearLayoutManager(this@PainlessLoginActivity)
            adapter = identitiesAdapter
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    companion object {
        const val SCAN_RESULT = "scanResult"
    }
}
