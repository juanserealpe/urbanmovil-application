package co.urbanmovil.app.ui.profile

import androidx.lifecycle.ViewModel
import co.urbanmovil.app.data.AuthRepository
import co.urbanmovil.app.data.model.Usuario
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val usuario: StateFlow<Usuario?> = authRepository.sesion

    /** CA-17.4: al borrar la sesión, `App` vuelve a la pantalla de acceso. */
    fun cerrarSesion() = authRepository.cerrarSesion()
}
