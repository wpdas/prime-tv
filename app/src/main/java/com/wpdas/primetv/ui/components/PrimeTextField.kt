package com.wpdas.primetv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Campo de texto de uma linha para TV, em dois estados:
 * - navegação: é um botão focável pelo D-pad (anel violeta) que mostra o texto;
 * - edição: ao apertar OK, vira um campo real com o teclado do sistema aberto.
 * Done/Voltar fecham o teclado e devolvem o foco ao botão. Assim o teclado nunca
 * abre só porque o foco passou pelo campo.
 */
@Composable
fun PrimeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var editing by remember { mutableStateOf(false) }
    var fieldHadFocus by remember { mutableStateOf(false) }
    // Estado com seleção, para o cursor começar no fim do texto ao entrar em edição.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val scope = rememberCoroutineScope()
    val surfaceFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }
    val textStyle = PrimeTheme.typography.body.copy(fontSize = 17.sp, lineHeight = 22.sp, color = colors.text)

    fun stopEditing() {
        if (!editing) return
        editing = false
        fieldHadFocus = false
        keyboard?.hide()
        // Depois que o campo de edição sai da composição, o foco volta ao botão.
        scope.launch {
            delay(80)
            runCatching { surfaceFocus.requestFocus() }
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
            runCatching { fieldFocus.requestFocus() }
            keyboard?.show()
        }
    }
    BackHandler(enabled = editing) { stopEditing() }

    Surface(
        onClick = { if (enabled) editing = true },
        enabled = enabled,
        modifier = modifier.height(56.dp).focusRequester(surfaceFocus),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (editing) colors.selected else colors.surface2,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
            disabledContainerColor = colors.surface2,
            disabledContentColor = colors.textBody,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
            if (editing) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { new ->
                        fieldValue = new
                        if (new.text != value) onValueChange(new.text)
                    },
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(
                        onDone = { stopEditing(); onDone?.invoke() },
                        onGo = { stopEditing(); onDone?.invoke() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                        // Na TV o teclado some com Voltar mas o campo continuaria em edição; aqui
                        // Voltar encerra a edição e cima/baixo saem do campo na direção da tecla.
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Back, Key.Escape -> { stopEditing(); true }
                                Key.DirectionDown, Key.DirectionUp -> {
                                    val direction = if (event.key == Key.DirectionDown) FocusDirection.Down else FocusDirection.Up
                                    editing = false
                                    fieldHadFocus = false
                                    keyboard?.hide()
                                    scope.launch {
                                        delay(80)
                                        runCatching { surfaceFocus.requestFocus() }
                                        delay(40)
                                        focusManager.moveFocus(direction)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .onFocusChanged { state ->
                            // onFocusChanged dispara ao anexar (sem foco); só encerra se já teve foco antes.
                            if (state.isFocused) fieldHadFocus = true
                            else if (fieldHadFocus && editing) stopEditing()
                        },
                )
            } else {
                Text(
                    text = value.ifEmpty { placeholder.orEmpty() },
                    style = textStyle,
                    color = if (value.isEmpty()) colors.textSecondary else colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
