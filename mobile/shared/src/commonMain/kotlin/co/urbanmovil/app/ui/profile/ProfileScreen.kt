package co.urbanmovil.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.LocalEsDebug
import co.urbanmovil.app.data.model.Usuario
import co.urbanmovil.app.ui.common.Aviso
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.aviso_prototipo
import urbanmovil.shared.generated.resources.ic_perfil
import urbanmovil.shared.generated.resources.perfil_cerrar_sesion
import urbanmovil.shared.generated.resources.perfil_compilacion_debug
import urbanmovil.shared.generated.resources.perfil_titulo

@Composable
fun ProfileScreen() {
    val authRepository = LocalAppContainer.current.authRepository
    val viewModel = viewModel { ProfileViewModel(authRepository) }
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()

    ProfileContenido(
        usuario = usuario,
        esDebug = LocalEsDebug.current,
        alCerrarSesion = viewModel::cerrarSesion,
    )
}

@Composable
fun ProfileContenido(usuario: Usuario?, esDebug: Boolean, alCerrarSesion: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.perfil_titulo), style = MaterialTheme.typography.headlineMedium)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(Res.drawable.ic_perfil),
                            contentDescription = null, // decorativo: el nombre va al lado
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(usuario?.nombre.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        usuario?.correo.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Aviso(stringResource(Res.string.aviso_prototipo))

        if (esDebug) {
            Text(
                stringResource(Res.string.perfil_compilacion_debug),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = alCerrarSesion,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(Res.string.perfil_cerrar_sesion))
        }
    }
}
