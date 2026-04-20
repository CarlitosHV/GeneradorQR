package com.hardbug.escanerqr

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hardbug.escanerqr.views.AdvancedCreateFragment
import com.hardbug.escanerqr.views.CameraFragment
import com.hardbug.escanerqr.views.CreateFragment
import com.hardbug.escanerqr.views.CustomizeCode
import com.hardbug.escanerqr.views.HomeFragment

class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var bottomNavCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        appBarLayout = findViewById(R.id.appBarLayout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavCard = findViewById(R.id.bottomNavCard)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { _, insets -> insets }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            appBarLayout.updatePadding(top = insets.top)

            bottomNavCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.fab_margin)
            }
            
            windowInsets
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.page_scan -> {
                    replaceFragment(CameraFragment())
                    true
                }
                R.id.page_create -> {
                    replaceFragment(CreateFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigationState(supportFragmentManager.findFragmentById(R.id.content))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_SCAN" -> {
                bottomNavigationView.selectedItemId = R.id.page_scan
            }
            "ACTION_CREATE" -> {
                bottomNavigationView.selectedItemId = R.id.page_create
            }
            else -> {
                if (supportFragmentManager.findFragmentById(R.id.content) == null) {
                    bottomNavigationView.selectedItemId = R.id.page_home
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHelpDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_help, null)
        val tvGithubLink = dialogView.findViewById<TextView>(R.id.tvGithubLink)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        tvGithubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW,
                getString(R.string.https_github_com_carlitoshv).toUri())
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        
        if (addToBackStack) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        transaction.replace(R.id.content, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
        updateNavigationState(fragment)
    }

    private fun updateNavigationState(fragment: Fragment?) {
        if (fragment is AdvancedCreateFragment || fragment is CustomizeCode) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { onBackPressed() }
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        
        supportActionBar?.setLogo(null)
        supportActionBar?.setDisplayUseLogoEnabled(false)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}