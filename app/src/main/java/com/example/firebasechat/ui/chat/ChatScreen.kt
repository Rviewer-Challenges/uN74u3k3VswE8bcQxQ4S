package com.example.firebasechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firebasechat.R
import com.example.firebasechat.data.models.Message
import com.example.firebasechat.ui.theme.FirebaseChatTheme
import com.example.firebasechat.ui.theme.LightGray
import com.example.firebasechat.ui.theme.MessageBubbleShape
import com.example.firebasechat.ui.theme.SelfMessageBubbleShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.example.firebasechat.ui.chat.ChatUIEvent as UIEvent

@RootNavGraph(start = true)
@Destination
@Composable
fun ChatScreen(
    viewModel: ChatVM = hiltViewModel()
) {
    AuthScreenContent(
        state = viewModel.state.collectAsState(),
        onUIEvent = viewModel::onUIEvent
    )
}

@Composable
private fun AuthScreenContent(
    state: State<ChatState>,
    onUIEvent: (UIEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TopActionsRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f), // Draw on top
                isDarkMode = state.value.isDarkMode,
                isSignedIn = state.value.isSignedIn,
                onSwapDarkLightMode = { onUIEvent(UIEvent.SwapDarkLightMode) },
                onSignOut = { onUIEvent(UIEvent.SignOut) }
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MessageList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(),
                    messages = state.value.messages
                )
                BottomSection(
                    modifier = Modifier.fillMaxWidth(),
                    isSignedIn = state.value.isSignedIn,
                    editor = state.value.editor,
                    onEditorChanged = { newMessage -> onUIEvent(UIEvent.OnEditorChanged(newMessage)) },
                    onMessageSent = { onUIEvent(UIEvent.OnMessageSent) },
                    onSignIn = { onUIEvent(UIEvent.SignIn) }
                )
            }
        }
    }
}

@Composable
private fun TopActionsRow(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    isSignedIn: Boolean,
    onSwapDarkLightMode: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth(),
        ) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSwapDarkLightMode) {
                Crossfade(isDarkMode) { isDarkMode ->
                    if (isDarkMode) {
                        Icon(
                            painter = painterResource(R.drawable.ic_light_mode),
                            contentDescription = stringResource(R.string.light_mode)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_dark_mode),
                            contentDescription = stringResource(R.string.dark_mode)
                        )
                    }
                }
            }
            if (isSignedIn) {
                IconButton(onClick = onSignOut) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sign_out),
                        contentDescription = stringResource(R.string.sign_out)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<Message>,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
    ) {
        items(items = messages, key = Message::id) { message ->
            MessageBubble(message = message)
        }
    }
}

@Composable
private fun MessageBubble(
    modifier: Modifier = Modifier,
    message: Message
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        val alignment = if (message.isSelf) Alignment.TopEnd else Alignment.TopStart
        val backgroundColor = if (message.isSelf) Color.Black else LightGray
        val shape = if (message.isSelf) SelfMessageBubbleShape else MessageBubbleShape

        Surface(
            modifier = modifier.align(alignment),
            color = backgroundColor,
            shape = shape
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                text = message.text
            )
        }
    }
}

@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean,
    editor: String,
    onEditorChanged: (String) -> Unit,
    onMessageSent: () -> Unit,
    onSignIn: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            if (isSignedIn) {
                Editor(
                    message = editor,
                    onMessageChanged = onEditorChanged,
                    onMessageSent = onMessageSent
                )
            } else {
                SignInButton(onSignIn = onSignIn)
            }
        }
    }
}

@Composable
private fun Editor(
    modifier: Modifier = Modifier,
    message: String,
    onMessageChanged: (String) -> Unit,
    onMessageSent: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = onMessageChanged,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions { onMessageSent() }
        )

        AnimatedVisibility(
            visible = message.isNotBlank()
        ) {
            IconButton(
                onClick = onMessageSent,
                colors = IconButtonDefaults.outlinedIconButtonColors()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.send)
                )
            }
        }
    }
}

@Composable
private fun SignInButton(
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit
) {
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 22.dp,
            vertical = 12.dp
        ),
        shape = MaterialTheme.shapes.medium,
        onClick = onSignIn
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp),
            painter = painterResource(R.drawable.ic_google),
            tint = Color.Unspecified,
            contentDescription = null
        )
        Text(
            text = "Join in with Google"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TopActionsRowPreview() {
    FirebaseChatTheme {
        var isSignedIn by remember { mutableStateOf(true) }
        var isDarkMode by remember { mutableStateOf(true) }

        TopActionsRow(
            isDarkMode = isDarkMode,
            isSignedIn = isSignedIn,
            onSwapDarkLightMode = { isDarkMode = !isDarkMode },
            onSignOut = { isSignedIn = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorPreview() {
    FirebaseChatTheme {
        var message by remember { mutableStateOf("") }

        Editor(
            message = message,
            onMessageChanged = { message = it },
            onMessageSent = { message = "" }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignInButtonPreview() {
    FirebaseChatTheme {
        SignInButton(onSignIn = { })
    }
}