package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Credenciales
import co.urbanmovil.app.data.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sesión del pasajero (HU-17). La UI nunca valida credenciales por su cuenta. */
interface AuthRepository {
    /** Usuario con sesión iniciada, o `null`. Sin sesión no se navega a ninguna pantalla (RN-18). */
    val sesion: StateFlow<Usuario?>

    /** Credenciales para precargar el formulario en la demo; `null` en una implementación real. */
    val credencialesDemo: Credenciales?

    /** `true` si las credenciales son válidas y se inició sesión. */
    fun iniciarSesion(correo: String, clave: String): Boolean

    /** CA-17.3 (MVP): «Crear cuenta» entra directo como usuario demo. */
    fun entrarComoDemo()

    fun cerrarSesion()
}

/** Login quemado contra el usuario demo. La sesión vive solo en memoria. */
class FakeAuthRepository : AuthRepository {

    private val _sesion = MutableStateFlow<Usuario?>(null)
    override val sesion: StateFlow<Usuario?> = _sesion.asStateFlow()

    override val credencialesDemo: Credenciales = FakeData.CREDENCIALES_DEMO

    override fun iniciarSesion(correo: String, clave: String): Boolean {
        val valido = correo.trim().equals(FakeData.USUARIO_DEMO.correo, ignoreCase = true) &&
            clave == FakeData.CLAVE_DEMO
        if (valido) _sesion.value = FakeData.USUARIO_DEMO
        return valido
    }

    override fun entrarComoDemo() {
        _sesion.value = FakeData.USUARIO_DEMO
    }

    override fun cerrarSesion() {
        _sesion.value = null
    }
}
