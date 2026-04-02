package com.ivy.paywall.ui

import androidx.compose.runtime.Immutable

@Immutable
data class PaywallUiState(
  val price: PriceUi,
)

data class PriceUi(
  val amount: Double,
  val currency: String,
)

sealed interface PaywallUiEvent {
  // TODO
}