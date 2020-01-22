package minerva.android.main.handler

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import minerva.android.R
import minerva.android.history.HistoryFragment
import minerva.android.identities.IdentitiesFragment
import minerva.android.main.MainActivity
import minerva.android.services.ServicesFragment
import minerva.android.settings.SettingsFragment
import minerva.android.values.ValuesFragment

internal fun MainActivity.shouldShowAddIdentityIcon() = isIdentitiesTabSelected()

internal fun MainActivity.shouldShowAddValueIcon() = isValuesTabSelected()

internal fun MainActivity.isServicesTabSelected() = bottomNavigation.selectedItemId == R.id.services

internal fun MainActivity.isValuesTabSelected() = bottomNavigation.selectedItemId == R.id.values

internal fun MainActivity.isIdentitiesTabSelected() = bottomNavigation.selectedItemId == R.id.identities

internal fun MainActivity.prepareBottomNavMenu() {
    bottomNavigation.setOnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.identities -> replaceFragment(IdentitiesFragment())
            R.id.values -> replaceFragment(
                ValuesFragment(),
                R.string.values
            )
            R.id.services -> replaceFragment(
                ServicesFragment(),
                R.string.services
            )
            R.id.activity -> replaceFragment(
                HistoryFragment(),
                R.string.activity
            )
            R.id.settings -> replaceFragment(
                SettingsFragment(),
                R.string.settings
            )
        }
        true
    }
}

internal fun MainActivity.replaceFragment(fragment: Fragment, @StringRes title: Int = R.string.identities) {
    supportActionBar?.setTitle(title)
    invalidateOptionsMenu()
    supportFragmentManager.beginTransaction().apply {
        replace(R.id.fragmentContainer, fragment)
        commit()
    }
}