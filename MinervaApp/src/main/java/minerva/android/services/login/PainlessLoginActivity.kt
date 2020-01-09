package minerva.android.services.login

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_painless_login.*
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import org.koin.androidx.viewmodel.ext.android.viewModel

class PainlessLoginActivity : AppCompatActivity() {

    private val viewModel: PainlessLoginViewModel by viewModel()
    private val identitiesAdapter = IdentitiesAdapter()
    private lateinit var scanResult: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painless_login)
        setupActionBar()
        intent.getStringExtra(SCAN_RESULT)?.let { scanResult = it }
    }

    override fun onResume() {
        super.onResume()
        setupRecycleView()
        viewModel.getIdentities()?.let {
            identitiesAdapter.updateList(it)
        }
        loginButton.setOnClickListener {
            Toast.makeText(this, identitiesAdapter.getSelectedIdentity()?.name, Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show()
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
