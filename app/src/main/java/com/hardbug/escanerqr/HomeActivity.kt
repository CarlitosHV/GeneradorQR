package com.hardbug.escanerqr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hardbug.escanerqr.views.CameraFragment
import com.hardbug.escanerqr.views.CreateFragment
import com.hardbug.escanerqr.views.HomeFragment
import com.hardbug.escanerqr.views.AdvancedCreateFragment
import com.hardbug.escanerqr.views.CustomizeCode

class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)

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

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigationState(supportFragmentManager.findFragmentById(R.id.content))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
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
        val title = when (fragment) {
            is HomeFragment -> getString(R.string.title_home)
            is CameraFragment -> getString(R.string.title_scan)
            is CreateFragment -> getString(R.string.title_create)
            is AdvancedCreateFragment -> getString(R.string.title_advanced_create)
            is CustomizeCode -> getString(R.string.title_customize)
            else -> getString(R.string.app_name)
        }
        supportActionBar?.title = title

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