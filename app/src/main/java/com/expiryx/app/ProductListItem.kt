package com.expiryx.app

sealed class ProductListItem {
    data class Header(val title: String, val colorRes: Int) : ProductListItem()
    data class Item(val product: Product) : ProductListItem()
}
