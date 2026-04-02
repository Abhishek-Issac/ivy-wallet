package com.ivy.paywall.ui

import androidx.compose.runtime.Composable
import com.ivy.navigation.screenScopedViewModel

@Composable
fun PaywallScreen() {
  val viewModel: PaywallViewModel = screenScopedViewModel()

  PaywallUi(
    uiState = viewModel.uiState(),
    onEvent = viewModel::onEvent,
  )
}

@Composable
private fun PaywallUi(
  uiState: PaywallState,
  onEvent: (PaywallEvent) -> Unit
) {
  // TODO
}