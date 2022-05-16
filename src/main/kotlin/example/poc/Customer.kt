package example.poc

import io.swagger.v3.oas.annotations.media.Schema

data class Customer(
    @field:Schema(required = true) val firstName: String,
    @field:Schema(required = true) val lastName: String,
)