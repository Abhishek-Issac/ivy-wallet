package com.ivy.paywall.ui

import androidx.compose.runtime.Composable
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor() : ComposeViewModel<PaywallState, PaywallEvent>() {
  @Composable
  override fun uiState(): PaywallState {
    return PaywallState()
  }

  override fun onEvent(event: PaywallEvent) {
    // TODO
  }
}