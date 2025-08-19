package com.expiryx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recyclerProducts)
        val emptyState = findViewById<View>(R.id.emptyStateContainer)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val products = db.productDao().getAll()

            if (products.isEmpty()) {
                recycler.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                recycler.layoutManager = LinearLayoutManager(this@MainActivity)
                recycler.adapter = ProductAdapter(products)
            }
        }
    }
}
