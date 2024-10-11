package com.example.privatevpn

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

lateinit var backs: ImageView

class AllServersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_servers)

        // Receive server data from MainActivity
        val serverData = intent.getStringExtra("SERVER_DATA")

        // Set up the back button behavior
        backs = findViewById(R.id.backs)
        backs.setOnClickListener {
            finish()  // Go back to the previous activity
        }

        // Initialize TabLayout and ViewPager
        val tabLayout = findViewById<TabLayout>(R.id.my_tab_layout)
        val viewPager = findViewById<ViewPager>(R.id.my_view_pager)

        // Create a Bundle to pass the server data to fragments
        val bundle = Bundle()
        bundle.putString("SERVER_DATA", serverData)

        // Set up ViewPager adapter
        val myvpAdapter = MyvpAdapter(supportFragmentManager)

        // Pass the bundle to each fragment
        val allFragment = AllFragment()
        allFragment.arguments = bundle
        myvpAdapter.addFragment(allFragment, "All")

        val streamingFragment = StreamingFragment()
        streamingFragment.arguments = bundle
        myvpAdapter.addFragment(streamingFragment, "Streaming")

        val gamingFragment = GamingFragment()
        gamingFragment.arguments = bundle
        myvpAdapter.addFragment(gamingFragment, "Gaming")

        viewPager.adapter = myvpAdapter

        // Link TabLayout with ViewPager
        tabLayout.setupWithViewPager(viewPager)

        // Set hardcoded text colors for unselected and selected tabs (both white)
        tabLayout.setTabTextColors(
            0xFFFFFFFF.toInt(),   // Unselected tab text color (white)
            0xFFFFBF00.toInt()    // Selected tab text color (yellow)
        )
    }
}
