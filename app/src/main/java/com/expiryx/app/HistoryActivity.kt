package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var navHome: ImageView
    private lateinit var navCart: ImageView
    private lateinit var navHistory: ImageView
    private lateinit var navSettings: ImageView

    private lateinit var recyclerHistory: RecyclerView
    private lateinit var placeholder: LinearLayout

    private val viewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        navHome = findViewById(R.id.navHome)
        navCart = findViewById(R.id.navCart)
        navHistory = findViewById(R.id.navHistory)
        navSettings = findViewById(R.id.navSettings)
        recyclerHistory = findViewById(R.id.recyclerHistory)
        placeholder = findViewById(R.id.historyPlaceholder)

        // highlight current tab
        navHome.setImageResource(R.drawable.ic_home_unfilled)
        navCart.setImageResource(R.drawable.ic_cart)
        navHistory.setImageResource(R.drawable.ic_clock_filled)
        navSettings.setImageResource(R.drawable.ic_settings_unfilled)

        // setup RecyclerView
        adapter = HistoryAdapter(emptyList())
        recyclerHistory.layoutManager = LinearLayoutManager(this)
        recyclerHistory.adapter = adapter

        // Observe history list
        viewModel.allHistory.observe(this) { list ->
            if (list.isNullOrEmpty()) {
                recyclerHistory.visibility = View.GONE
                placeholder.visibility = View.VISIBLE
            } else {
                adapter.updateData(list)
                recyclerHistory.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            }
        }

        // bottom nav wiring
        navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        navCart.setOnClickListener {
            Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show()
        }
        navHistory.setOnClickListener {
            // already here
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
