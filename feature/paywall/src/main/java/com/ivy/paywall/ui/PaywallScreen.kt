package com.ivy.paywall.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.component.BackButton

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
  uiState: PaywallUiState,
  onEvent: (PaywallUiEvent) -> Unit
) {
  val nav = navigation()
  val uriHandler = LocalUriHandler.current
  val termsUrl = "https://ivywallet.app/terms"
  val privacyUrl = "https://ivywallet.app/privacy"
  val manageSubscriptionsUrl = "https://play.google.com/store/account/subscriptions"

  @OptIn(ExperimentalMaterial3Api::class)
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Go Pro",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          BackButton(onClick = { nav.back() })
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Unlock the full Ivy Wallet experience",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "You can manage or cancel your subscription at any time in Google Play.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        Card(
          modifier = Modifier
            .fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          onClick = {}
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(Modifier.weight(1f)) {
                Text(
                  text = "Ivy Pro — Monthly",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "Auto-renews. Cancel anytime in Google Play.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(Modifier.width(12.dp))

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${uiState.price.amount} ${uiState.price.currency}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "monthly",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
              // Billing purchase flow will be triggered once wired.
              // Keep UI compliant by not allowing checkout without showing real pricing.
            }
          ) {
            Text(text = "Subscribe")
          }

          OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
              // Restore/re-sync purchases once billing is wired.
            }
          ) {
            Text(text = "Restore purchases")
          }

          OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
              uriHandler.openUri(manageSubscriptionsUrl)
            }
          ) {
            Text(text = "Manage subscriptions")
          }
        }
      }

      item {
        Card(shape = RoundedCornerShape(12.dp)) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Subscription details",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Payment will be charged to your Google Play account at confirmation of purchase.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Your subscription renews automatically unless canceled in Google Play before the next billing date.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "By subscribing, you agree to the Terms of Service and Privacy Policy.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              LinkText(
                text = "Terms",
                onClick = { uriHandler.openUri(termsUrl) }
              )
              LinkText(
                text = "Privacy",
                onClick = { uriHandler.openUri(privacyUrl) }
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun LinkText(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Text(
    modifier = modifier.clickable(onClick = onClick),
    text = text,
    style = MaterialTheme.typography.bodySmall.copy(
      color = MaterialTheme.colorScheme.primary,
      textDecoration = TextDecoration.Underline,
      fontWeight = FontWeight.SemiBold,
    ),
  )
}