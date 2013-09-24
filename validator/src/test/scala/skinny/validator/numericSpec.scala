package skinny.validator

import org.scalatest._
import org.scalatest.matchers._

class numericSpec extends FlatSpec with ShouldMatchers {

  behavior of "numeric"

  it should "be available" in {
    val validate = numeric
    validate.name should equal("numeric")

    validate(param("id", "abc")).isSuccess should equal(false)
    validate(param("id", "あ")).isSuccess should equal(false)
    validate(param("id", "1a")).isSuccess should equal(false)

    validate(param("id", null)).isSuccess should equal(true)
    validate(param("id", "")).isSuccess should equal(true)

    validate(param("id", "0")).isSuccess should equal(true)
    validate(param("id", 0)).isSuccess should equal(true)

    validate(param("id", -1)).isSuccess should equal(true)
    validate(param("id", -0.1D)).isSuccess should equal(true)

    validate(param("id", 1)).isSuccess should equal(true)
    validate(param("id", 2)).isSuccess should equal(true)
    validate(param("id", 3)).isSuccess should equal(true)
    validate(param("id", 0.3D)).isSuccess should equal(true)
    validate(param("id", 0.3F)).isSuccess should equal(true)
    validate(param("id", 123L)).isSuccess should equal(true)

  }

}
