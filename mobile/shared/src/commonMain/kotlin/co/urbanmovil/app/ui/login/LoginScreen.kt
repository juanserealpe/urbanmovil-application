package co.urbanmovil.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.ui.common.Aviso
import co.urbanmovil.app.ui.common.LogoUrbanMovil
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.app_nombre
import urbanmovil.shared.generated.resources.aviso_prototipo
import urbanmovil.shared.generated.resources.login_clave
import urbanmovil.shared.generated.resources.login_correo
import urbanmovil.shared.generated.resources.login_crear_cuenta
import urbanmovil.shared.generated.resources.login_credenciales_invalidas
import urbanmovil.shared.generated.resources.login_ingresar
import urbanmovil.shared.generated.resources.login_lema

@Composable
fun LoginScreen() {
    val authRepository = LocalAppContainer.current.authRepository
    val viewModel = viewModel { LoginViewModel(authRepository) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LoginContenido(
        estado = estado,
        alCambiarCorreo = viewModel::alCambiarCorreo,
        alCambiarClave = viewModel::alCambiarClave,
        alIngresar = viewModel::ingresar,
        alCrearCuenta = viewModel::crearCuenta,
    )
}

/** Mockup «01 · Inicio de sesión». Sin estado propio: fácil de previsualizar y probar. */
@Composable
fun LoginContenido(
    estado: LoginUiState,
    alCambiarCorreo: (String) -> Unit,
    alCambiarClave: (String) -> Unit,
    alIngresar: () -> Unit,
    alCrearCuenta: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.size(24.dp))
            LogoUrbanMovil()
            Text(stringResource(Res.string.app_nombre), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(Res.string.login_lema),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))

            OutlinedTextField(
                value = estado.correo,
                onValueChange = alCambiarCorreo,
                label = { Text(stringResource(Res.string.login_correo)) },
                singleLine = true,
                isError = estado.credencialesInvalidas,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.clave,
                onValueChange = alCambiarClave,
                label = { Text(stringResource(Res.string.login_clave)) },
                singleLine = true,
                isError = estado.credencialesInvalidas,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { alIngresar() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (estado.credencialesInvalidas) {
                Text(
                    text = stringResource(Res.string.login_credenciales_invalidas),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Button(onClick = alIngresar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(Res.string.login_ingresar))
            }
            OutlinedButton(onClick = alCrearCuenta, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(Res.string.login_crear_cuenta))
            }

            Aviso(stringResource(Res.string.aviso_prototipo))
        }
    }
}
