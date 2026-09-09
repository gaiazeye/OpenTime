package com.gaiazeye.businessscheduler

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateNavigationIcon()

        toolbar.setNavigationOnClickListener {
            handleHeaderIconClick()
        }

        // Load Data
        AppointmentManager.loadAppointments(this)
        ClientManager.loadClients(this)
        ServiceManager.loadServices(this)
        AvailabilityManager.init(this)
        CategoryManager.init(this)

        auth = FirebaseAuth.getInstance()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment(), false)
                    bottomNav.selectedItemId = R.id.nav_home
                }
                R.id.nav_working_hours -> {
                    loadFragment(working_hours(), true)
                }
                R.id.nav_holidays -> {
                    loadFragment(HolidaysFragment(), true)
                }
                R.id.nav_breaks -> {
                    loadFragment(edit_breaks(), true)
                }
                R.id.nav_subscription -> {
                    loadFragment(subscription_status(), true)
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        bottomNav.setOnItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment(), false); true }
                R.id.nav_calendar -> { loadFragment(calendar(), false); true }
                R.id.nav_services -> { loadFragment(ServicesFragment(), false); true }
                R.id.nav_clients -> { loadFragment(clients_list(), false); true }
                R.id.nav_analytics -> { loadFragment(analytics(), false); true }
                else -> false
            }
            if (handled) {
                navView.setCheckedItem(item.itemId)
            }
            handled
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigationIcon()
        }

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun isSettingsPageActive(): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return currentFragment is SettingsFragment
    }

    private fun updateNavigationIcon() {
        if (isSettingsPageActive()) {
            supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        } else {
            supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_preferences)
        }
    }

    private fun handleHeaderIconClick() {
        if (isSettingsPageActive()) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            loadFragment(SettingsFragment(), true)
        }
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        if (!addToBackStack && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleHeaderIconClick()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
