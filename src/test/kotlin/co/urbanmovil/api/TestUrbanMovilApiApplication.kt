package co.urbanmovil.api

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<UrbanMovilApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
