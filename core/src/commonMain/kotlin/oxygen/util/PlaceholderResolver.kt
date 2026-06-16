package oxygen.util

object PlaceholderResolver {
  val map = mutableMapOf<String, String>()

  fun put(key: String, value: String) {
    map[key] = value
  }

  fun resolve(template: Array<String>): Array<String> {
    return template
        .map { line ->
          var result = line
          map.forEach { (key, value) -> result = result.replace("\${$key}", value) }
          result
        }
        .toTypedArray()
  }
}
