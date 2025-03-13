package com.trackasia.sample

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val toolbarText: TextView = findViewById(R.id.toolbarText)
        setSupportActionBar(toolbar)
        sharedPreferences = getSharedPreferences("trackasia", Context.MODE_PRIVATE)
        var idCountry = sharedPreferences.getString("country", "vn")
        toolbarText.text = idCountry?.let { MapUtils(this).getNameContry(idCountry) }
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
                    supportActionBar?.title = "Map Cluster"
                }

                R.id.navigation_map_single_point -> {
                    selectedFragment = MapSinglePointFragment()
                    supportActionBar?.title = "Map Single"
                }

                R.id.navigation_map_multi_point -> {
                    selectedFragment = MapDirectionPointFragment()
                    supportActionBar?.title = "Map Multi"
                }

                R.id.navigation_layer -> {
                    selectedFragment = MapAnimationFragment()
                    supportActionBar?.title = "Map Animation"
                }

                R.id.navigation_feature -> {
                    selectedFragment = MapFeatureFragment()
                    supportActionBar?.title = "Map Feature"
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, selectedFragment!!).commit()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val toolbarText: TextView = findViewById(R.id.toolbarText)
        saveCountry(item.title.toString())
//        if(item.title == "Malaysia"){
//            showSnackbar("Malaysia is under development, please come back later")
//        }
        toolbarText.text = item.title
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        when (currentFragment) {
            is MapClusterFragment -> {
                selectedFragment = MapClusterFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment as MapClusterFragment)
                    .commit()

            }

            is MapSinglePointFragment -> {
                selectedFragment = MapSinglePointFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment as MapSinglePointFragment)
                    .commit()
            }

            is MapAnimationFragment -> {
                selectedFragment = MapAnimationFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment as MapAnimationFragment)
                    .commit()
            }

            is MapDirectionPointFragment -> {
                selectedFragment = MapDirectionPointFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment as MapDirectionPointFragment)
                    .commit()
            }

            is MapFeatureFragment -> {
                selectedFragment = MapFeatureFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment as MapFeatureFragment)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, selectedFragment!!).commit()
        return when (id) {
            R.id.action_vietnam -> true

            R.id.action_thailand -> true

//            R.id.action_malaysia -> true
//
//            R.id.action_taiwain -> true

            R.id.action_singapo -> true

            else -> super.onOptionsItemSelected(item)
        }
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

}