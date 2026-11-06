package com.example.data.repository

/**
 * Human-readable assessment for the European Air Quality Index (European AQI).
 * The caller must check that the provider actually supplied an AQI value.
 */
internal object EuropeanAqi {
    fun describe(aqi: Int): Pair<String, String> = when {
        aqi < 0 -> "غير متاح" to "لا تتوفر قراءة لمؤشر جودة الهواء حالياً."
        aqi <= 20 -> "جيد" to "جودة الهواء جيدة جداً؛ يمكن ممارسة الأنشطة المعتادة في الخارج."
        aqi <= 40 -> "مقبول" to "جودة الهواء مقبولة؛ قد يتأثر الأشخاص شديدو الحساسية."
        aqi <= 60 -> "متوسط" to "قد تظهر آثار صحية لدى الفئات الحساسة؛ راقب الأعراض عند قضاء وقت طويل في الخارج."
        aqi <= 80 -> "رديء" to "قلّل الأنشطة البدنية الشاقة في الخارج، خصوصاً للفئات الحساسة."
        aqi <= 100 -> "رديء جداً" to "قلّل التعرض للهواء الخارجي واتبع الإرشادات الصحية المحلية."
        else -> "شديد السوء" to "تجنب المجهود الخارجي غير الضروري واتبع الإرشادات الصحية المحلية."
    }
}
