package com.madhwanikritika.sportsmeet.ui.util

import java.text.NumberFormat
import java.util.Locale

fun formatUsd(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.CANADA).format(amount)
