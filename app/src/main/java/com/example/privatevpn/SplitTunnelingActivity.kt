package com.example.privatevpn

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class SplitTunnelingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_split_tunneling)

        // Enable edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recyclerView_apps)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch the installed apps and bind to RecyclerView
        val installedApps = getInstalledApps()
        recyclerView.adapter = AppAdapter(installedApps)
    }

    // Function to get the installed apps
    private fun getInstalledApps(): List<AppModel> {
        val pm: PackageManager = packageManager
        val apps = mutableListOf<AppModel>()

        val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedPackages) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) { // Only display launchable apps
                val appName = app.loadLabel(pm).toString()
                val appIcon = app.loadIcon(pm)
                apps.add(AppModel(appName, appIcon))
            }
        }
        return apps
    }
}

// Model class for each app
data class AppModel(val name: String, val icon: Drawable)

// Adapter for the RecyclerView
class AppAdapter(private val apps: List<AppModel>) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    // ViewHolder for each app item
    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val appName: TextView = itemView.findViewById(R.id.app_name)
        val appSwitch: Switch = itemView.findViewById(R.id.app_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appIcon.setImageDrawable(app.icon)
        holder.appName.text = app.name
        holder.appSwitch.isChecked = false // You can manage switch states as needed
    }

    override fun getItemCount(): Int {
        return apps.size
    }
}
