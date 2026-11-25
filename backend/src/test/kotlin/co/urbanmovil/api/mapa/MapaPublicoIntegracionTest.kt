package co.urbanmovil.api.mapa

import co.urbanmovil.api.soporte.IntegracionTest
import org.junit.jupiter.api.Test
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.get

/** Web y app cargan el MISMO estilo desde aquí: así la ciudad se ve igual en las dos. */
class MapaPublicoIntegracionTest : IntegracionTest() {

    @Test
    fun `el estilo del mapa es publico y trae la ciudad embebida`() {
        mockMvc.get("/api/v1/public/mapa/estilo") { with(anonymous()) }.andExpect {
            status { isOk() }
            jsonPath("$.version") { value(8) }
            jsonPath("$.sources.ciudad.type") { value("geojson") }
            jsonPath("$.sources.ciudad.data.type") { value("FeatureCollection") }
            jsonPath("$.sources.ciudad.data.features.length()") { value(org.hamcrest.Matchers.greaterThan(300)) }
            jsonPath("$.glyphs") { isString() }
            jsonPath("$.layers[0].type") { value("background") }
            jsonPath("$.layers[?(@.id == 'calles')]") { exists() }
            jsonPath("$.layers[?(@.id == 'nombres-avenidas')]") { exists() }
        }
    }

    @Test
    fun `los lugares de la ciudad son publicos`() {
        mockMvc.get("/api/v1/public/mapa/lugares") { with(anonymous()) }.andExpect {
            status { isOk() }
            jsonPath("$.ubicacionPasajero.nombre") { value("Mi ubicación") }
            jsonPath("$.ubicacionPasajero.punto.lat") { isNumber() }
            jsonPath("$.lugares.length()") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)) }
            jsonPath("$.lugares[0].nombre") { isString() }
        }
    }
}
