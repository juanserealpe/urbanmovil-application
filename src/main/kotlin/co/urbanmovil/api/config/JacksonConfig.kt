package co.urbanmovil.api.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.jdk.StringDeserializer
import tools.jackson.databind.deser.std.StdScalarDeserializer
import tools.jackson.databind.module.SimpleModule

/**
 * Recorta espacios de todo texto de entrada ANTES de que llegue a Bean Validation
 * (regla §3.3: «se recortan espacios antes de validar»).
 */
@Configuration(proxyBeanMethods = false)
class JacksonConfig {

    @Bean
    fun recorteDeTextos(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.addModule(
            SimpleModule("recorte-de-textos").addDeserializer(String::class.java, TextoRecortadoDeserializer()),
        )
    }

    class TextoRecortadoDeserializer : StdScalarDeserializer<String>(String::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String? =
            StringDeserializer.instance.deserialize(p, ctxt)?.trim()
    }
}
