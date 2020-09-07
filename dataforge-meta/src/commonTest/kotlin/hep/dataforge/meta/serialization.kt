package hep.dataforge.meta

import kotlinx.serialization.json.Json

public val JSON_PRETTY: Json = Json { prettyPrint = true; useArrayPolymorphism = true }
public val JSON_PLAIN: Json = Json { prettyPrint = false; useArrayPolymorphism = true }