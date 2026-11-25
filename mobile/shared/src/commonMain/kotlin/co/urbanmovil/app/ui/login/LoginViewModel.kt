package co.urbanmovil.app.ui.login

import androidx.lifecycle.ViewModel
import co.urbanmovil.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    /** CA-17.2: un único error genérico, sin decir qué dato falló. */
    val credencialesInvalidas: Boolean = false,
)

/**
 * HU-17. No navega: al iniciar sesión cambia [AuthRepository.sesion] y `App` muestra la navegación principal.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(
        authRepository.credencialesDemo
            ?.let { LoginUiState(correo = it.correo, clave = it.clave) } // precargado para agilizar la demo
            ?: LoginUiState(),
    )
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun alCambiarCorreo(correo: String) =
        _estado.update { it.copy(correo = correo, credencialesInvalidas = false) }

    fun alCambiarClave(clave: String) =
        _estado.update { it.copy(clave = clave, credencialesInvalidas = false) }

    /** CA-17.1 / CA-17.2. */
    fun ingresar() {
        val actual = _estado.value
        val ok = authRepository.iniciarSesion(actual.correo, actual.clave)
        _estado.update { it.copy(credencialesInvalidas = !ok) }
    }

    /** CA-17.3 (MVP): entra directo como usuario demo. */
    fun crearCuenta() {
        authRepository.entrarComoDemo()
        _estado.update { it.copy(credencialesInvalidas = false) }
    }
}
