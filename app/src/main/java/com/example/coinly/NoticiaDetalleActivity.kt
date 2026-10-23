package com.example.coinly

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.coinly.data.NewsRepository

class NoticiaDetalleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_noticia_detalle)

        findViewById<View>(R.id.btnBackNews).setOnClickListener { finish() }

        val news = NewsRepository.byId(intent.getStringExtra(EXTRA_ID).orEmpty())
        if (news == null) {
            finish()
            return
        }
        findViewById<ImageView>(R.id.ivDetailImage).setImageResource(news.imageRes)
        findViewById<TextView>(R.id.tvDetailCategory).text = news.category
        findViewById<TextView>(R.id.tvDetailTitle).text = news.title
        findViewById<TextView>(R.id.tvDetailDate).text = news.date
        findViewById<TextView>(R.id.tvDetailBody).text = news.content

        // El artículo se lee completo dentro de la app (se oculta búsqueda externa)
        findViewById<View>(R.id.btnOpenArticle).visibility = View.GONE
    }

    companion object {
        const val EXTRA_ID = "news_id"
    }
}
