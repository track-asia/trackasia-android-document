package com.trackasia.sample

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.trackasia.sample.utils.MapUtils


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var selectedFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up country info
        val toolbarText: TextView = findViewById(R.id.toolbarText)
        sharedPreferences = getSharedPreferences("trackasia", Context.MODE_PRIVATE)
        var idCountry = sharedPreferences.getString("country", "vn")
        toolbarText.text = idCountry?.let { MapUtils(this).getNameContry(idCountry) }
        
        // Set up country selector
        toolbarText.setOnClickListener {
            showCountryMenu(it)
        }
        
        // Set initial screen title
        updateTitle("Single Point")
        
        // Set up bottom navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        selectedFragment = MapSinglePointFragment()
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_map_cluster -> {
                    selectedFragment = MapClusterFragment()
                    updateTitle("Clusters")
                }

                R.id.navigation_map_single_point -> {
                    selectedFragment = MapSinglePointFragment()
                    updateTitle("Single Point")
                }

                R.id.navigation_map_multi_point -> {
                    selectedFragment = MapDirectionPointFragment()
                    updateTitle("Multi-Point")
                }

                R.id.navigation_layer -> {
                    selectedFragment = MapAnimationFragment()
                    updateTitle("Animation")
                }

                R.id.navigation_feature -> {
                    selectedFragment = MapFeatureFragment()
                    updateTitle("Features")
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, selectedFragment!!).commit()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // No longer needed as we're using popup menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // No longer needed as we're handling country selection in popup menu
        return super.onOptionsItemSelected(item)
    }

    private fun showSnackbar(message: String) {
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun saveCountry(country: String) {
        val editor = sharedPreferences.edit()
        when (country) {
            "Việt Nam" -> editor.putString("country", "vn")
            "Singapore" -> editor.putString("country", "sg")
            "Thailand" -> editor.putString("country", "th")
//            "Taiwan" -> editor.putString("country", "tw")
//            "Malaysia" -> editor.putString("country", "my")
        }
        editor.apply()
    }

    private fun updateTitle(title: String) {
        val titleTextView: TextView? = findViewById(R.id.toolbarTitleText)
        titleTextView?.text = title
    }

    private fun showCountryMenu(view: View) {
        val popup = android.widget.PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.toolbar_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            val toolbarText: TextView = findViewById(R.id.toolbarText)
            saveCountry(item.title.toString())
            toolbarText.text = item.title
            
            // Refresh current fragment to update map
            refreshCurrentFragment()
            true
        }
        
        popup.show()
    }
    
    private fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        when (currentFragment) {
            is MapClusterFragment -> selectedFragment = MapClusterFragment()
            is MapSinglePointFragment -> selectedFragment = MapSinglePointFragment()
            is MapAnimationFragment -> selectedFragment = MapAnimationFragment()
            is MapDirectionPointFragment -> selectedFragment = MapDirectionPointFragment()
            is MapFeatureFragment -> selectedFragment = MapFeatureFragment()
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, selectedFragment!!).commit()
    }
}