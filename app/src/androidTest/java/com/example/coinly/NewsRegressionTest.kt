package com.example.coinly

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.coinly.data.NewsRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Regression for the crash when Noticias initializes an article containing %. */
@RunWith(AndroidJUnit4::class)
class NewsRegressionTest {
    @Test fun newsInitializeAndPreserveLiteralPercentages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = context.getString(R.string.text_089)
        assertEquals(expected, S.text(R.string.text_089))
        val news = NewsRepository.byId("1")!!
        assertEquals(expected, news.content)
        assertTrue(news.content.contains("50%"))
        assertTrue(news.content.contains("30%"))
        assertTrue(news.content.contains("20%"))
        assertEquals(4, NewsRepository.news.size)
        assertEquals(4, NewsRepository.news.map { it.id }.distinct().size)
    }

    @Test fun newsCalendarLabelsStillFormatArguments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(context.getString(R.string.text_045, 15, "octubre"),
            S.text(R.string.text_045, 15, "octubre"))
        assertEquals(context.getString(R.string.text_047, "octubre"),
            S.text(R.string.text_047, "octubre"))
        assertTrue(NewsRepository.events.isNotEmpty())
    }
}
