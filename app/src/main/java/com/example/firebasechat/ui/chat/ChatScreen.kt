package com.example.firebasechat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.firebasechat.R
import com.example.firebasechat.messages.model.Message
import com.example.firebasechat.ui.theme.*
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
        Column(modifier = Modifier.fillMaxSize()) {
            TopActionsRow(
                isDarkMode = state.value.isDarkMode,
                isSignedIn = state.value.isSignedIn,
                onSwapDarkLightMode = { onUIEvent(UIEvent.SwapDarkLightMode) },
                onSignOut = { onUIEvent(UIEvent.SignOut) }
            )
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
        shadowElevation = 6.dp
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
        contentPadding = PaddingValues(6.dp)
    ) {
        itemsIndexed(
            items = messages,
            key = { _, message -> message.uid }
        ) { index, message ->
            val firstByUser = index == 0 || messages[index - 1].user != message.user
            val topSpacing = if (firstByUser && index != 0) PaddingValues(top =10.dp) else PaddingValues()

            MessageCell(
                modifier = Modifier.padding(topSpacing),
                message = message,
                firstByUser = firstByUser
            )
        }
    }
}

@Composable
private fun MessageCell(
    modifier: Modifier = Modifier,
    message: Message,
    firstByUser: Boolean // TODO draw a header with the photoURL and the name
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        val alignment = if (message.isSelf) Alignment.TopEnd else Alignment.TopStart
        val backgroundColor = if (message.isSelf) Color.Black else LightGray
        val shape = when {
            firstByUser && message.isSelf -> FirstSelfMessageBubbleShape
            firstByUser && !message.isSelf -> FirstMessageBubbleShape
            else -> MessageBubbleShape
        }

        Box(
            modifier = modifier
                .align(alignment)
                .fillMaxWidth(0.82f) // Don't let the messages extend all the way to the other side
        ) {
            if (!message.isSelf && firstByUser) {
                Row(
                    modifier = Modifier
                        .align(alignment)
                        .requiredHeight(IntrinsicSize.Max)
                ) {
                    AsyncImage(
                        model = message.user?.photoUrl
                            ?: "https://www.princeton.edu/sites/default/files/styles/half_2x/public/images/2022/02/KOA_Nassau_2697x1517.jpg",
                        contentDescription = stringResource(R.string.avatar),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 2.dp, bottom = 2.dp, end = 6.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = message.user?.name ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        MessageBubble(
                            text = message.text,
                            backgroundColor = backgroundColor,
                            shape = shape
                        )
                    }
                }
            } else {
                MessageBubble(
                    modifier = Modifier.align(alignment),
                    text = message.text,
                    backgroundColor = backgroundColor,
                    shape = shape
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    shape: Shape,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = shape
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
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
        shadowElevation = 6.dp
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
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = message,
            onValueChange = onMessageChanged,
            modifier = Modifier.weight(1f),
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions { onMessageSent() },
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = message,
                placeholder = { Text(stringResource(R.string.editor_placeholder)) },
                innerTextField = innerTextField,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                singleLine = false,
                enabled = true,
                visualTransformation = VisualTransformation.None
            )
        }

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
private fun EditorEmptyPreview() {
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
private fun EditorPreview() {
    FirebaseChatTheme {
        var message by remember { mutableStateOf("test message") }

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