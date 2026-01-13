package com.berat.mediagrab.data.model

/** Download state enum with Turkish display names */
@Suppress("unused")
enum class DownloadState(val displayName: String) {
    QUEUED("Kuyrukta"),
    FETCHING_INFO("Bilgi Alınıyor"),
    DOWNLOADING("İndiriliyor"),
    PROCESSING("İşleniyor"),
    COMPLETED("Tamamlandı"),
    FAILED("Başarısız"),
    CANCELLED("İptal Edildi"),
    PAUSED("Duraklatıldı");

    val isActive: Boolean
        get() = this == DOWNLOADING || this == PROCESSING || this == FETCHING_INFO

    val isFinished: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED
}
