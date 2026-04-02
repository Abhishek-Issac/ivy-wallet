package com.ivy.paywall.ui

import androidx.compose.runtime.Composable
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor() : ComposeViewModel<PaywallUiState, PaywallUiEvent>() {
  @Composable
  override fun uiState(): PaywallUiState {
    return PaywallUiState(
      price = PriceUi(
        amount = 4.99,
        currency = "USD"
      )
    )
  }

  override fun onEvent(event: PaywallUiEvent) {
    // TODO
  }
}