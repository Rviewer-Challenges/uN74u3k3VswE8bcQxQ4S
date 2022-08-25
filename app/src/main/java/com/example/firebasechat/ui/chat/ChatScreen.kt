package com.example.firebasechat.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.firebasechat.R
import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.auth.model.User
import com.example.firebasechat.messages.model.Message
import com.example.firebasechat.messages.model.Reaction
import com.example.firebasechat.ui.theme.FirebaseChatTheme
import com.example.firebasechat.ui.theme.FirstMessageBubbleShape
import com.example.firebasechat.ui.theme.FirstSelfMessageBubbleShape
import com.example.firebasechat.ui.theme.MessageBubbleShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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
                isSignedIn = state.value.authState is AuthState.SignedIn,
                onSwapDarkLightMode = { onUIEvent(UIEvent.SwapDarkLightMode) },
                onSignOut = { onUIEvent(UIEvent.SignOut) }
            )
            val scrollState = rememberLazyListState()
            MessageList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                messages = state.value.messages,
                scrollState = scrollState,
                pressEnabled = state.value.authState is AuthState.SignedIn,
                onReactionPressed = { emoji, messageUid -> onUIEvent(UIEvent.OnReactionPressed(emoji, messageUid)) }
            )
            BottomSection(
                modifier = Modifier.fillMaxWidth(),
                authState = state.value.authState,
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
            AnimatedVisibility(isSignedIn) {
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
    scrollState: LazyListState,
    pressEnabled: Boolean,
    onReactionPressed: (String, String) -> Unit
) {
    Box(modifier = modifier) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        var selected by remember { mutableStateOf<String?>(null) }

        // Scroll down on new messages
        var previousMessageCount by remember { mutableStateOf(messages.size) }
        LaunchedEffect(messages.size) {
            if (messages.size > previousMessageCount) {
                scope.launch { scrollState.animateScrollToItem(0) }
            }
            previousMessageCount = messages.size
        }

        LazyColumn(
            verticalArrangement = Arrangement.Top,
            state = scrollState,
            reverseLayout = true,
            contentPadding = PaddingValues(6.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { _, message -> message.uid }
            ) { index, message ->
                val firstByUser = index == messages.lastIndex || messages[index + 1].user != message.user
                val topSpacing = PaddingValues(top = if (firstByUser && index != messages.lastIndex) 10.dp else 0.dp)

                MessageCell(
                    modifier = Modifier.padding(topSpacing),
                    message = message,
                    firstByUser = firstByUser,
                    selected = selected == message.uid,
                    onPressed = {
                        selected = if (selected == message.uid) null else message.uid
                    },
                    pressEnabled = pressEnabled,
                    onReactionPressed = { reaction ->
                        selected = null
                        onReactionPressed(reaction, message.uid)
                    }
                )
            }
        }

        val scrollToBottomVisible by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 || scrollState.firstVisibleItemScrollOffset > with(density) { 48.dp.toPx() }
            }
        }

        ScrollToBottomButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            visible = scrollToBottomVisible,
            onPressed = { scope.launch { scrollState.animateScrollToItem(0) } }
        )
    }
}

@Composable
private fun MessageCell(
    modifier: Modifier = Modifier,
    message: Message,
    firstByUser: Boolean,
    selected: Boolean,
    pressEnabled: Boolean,
    onPressed: () -> Unit,
    onReactionPressed: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .clickable(onClick = onPressed, enabled = pressEnabled)
    ) {
        val alignment = if (message.isSelf) Alignment.TopEnd else Alignment.TopStart
        val backgroundColor = if (message.isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
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
                MessageWithAuthor(
                    modifier = Modifier.align(alignment),
                    message = message,
                    selected = selected,
                    backgroundColor = backgroundColor,
                    shape = shape,
                    onReactionPressed = onReactionPressed
                )
            } else {
                MessageBubbleWithReactions(
                    modifier = Modifier.align(alignment),
                    message = message,
                    selected = selected,
                    backgroundColor = backgroundColor,
                    shape = shape,
                    onReactionPressed = onReactionPressed
                )
            }
        }
    }
}

@Composable
private fun MessageWithAuthor(
    modifier: Modifier = Modifier,
    message: Message,
    selected: Boolean,
    backgroundColor: Color,
    shape: Shape,
    onReactionPressed: (String) -> Unit
) {
    Row(modifier = modifier) {
        AsyncImage(
            model = message.user?.photoUrl ?: stringResource(R.string.default_photo_url),
            contentDescription = stringResource(R.string.avatar),
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp, end = 6.dp)
                .size(52.dp)
                .clip(CircleShape)
        )
        Column {
            Text(
                text = message.user?.name ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.labelLarge,
            )
            MessageBubbleWithReactions(
                message = message,
                selected = selected,
                backgroundColor = backgroundColor,
                shape = shape,
                onReactionPressed = onReactionPressed
            )
        }
    }
}

