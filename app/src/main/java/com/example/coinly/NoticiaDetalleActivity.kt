package com.example.coinly

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.coinly.data.NewsRepository
import com.example.coinly.ui.Tone
import com.example.coinly.ui.Ui

class NoticiaDetalleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreen(R.layout.activity_noticia_detalle)

        findViewById<View>(R.id.btnBackNews).setOnClickListener { finish() }

        val requestedId = intent.getStringExtra(EXTRA_ID).orEmpty()
        if (requestedId.isBlank() || requestedId.contains('/')) { finish(); return }
        val news = NewsRepository.byId(requestedId)
        if (news == null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("noticias")
                .document(requestedId).get().addOnCompleteListener(this) { result ->
                    val loaded = if (result.isSuccessful) NewsRepository.fromDocument(result.result) else null
                    if (loaded == null) { android.widget.Toast.makeText(applicationContext, R.string.news_network_error, android.widget.Toast.LENGTH_LONG).show(); finish() }
                    else bindNews(loaded)
                }
            return
        }
        bindNews(news)
    }
    private fun bindNews(news: com.example.coinly.model.NewsItem) {
        findViewById<ImageView>(R.id.ivDetailImage).setImageResource(news.imageRes)
        findViewById<TextView>(R.id.tvDetailCategory).text = news.category
        findViewById<TextView>(R.id.tvDetailTitle).text = news.title
        findViewById<TextView>(R.id.tvDetailDate).text = news.date
        findViewById<TextView>(R.id.tvDetailBody).text = news.content

        // No hay URL propia por artículo: se abre una búsqueda con el título.
        findViewById<View>(R.id.btnOpenArticle).setOnClickListener {
            val url = "https://www.google.com/search?q=" + Uri.encode(news.title)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                Ui.toast(findViewById(android.R.id.content), Tone.DANGER, S.text(R.string.text_038))
            }
        }
    }

    companion object {
        const val EXTRA_ID = "news_id"
    }
}
