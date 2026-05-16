package oxygen

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import oxygen.util.*

class Settings() {
  val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    isLenient = true
  }

  val settingsFile: Fi
    get() = OLPath.settingsFile

  var cache: JsonObject = JsonObject(emptyMap())

  /// ----
  companion object {
    const val LAUNCHER_KEY = "launcher"
    const val GAME_KEY = "game"
  }

  var launcher: LauncherConfig
    get() {
      val existing: LauncherConfig? = getOrNull(LAUNCHER_KEY)
      if (existing != null) return existing
      val default = LauncherConfig()
      put(LAUNCHER_KEY, default)
      return default
    }
    set(value) {
      put(LAUNCHER_KEY, value)
    }

  var game: JsonObject
    get() =
        getJsonObject(GAME_KEY)
            ?: run {
              val default = JsonObject(emptyMap())
              putJsonObject(GAME_KEY, default)
              return default
            }
    set(value) = putJsonObject(GAME_KEY, value)

  fun setGameDefault(json: String) {
    putDefault(GAME_KEY, asObj(json))
  }

  inline fun <reified T> initField(key: String, def: T): T {
    putDefault(key, json.encodeToJsonElement(def).jsonObject)
    return get(key, def)
  }

  fun initJsonFiled(key: String, def: JsonObject): JsonObject {
    putDefault(key, def)
    return getJsonObject(key) ?: def
  }

  init {
    loadAll()
    launcher = initField(LAUNCHER_KEY, LauncherConfig())
    game = initJsonFiled(GAME_KEY, JsonObject(emptyMap()))
  }

  /// ----

  fun loadAll() {
    if (settingsFile.exists()) loadFrom(settingsFile.readString())
  }

  fun asObj(jsonString: String): JsonObject =
      try {
        json.decodeFromString<JsonObject>(jsonString)
      } catch (e: Exception) {
        JsonObject(emptyMap())
      }

  fun loadFrom(jsonString: String) {
    cache = asObj(jsonString)
  }

  fun toJsonString(): String = toJsonString(cache)

  fun toJsonString(obj: JsonObject): String = json.encodeToString(obj)

  fun flush() {
    settingsFile.writeString(toJsonString())
  }

  inline fun <reified T> put(key: String, value: T) {
    val element =
        when (value) {
          is String -> json.encodeToJsonElement(value)
          is Int -> json.encodeToJsonElement(value)
          is Long -> json.encodeToJsonElement(value)
          is Float -> json.encodeToJsonElement(value)
          is Double -> json.encodeToJsonElement(value)
          is Boolean -> json.encodeToJsonElement(value)
          is List<*> -> json.encodeToJsonElement(value)
          else -> json.encodeToJsonElement(value)
        }
    cache = JsonObject(cache.toMap() + (key to element))
    flush()
  }

  inline fun <reified T> get(key: String, defaultValue: T): T {
    val element = cache[key] ?: return defaultValue
    return try {
      when (defaultValue) {
        is String -> element.jsonPrimitive.content as T
        is Int -> element.jsonPrimitive.int as T
        is Long -> element.jsonPrimitive.long as T
        is Double -> element.jsonPrimitive.double as T
        is Boolean -> element.jsonPrimitive.boolean as T
        else -> json.decodeFromJsonElement<T>(element)
      }
    } catch (e: Exception) {
      defaultValue
    }
  }

  inline fun <reified T : Any> getOrNull(key: String): T? {
    val element = cache[key] ?: return null
    return try {
      json.decodeFromJsonElement<T>(element)
    } catch (e: Exception) {
      null
    }
  }

  fun putJsonObject(key: String, jsonObject: JsonObject) {
    cache = JsonObject(cache.toMap() + (key to jsonObject))
    flush()
  }

  fun getJsonObject(key: String): JsonObject? {
    val element = cache[key] ?: return null
    return try {
      element.jsonObject
    } catch (e: Exception) {
      null
    }
  }

  fun mergeJsonObject(key: String, jsonObject: JsonObject) {
    val existing = getJsonObject(key)
    cache =
        JsonObject(
            cache.toMap() +
                (key to
                    if (existing != null) {
                      JsonObject(existing.toMap() + jsonObject.toMap())
                    } else {
                      jsonObject
                    })
        )
    flush()
  }

  fun putDefault(key: String, defaultJson: JsonObject) {
    val existing = getJsonObject(key)
    cache =
        JsonObject(
            cache.toMap() +
                (key to
                    if (existing != null) {
                      deepMergeDefault(defaultJson, existing)
                    } else {
                      defaultJson
                    })
        )
    flush()
  }

  fun deepMergeDefault(default: JsonObject, existing: JsonObject): JsonObject {
    val merged = existing.toMap().toMutableMap()
    for ((key, defaultValue) in default) {
      val existingValue = existing[key]
      merged[key] =
          when {
            existingValue == null -> defaultValue
            defaultValue is JsonObject && existingValue is JsonObject ->
                deepMergeDefault(defaultValue, existingValue)
            !isSameType(defaultValue, existingValue) -> defaultValue
            else -> existingValue
          }
    }
    return JsonObject(merged)
  }

  private fun isSameType(a: JsonElement, b: JsonElement): Boolean {
    return when (a) {
      is JsonObject -> b is JsonObject
      is JsonArray -> b is JsonArray
      is JsonPrimitive -> b is JsonPrimitive && a.isString == b.isString
      is JsonNull -> b is JsonNull
    }
  }

  fun remove(key: String) {
    cache = JsonObject(cache.toMap() - key)
    flush()
  }

  fun clear() {
    cache = JsonObject(emptyMap())
    flush()
  }
}

@Serializable
data class LauncherConfig(
    val userArgs: String =
        "-XX:+AlwaysPreTouch -XX:+UseG1GC -XX:MaxGCPauseMillis=130 -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=75 -XX:G1HeapWastePercent=5 -XX:+DisableExplicitGC -XX:+PerfDisableSharedMem",
    val useWakelock: Boolean = false,
    val hideStatusBar: Boolean = true,
    val useImmersiveMode: Boolean = true,
    val redirectStdio: Boolean = false,
    val setupExitTrap: Boolean = true,
)
