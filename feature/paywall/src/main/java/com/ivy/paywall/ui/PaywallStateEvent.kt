package com.ivy.paywall.ui

import androidx.compose.runtime.Immutable

@Immutable
data class PaywallUiState(
  val accountType: AccountType,
)

sealed interface AccountType {
  data class Unsubscribed(val price: PriceUi) : AccountType
  data object Lifetime : AccountType
  data object Subscribed : AccountType
}

data class PriceUi(
  val amount: Double,
  val currency: String,
)

sealed interface PaywallUiEvent {
  // TODO
}