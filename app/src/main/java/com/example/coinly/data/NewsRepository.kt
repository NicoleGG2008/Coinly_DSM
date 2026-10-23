package com.example.coinly.data

import com.example.coinly.R
import com.example.coinly.model.CalendarEvent
import com.example.coinly.model.EventType
import com.example.coinly.model.NewsItem

object NewsRepository {

    val categories = listOf("Todas", "Ahorro", "Finanzas personales", "Inversión", "Educación financiera")

    val news = listOf(
        NewsItem(
            "1",
            "5 estrategias para ahorrar siendo estudiante universitario",
            "Aprende cómo maximizar tu ahorro con ingresos limitados aplicando técnicas simples en tu día a día.",
            "12 Ago 2026",
            "Ahorro",
            "Ahorrar siendo estudiante universitario puede parecer un desafío difícil frente a los gastos diarios de transporte, fotocopias, alimentación y material de estudio. Sin embargo, desarrollar una disciplina financiera desde la universidad establece la base para tu libertad económica futura.\n\n" +
                "1. Aplica la Regla 50/30/20 Adaptada:\n" +
                "Divide tus ingresos (becas, empleos part-time o mesadas) asignando un 50% a tus gastos indispensables (transporte, servicios, comida), un 30% a actividades recreativas o personales, y destina un 20% inamovible al ahorro.\n\n" +
                "2. Registra Cada Pequeño Gasto:\n" +
                "Los 'gastos hormiga' como el café de la mañana, aperitivos entre clases o suscripciones en desuso pueden sumar más del 25% de tu presupuesto. Usa Coinly para registrar cada transacción al instante.\n\n" +
                "3. Aprovecha los Descuentos Estudiantiles:\n" +
                "Muestra siempre tu carné universitario en transporte público, museos, software educativo, librerías y restaurantes. Existen múltiples alianzas que reducen significativamente tus gastos obligatorios.\n\n" +
                "4. Planifica tus Comidas y Transporte:\n" +
                "Preparar tus almuerzos en casa y llevar una botella de agua reutilizable te ahorrará cientos de dólares al año. Del mismo modo, evaluar bonos mensuales de transporte resulta mucho más económico que pagar pasajes individuales.\n\n" +
                "5. Crea un Fondo de Emergencia Pequeño:\n" +
                "Empieza guardando aunque sea $10 o $20 cada mes. Contar con un colchón para imprevistos (reparación de laptop, salud, libros obligatorios) te evitará endeudarte con tarjetas de crédito o préstamos urgentes.",
            R.drawable.img_news_ahorro
        ),
        NewsItem(
            "2",
            "Cómo crear un presupuesto mensual que realmente funcione",
            "Guía práctica para estudiantes que quieren tomar el control de sus finanzas personales este semestre.",
            "10 Ago 2026",
            "Finanzas personales",
            "Un presupuesto no es una limitación a tu libertad, sino un mapa que te dice exactamente hacia dónde va tu dinero antes de gastarlo. La mayoría de estudiantes falla al hacer presupuestos porque crean metas irreales o demasiado rígidas.\n\n" +
                "Paso 1: Calcula tus Ingresos Fijos y Variables\n" +
                "Suma todas tus fuentes de ingreso mensuales: apoyo familiar, becas estudiantiles, sueldo por trabajos a tiempo parcial o ingresos freelance. Si tus ingresos varían, calcula con base en el mes más bajo del último trimestre.\n\n" +
                "Paso 2: Categoriza tus Gastos Obligatorios\n" +
                "Identifica aquellos pagos que no puedes eludir: alquiler de habitación, mensualidad universitaria, transporte diario y alimentación básica. Estos compromisos deben estar cubiertos prioritariamente.\n\n" +
                "Paso 3: Establece Límites Máximos por Categoría en Coinly\n" +
                "Configura tus topes en la sección de Presupuestos de Coinly. Cuando te acerques al 80% o alcances el 100% de tu límite, la aplicación te enviará alertas preventivas para proteger tu estabilidad financiera.\n\n" +
                "Paso 4: Evalúa y Reajusta Semanalmente\n" +
                "Tus hábitos cambian a lo largo del semestre (exámenes, vacaciones, proyectos). Si notas que gastaste menos en ocio pero más en impresiones y materiales, reasigna los montos entre presupuestos sin perder el control de tu balance final.",
            R.drawable.img_news_presupuesto
        ),
        NewsItem(
            "3",
            "Inversión básica para jóvenes: por dónde empezar",
            "La educación financiera temprana puede cambiar tu futuro. Descubre las opciones de inversión accesibles para estudiantes.",
            "8 Ago 2026",
            "Inversión",
            "La mayor ventaja financiera que tienes como estudiante universitario es el tiempo. Gracias al interés compuesto, empezar a invertir pequeñas cantidades a tus 20 años genera un impacto significativamente mayor que comenzar con sumas grandes a los 35 años.\n\n" +
                "1. Construye Primero tu Colchón de Seguridad:\n" +
                "Antes de realizar cualquier inversión, asegúrate de tener ahorrado el equivalente a al menos 1 o 2 meses de tus gastos indispensables en una cuenta líquida y segura.\n\n" +
                "2. Entiende el Concepto de Interés Compuesto:\n" +
                "El interés compuesto consiste en reinvertir las ganancias generadas por tu capital inicial, logrando que tu dinero trabaje y crezca exponencialmente con los años.\n\n" +
                "3. Diversificación y Fondos Indexados:\n" +
                "No pongas todos los huevos en la misma canasta. Los fondos indexados que replican el mercado de acciones global o nacional te permiten invertir en cientos de empresas sólidas al mismo tiempo con costos muy bajos.\n\n" +
                "4. Mantén la Visión a Largo Plazo:\n" +
                "La inversión no es un esquema para hacerte rico de la noche a la mañana. La paciencia, la constancia de aportar mes a mes y la educación continua son la clave del éxito financiero.",
            R.drawable.img_news_inversion
        ),
        NewsItem(
            "4",
            "Becas universitarias: cómo aprovecharlas al máximo",
            "Convierte tu beca en una herramienta de crecimiento financiero y académico con estos consejos clave.",
            "5 Ago 2026",
            "Educación financiera",
            "Obtener una beca académica o de asistencia es un hito trascendental en tu carrera estudiantil. Sin embargo, administrar inadecuadamente el fondo asignado puede hacerte perder oportunidades valiosas o poner en riesgo la renovación de tu beneficio.\n\n" +
                "Estrategias de Gestión para Becarios:\n\n" +
                "• Separa de Inmediato los Fondos Educativos:\n" +
                "Apenas recibas el desembolso de tu beca, destina el monto correspondiente a matrícula, libros, herramientas informáticas o colegiatura. No lo mezcles con tu dinero disponible diario.\n\n" +
                "• Presupuesta el Estipendio de Sustento:\n" +
                "Si tu beca incluye una cuota mensual para transporte y manutención, crea un presupuesto estricto en Coinly para dosificar ese monto a lo largo de las semanas sin quedarte corto al final del mes.\n\n" +
                "• Invierte en Recursos de Alto Rendimiento Académico:\n" +
                "Utiliza una fracción de tu beneficio en cursos complementarios, certificaciones, libros especializados o un equipo de cómputo eficiente que optimice tu desempeño laboral y universitario.\n\n" +
                "• Mantén el Rendimiento Académico Requerido:\n" +
                "Recuerda que casi todas las becas exigen un promedio acumulado mínimo. Revisa tu calendario de exámenes y programa alarmas para conservar tu beca semestre a semestre.",
            R.drawable.img_news_becas
        )
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
