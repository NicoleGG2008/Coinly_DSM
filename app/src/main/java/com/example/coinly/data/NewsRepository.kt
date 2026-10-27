package com.example.coinly.data

import com.example.coinly.R
import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.model.NewsItem

object NewsRepository {

    val categories = listOf("Todas", "Ahorro", "Finanzas personales", "Inversión", "Educación financiera")

    val news = listOf(
        NewsItem("1", "5 estrategias para ahorrar siendo estudiante universitario",
            "Aprende cómo maximizar tu ahorro con ingresos limitados aplicando técnicas simples en tu día a día.",
            "12 Ago 2026", "Ahorro",
            "Ahorrar siendo estudiante puede parecer difícil, pero con los hábitos correctos es completamente posible. La regla 50/30/20 es un buen punto de partida: destina el 50% de tus ingresos a necesidades básicas, el 30% a deseos y el 20% al ahorro. Registrar cada gasto te dará claridad sobre dónde va tu dinero y te permitirá tomar mejores decisiones.",
            R.drawable.img_news_ahorro),
        NewsItem("2", "Cómo crear un presupuesto mensual que realmente funcione",
            "Guía práctica para estudiantes que quieren tomar el control de sus finanzas personales este semestre.",
            "10 Ago 2026", "Finanzas personales",
            "Un presupuesto efectivo no es solo una lista de gastos, es un plan de acción. Empieza por identificar tus ingresos fijos: becas, trabajo part-time, apoyo familiar. Luego lista tus gastos obligatorios y finalmente asigna lo restante a categorías discrecionales. Revisa y ajusta cada mes.",
            R.drawable.img_news_presupuesto),
        NewsItem("3", "Inversión básica para jóvenes: por dónde empezar",
            "La educación financiera temprana puede cambiar tu futuro. Descubre las opciones de inversión accesibles para estudiantes.",
            "8 Ago 2026", "Inversión",
            "Invertir no requiere grandes sumas de dinero. Con tan solo \$10 al mes puedes comenzar a construir un hábito de inversión. Los fondos de inversión indexados son una excelente opción para principiantes por su bajo costo y diversificación automática. La clave está en empezar temprano y ser constante.",
            R.drawable.img_news_inversion),
        NewsItem("4", "Becas universitarias: cómo aprovecharlas al máximo",
            "Convierte tu beca en una herramienta de crecimiento financiero y académico con estos consejos clave.",
            "5 Ago 2026", "Educación financiera",
            "Una beca es mucho más que dinero gratuito; es una inversión en tu futuro. Para aprovecharla al máximo: crea un presupuesto específico para la beca, separa una parte para materiales académicos, considera ahorrar un pequeño porcentaje y mantén tus calificaciones para renovarla.",
            R.drawable.img_news_becas)
    )

    val events = listOf(
        CalendarEvent("1", "2026-08-15", "Fecha límite matrícula", EventType.ACADEMIC),
        CalendarEvent("2", "2026-08-20", "Pago servicios estudiantiles", EventType.FINANCIAL),
        CalendarEvent("3", "2026-08-22", "Examen parcial Cálculo", EventType.ACADEMIC),
        CalendarEvent("4", "2026-08-28", "Inicio semana de proyectos", EventType.EVENT),
        CalendarEvent("5", "2026-09-01", "Cobro beca universitaria", EventType.FINANCIAL),
        CalendarEvent("6", "2026-09-05", "Feria de empleo universitaria", EventType.EVENT),
        CalendarEvent("7", "2026-09-10", "Entrega proyecto final", EventType.ACADEMIC),
        CalendarEvent("8", "2026-09-15", "Pago alquiler habitación", EventType.FINANCIAL)
    )

    fun byId(id: String) = news.firstOrNull { it.id == id }
}
