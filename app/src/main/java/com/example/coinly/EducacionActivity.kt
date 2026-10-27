package com.example.coinly

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class EducacionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_educacion)

        val listView = findViewById<ListView>(R.id.listViewNews)

        val articulos = arrayOf(
            S.text(R.string.text_026),
            S.text(R.string.text_027),
            S.text(R.string.text_028)
        )

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, articulos)
        listView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, S.text(R.string.text_029, articulos[position]), Toast.LENGTH_SHORT).show()
        }
    }
}
