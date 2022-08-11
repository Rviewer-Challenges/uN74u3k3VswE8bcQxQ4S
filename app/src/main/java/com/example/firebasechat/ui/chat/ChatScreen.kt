package com.example.firebasechat.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firebasechat.R
import com.example.firebasechat.ui.theme.FirebaseChatTheme
import com.ramcosta.composedestinations.annotation.Destination

@Destination(start = true)
@Composable
fun ChatScreen(
    viewModel: ChatVM = hiltViewModel()
) {
    AuthScreenContent(
        signedIn = viewModel.signedIn.collectAsState(),
        onUIEvent = viewModel::onUIEvent
    )
}

@Composable
private fun AuthScreenContent(
    signedIn: State<Boolean>,
    onUIEvent: (ChatUIEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TopActionsRow(
                modifier = Modifier.align(Alignment.TopCenter),
                signedIn = signedIn.value,
                onSignOut = { onUIEvent(ChatUIEvent.signOut) }
            )
            MessageComposer(
                modifier = Modifier.align(Alignment.BottomCenter),
                signedIn = signedIn.value,
                onSignIn = { onUIEvent(ChatUIEvent.signIn) }
            )
        }
    }
}

@Composable
private fun TopActionsRow(
    modifier: Modifier = Modifier,
    signedIn: Boolean,
    onSignOut: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (signedIn) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_dark_mode),
                    contentDescription = stringResource(R.string.dark_mode)
                )
            }
            IconButton(onClick = onSignOut) {
                Icon(
                    painter = painterResource(R.drawable.ic_sign_out),
                    contentDescription = stringResource(R.string.sign_out)
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    modifier: Modifier = Modifier,
    signedIn: Boolean,
    onSignIn: () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(value = "my message", onValueChange = {})
        if (!signedIn) {
            Button(onClick = onSignIn) {
                Text(text = "Chat with Google")
            }
        } else {
            Button(onClick = { }) {
                Text(text = "Send")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FirebaseChatTheme {
        var signedIn by remember { mutableStateOf(true) }

        TopActionsRow(signedIn = signedIn, onSignOut = { signedIn = false })
    }
}