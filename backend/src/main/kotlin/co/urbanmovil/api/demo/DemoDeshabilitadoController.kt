package co.urbanmovil.api.demo

import co.urbanmovil.api.common.NoEncontradoException
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Sin el perfil demo, cualquier llamada a `/api/v1/demo/...` responde 404 con cualquier método
 * (sin esto, un POST a una ruta inexistente terminaría en 405 por el manejador de recursos estáticos).
 */
@Hidden
@RestController
@ConditionalOnProperty(prefix = "urbanmovil.demo", name = ["habilitado"], havingValue = "false", matchIfMissing = true)
class DemoDeshabilitadoController {

    @RequestMapping("/api/v1/demo", "/api/v1/demo/**")
    fun noDisponible(): Nothing =
        throw NoEncontradoException("Los recorridos de demostración solo existen con el perfil demo")
}
