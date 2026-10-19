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
            "Cómo ahorrar siendo estudiante universitario",
            "Guía básica de inversiones para jóvenes",
            "Errores comunes al crear tu primer presupuesto"
        )

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, articulos)
        listView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, "Abriendo: ${articulos[position]}", Toast.LENGTH_SHORT).show()
        }
    }
}
