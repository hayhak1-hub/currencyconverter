package com.hayhak.currencyconverter.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light Mode ────────────────────────────────────────────────────
val Blue40     = Color(0xFF1565C0)   // Derin mavi  — primary buton/aksan
val BlueGrey40 = Color(0xFF455A64)   // Mavi-gri    — secondary
val Teal40     = Color(0xFF00796B)   // Teal        — tertiary

// ── Dark Mode ─────────────────────────────────────────────────────
// Arka plan ailesi — gece mavisi, tam siyah değil
val DarkBg          = Color(0xFF0D1829)   // Ana arka plan  — gece mavisi
val DarkSurface     = Color(0xFF122033)   // Kart/yüzey     — koyu lacivert
val DarkSurfaceVar  = Color(0xFF1A2E44)   // İkinci yüzey   — orta lacivert

// Primary  — parlayan gök mavisi, koyu arka planda çok iyi görünür
val Blue80          = Color(0xFF4FC3F7)
val BluePrimCont    = Color(0xFF004D70)   // primary container
val BluePrimContOn  = Color(0xFFC8EEFF)   // on primary container

// Secondary — açık cyan-mavi tonu
val Cyan80          = Color(0xFF80DEEA)
val CyanSecCont     = Color(0xFF005363)
val CyanSecContOn   = Color(0xFFA9EFFF)

// Tertiary — daha açık mavi (primary ile aynı aile)
val LightBlue80     = Color(0xFF90CAF9)
val LightBlueTerCont   = Color(0xFF1B4568)
val LightBlueTerContOn = Color(0xFFD1E9FF)

// Yüzey metin rengi
val DarkOnSurface    = Color(0xFFE3EDF8)   // biraz mavi kırık beyaz
val DarkOnSurfaceVar = Color(0xFF9BB3CA)   // ikincil metin — soluk mavi-gri

// ── AMOLED Siyah — saf siyah yüzeyler ────────────────────────────
val AmoledBg         = Color(0xFF000000)   // Tam siyah arka plan
val AmoledSurface    = Color(0xFF000000)   // Tam siyah yüzey
val AmoledSurfaceVar = Color(0xFF0D0D0D)   // Kartlar için hafif yükseltilmiş

// ── Trend Renkleri (her iki temada da sabit anlam taşır) ──────────
val TrendUp     = Color(0xFF2E7D32)   // Light: koyu yeşil (WCAG AA)
val TrendDown   = Color(0xFFC62828)   // Light: koyu kırmızı (WCAG AA)
val TrendUpDark   = Color(0xFF81C784) // Dark: açık yeşil
val TrendDownDark = Color(0xFFEF9A9A) // Dark: açık kırmızı