// TODO: show full date for messages not from today
private val formatter = SimpleDateFormat("hh:mm")

@Composable
private fun MessageBubbleWithReactions(
    modifier: Modifier = Modifier,
    message: Message,
    selected: Boolean,
    backgroundColor: Color,
    shape: Shape,
    onReactionPressed: (String) -> Unit
) {
    val reactionsVisible = derivedStateOf { selected || message.reactions.isNotEmpty() }
    Box(modifier = modifier) {
        val padding = animateDpAsState(targetValue = if (reactionsVisible.value) 22.dp else 0.dp)
        val alpha = animateFloatAsState(targetValue = if (reactionsVisible.value) 1f else 0f)

        MessageBubble(
            modifier = Modifier
                .align(if (message.isSelf) Alignment.TopEnd else Alignment.TopStart)
                .padding(bottom = padding.value),
            text = message.text,
            createdAt = message.createdAt,
            backgroundColor = backgroundColor,
            shape = shape
        )

        ReactionSelector(
            modifier = Modifier
                .padding(start = 12.dp)
                .alpha(alpha.value)
                .align(if (message.isSelf) Alignment.BottomStart else Alignment.BottomEnd),
            reactions = message.reactions,
            onReactionPressed = onReactionPressed,
            enabled = reactionsVisible.value
        )
    }
}

@Composable
private fun MessageBubble(
    modifier: Modifier = Modifier,
    text: String,
    createdAt: Date,
    backgroundColor: Color,
    shape: Shape,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = shape
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 6.dp),
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatter.format(createdAt),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private val emojis = listOf("😀", "👍", "❤️", "😞")

@Composable
private fun ReactionSelector(
    modifier: Modifier = Modifier,
    reactions: List<Reaction>,
    enabled: Boolean,
    onReactionPressed: (String) -> Unit
) {
    Row(
        modifier = modifier.padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        emojis.forEach { emoji ->
            val reactionsForEmoji = reactions.filter { it.emoji == emoji }
            val reacted = reactionsForEmoji.any { it.isSelf }
            val background = animateColorAsState(if (reacted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            val foreground = animateColorAsState(if (reacted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

            // Not using a surface because it sets minimum size target for clicks...
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .then(if (enabled) Modifier.clickable(onClick = { onReactionPressed(emoji) }) else Modifier)
                    .background(
                        color = background.value,
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = if (reactionsForEmoji.isNotEmpty()) "+${reactionsForEmoji.size}$emoji" else emoji,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = foreground.value
                )
            }
        }
    }
}

@Composable
private fun ScrollToBottomButton(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onPressed: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        FloatingActionButton(
            onClick = onPressed,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_down),
                contentDescription = stringResource(R.string.scroll_down),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    authState: AuthState,
    editor: String,
    onEditorChanged: (String) -> Unit,
    onMessageSent: () -> Unit,
    onSignIn: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = 6.dp,
    ) {
        Crossfade(
            targetState = authState,
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) { targetState ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                when (targetState) {
                    is AuthState.SignedIn -> Editor(
                        message = editor,
                        onMessageChanged = onEditorChanged,
                        onMessageSent = onMessageSent
                    )
                    is AuthState.SigningIn -> SignInButton(onSignIn = onSignIn, loading = true)
                    else -> SignInButton(onSignIn = onSignIn, loading = false)
                }
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
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
    onSignIn: () -> Unit,
    loading: Boolean
) {
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 22.dp,
            vertical = 12.dp
        ),
        shape = MaterialTheme.shapes.medium,
        enabled = !loading,
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
            text = stringResource(if (!loading) R.string.join_with_google else R.string.joining)
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
private fun ChatPreview() {
    FirebaseChatTheme {
        val users = listOf(
            User("1", "John", null),
            User("2", "Michael", null),
            User("3", "George", null),
        )
        val messages = listOf(
            Message("1", "First message", user = users[0]),
            Message("2", "another one", Date(), user = users[0]),
            Message(
                "3",
                "and this is all I had to say for now, I'm gucci for now. Does this long message wrap nicely?",
                user = users[0]
            ),
            Message("4", "here's a reply from me", user = users[1], isSelf = true),
            Message("5", "and I'll finish it off with this one", user = users[2]),
            Message("6", "and the last", user = users[2]),
        )

        val scrollState = rememberLazyListState()
        MessageList(
            modifier = Modifier.size(400.dp, 600.dp),
            messages = messages,
            scrollState = scrollState,
            onReactionPressed = { _, _ -> },
            pressEnabled = true
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
        SignInButton(onSignIn = { }, loading = false)
    }
}