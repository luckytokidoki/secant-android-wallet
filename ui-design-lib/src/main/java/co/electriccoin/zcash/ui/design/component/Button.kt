package co.electriccoin.zcash.ui.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Preview
@Composable
fun ButtonComposablePreview() {
    ZcashTheme(darkTheme = true) {
        GradientSurface {
            Column {
                PrimaryButton(onClick = { }, text = "Primary")
                SecondaryButton(onClick = { }, text = "Secondary")
                TertiaryButton(onClick = { }, text = "Tertiary")
                NavigationButton(onClick = { }, text = "Navigation")
            }
        }
    }
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ),
        enabled = enabled,
        colors = buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            style = MaterialTheme.typography.labelLarge,
            text = text,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ),
        enabled = enabled,
        colors = buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text(
            style = MaterialTheme.typography.labelLarge,
            text = text,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
fun NavigationButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ),
        colors = buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text(style = MaterialTheme.typography.labelLarge, text = text, color = MaterialTheme.colorScheme.onSecondary)
    }
}

@Composable
fun TertiaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ),
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        colors = buttonColors(containerColor = ZcashTheme.colors.tertiary)
    ) {
        Text(
            style = MaterialTheme.typography.labelLarge,
            text = text,
            color = ZcashTheme.colors.onTertiary
        )
    }
}

@Composable
fun DangerousButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ),
        colors = buttonColors(containerColor = ZcashTheme.colors.dangerous)
    ) {
        Text(
            style = MaterialTheme.typography.labelLarge,
            text = text,
            color = ZcashTheme.colors.onDangerous
        )
    }
}

@Suppress("LongParameterList")
@Composable
fun TimedButton(
    modifier: Modifier = Modifier,
    duration: Duration = 5.seconds,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    LaunchedEffect(interactionSource) {
        var action: Job? = null

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    action = launch(coroutineDispatcher) {
                        delay(duration)
                        withContext(Dispatchers.Main) {
                            onClick()
                        }
                    }
                }
                is PressInteraction.Release -> {
                    action?.cancel()
                }
            }
        }
    }

    Button(
        modifier = modifier,
        onClick = {},
        interactionSource = interactionSource,
        content = content
    )
}
