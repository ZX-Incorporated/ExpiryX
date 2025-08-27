package com.expiryx.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ProductAdapter(
            onFavoriteClick = {}, // no favs in history
            onItemClick = {}      // optional: show detail
        )
        recyclerView.adapter = adapter

        productViewModel.history.observe(this) { products ->
            adapter.updateData(products)
        }
    }
}
